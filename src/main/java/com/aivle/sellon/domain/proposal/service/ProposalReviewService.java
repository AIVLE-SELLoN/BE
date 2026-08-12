package com.aivle.sellon.domain.proposal.service;

import com.aivle.sellon.domain.proposal.dto.request.ProposalAcceptRequest;
import com.aivle.sellon.domain.proposal.dto.request.ProposalRegenerateRequest;
import com.aivle.sellon.domain.proposal.dto.request.ProposalRejectRequest;
import com.aivle.sellon.domain.proposal.dto.response.ProposalAcceptHistoryResponse;
import com.aivle.sellon.domain.proposal.entity.Proposal;
import com.aivle.sellon.domain.proposal.entity.ProposalAcceptHistory;
import com.aivle.sellon.domain.proposal.entity.ProposalEvidence;
import com.aivle.sellon.domain.proposal.enums.HitlStatus;
import com.aivle.sellon.domain.proposal.event.ProposalReviewEventPublisher;
import com.aivle.sellon.domain.proposal.event.ProposalReviewedEvent;
import com.aivle.sellon.domain.proposal.exception.ProposalNotFoundException;
import com.aivle.sellon.domain.proposal.repository.ProposalAcceptHistoryRepository;
import com.aivle.sellon.domain.proposal.repository.ProposalEvidenceRepository;
import com.aivle.sellon.domain.proposal.repository.ProposalRepository;
import com.aivle.sellon.domain.proposal.service.support.ProposalAccessGuard;
import com.aivle.sellon.domain.proposal.service.support.ProposalProductDescriptionApplier;
import com.aivle.sellon.domain.proposal.service.support.ProposalResponseMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProposalReviewService {

    private final ProposalRepository proposalRepository;
    private final ProposalAcceptHistoryRepository proposalAcceptHistoryRepository;
    private final ProposalEvidenceRepository proposalEvidenceRepository;
    private final ProposalReviewEventPublisher proposalReviewEventPublisher;
    private final ProposalProductDescriptionApplier productDescriptionApplier;
    private final ProposalAccessGuard accessGuard;
    private final ProposalResponseMapper responseMapper;


    // 분석 재요청 — 반려와 동일하게 이력을 남긴다(reject와 구분 컬럼은 없음, 둘 다 REJECTED 이력으로 쌓임).
    @Transactional
    public ProposalAcceptHistoryResponse regenerateProposal(Long reportKey, ProposalRegenerateRequest request, Long companyId) {
        Proposal proposal = proposalRepository.findById(reportKey)
            .orElseThrow(ProposalNotFoundException::new);
        accessGuard.requireCompanyAccess(proposal, companyId);

        ProposalAcceptHistory history = ProposalAcceptHistory.ofReject(
            proposal,
            request.reasonCode(),
            request.reasonText(),
            request.processedBy()
        );
        proposalAcceptHistoryRepository.save(history);
        proposal.markReviewed(HitlStatus.REJECTED);

        publishReviewedEvent(proposal, history);

        return responseMapper.toResponse(history);
    }

    @Transactional
    public ProposalAcceptHistoryResponse acceptProposal(Long reportKey, ProposalAcceptRequest request, Long companyId) {
        Proposal proposal = proposalRepository.findById(reportKey)
            .orElseThrow(ProposalNotFoundException::new);
        accessGuard.requireCompanyAccess(proposal, companyId);

        boolean isEdited = request.improvedContent() != null && !request.improvedContent().isBlank();
        HitlStatus hitlStatus = isEdited ? HitlStatus.EDITED_APPROVED : HitlStatus.APPROVED;

        String prevContent = productDescriptionApplier.findCurrentDescription(proposal, companyId);

        ProposalAcceptHistory history = ProposalAcceptHistory.ofAccept(
            proposal,
            hitlStatus,
            proposal.getProposedContent(),
            request.improvedContent(),
            prevContent,
            request.processedBy()
        );
        proposalAcceptHistoryRepository.save(history);
        proposal.markReviewed(hitlStatus);

        String appliedText = isEdited ? request.improvedContent() : proposal.getProposedContent();
        productDescriptionApplier.apply(proposal, companyId, appliedText);

        publishReviewedEvent(proposal, history);

        return responseMapper.toResponse(history);
    }

    @Transactional
    public ProposalAcceptHistoryResponse rejectProposal(Long reportKey, ProposalRejectRequest request, Long companyId) {
        Proposal proposal = proposalRepository.findById(reportKey)
            .orElseThrow(ProposalNotFoundException::new);
        accessGuard.requireCompanyAccess(proposal, companyId);

        ProposalAcceptHistory history = ProposalAcceptHistory.ofReject(
            proposal,
            request.reasonCode(),
            request.reasonText(),
            request.processedBy()
        );
        proposalAcceptHistoryRepository.save(history);
        proposal.markReviewed(HitlStatus.REJECTED);

        publishReviewedEvent(proposal, history);

        return responseMapper.toResponse(history);
    }

    private void publishReviewedEvent(Proposal proposal, ProposalAcceptHistory history) {
        proposalReviewEventPublisher.publish(toReviewedEvent(
            proposal,
            history.getHitlStatus(),
            history.getProcessedBy(),
            history.getProcessedAt() != null ? history.getProcessedAt() : LocalDateTime.now(),
            history.getRejectionReasonCode(),
            history.getRejectionReasonText(),
            history.getImprovedContent()
        ));
    }

    private ProposalReviewedEvent toReviewedEvent(
        Proposal proposal,
        HitlStatus hitlStatus,
        String processedBy,
        LocalDateTime processedAt,
        String rejectionReasonCode,
        String rejectionReasonText,
        String editedText
    ) {
        ProposalReviewedEvent.RejectionReason rejectionReason =
            (rejectionReasonCode != null || rejectionReasonText != null)
                ? new ProposalReviewedEvent.RejectionReason(rejectionReasonCode, rejectionReasonText)
                : null;

        ProposalReviewedEvent.HitlFeedback hitlFeedback = new ProposalReviewedEvent.HitlFeedback(
            processedAt,
            processedBy,
            rejectionReason,
            editedText
        );

        return new ProposalReviewedEvent(
            proposal.getRecommendationId(),
            proposal.getAlertId(),
            hitlStatus,
            hitlFeedback,
            toAlertContext(proposal),
            toRecommendationContext(proposal)
        );
    }

    private ProposalReviewedEvent.AlertContext toAlertContext(Proposal proposal) {
        return new ProposalReviewedEvent.AlertContext(
            proposal.getDetectedAt(),
            proposal.getProductGroupId(),
            proposal.getChannel(),
            proposal.getVerdict(),
            proposal.getMainAspect(),
            proposal.getRecommendedAction()
        );
    }

    private ProposalReviewedEvent.RecommendationContext toRecommendationContext(Proposal proposal) {
        List<ProposalEvidence> evidences = proposalEvidenceRepository.findByProposal_ReportKey(proposal.getReportKey());
        List<ProposalReviewedEvent.RecommendationContext.Citation> citations = evidences.stream()
            .map(e -> new ProposalReviewedEvent.RecommendationContext.Citation(e.getInquiryId(), e.getQuoteText()))
            .toList();

        return new ProposalReviewedEvent.RecommendationContext(
            proposal.getProposalType(),
            proposal.getTargetField(),
            proposal.getCurrentText(),
            proposal.getProposedContent(),
            proposal.getRationale(),
            proposal.isDetailpageGrounded(),
            proposal.getConfidenceLevel(),
            proposal.getConfidenceDescription(),
            proposal.getSimilarCase(),
            proposal.isCappedByDetection(),
            proposal.isEvaluatorPassed(),
            citations
        );
    }
}
