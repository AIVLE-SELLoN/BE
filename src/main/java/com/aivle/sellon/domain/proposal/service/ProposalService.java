package com.aivle.sellon.domain.proposal.service;

import com.aivle.sellon.domain.proposal.client.ProposalGenerationClient;
import com.aivle.sellon.domain.proposal.client.ProposalGenerationResult;
import com.aivle.sellon.domain.proposal.dto.response.ProposalAcceptHistoryResponse;
import com.aivle.sellon.domain.proposal.dto.request.ProposalAcceptRequest;
import com.aivle.sellon.domain.proposal.dto.request.ProposalRegenerateRequest;
import com.aivle.sellon.domain.proposal.dto.request.ProposalRejectRequest;
import com.aivle.sellon.domain.proposal.dto.response.ProposalDetailResponse;
import com.aivle.sellon.domain.proposal.dto.response.ProposalEvidenceResponse;
import com.aivle.sellon.domain.proposal.dto.response.ProposalResponse;
import com.aivle.sellon.domain.proposal.entity.Proposal;
import com.aivle.sellon.domain.proposal.entity.ProposalAcceptHistory;
import com.aivle.sellon.domain.proposal.entity.ProposalEvidence;
import com.aivle.sellon.domain.proposal.enums.HitlStatus;
import com.aivle.sellon.domain.proposal.exception.ProposalAcceptHistoryNotFoundException;
import com.aivle.sellon.domain.proposal.exception.ProposalNotFoundException;
import com.aivle.sellon.domain.proposal.event.AlertDetectedEvent;
import com.aivle.sellon.domain.proposal.event.ProposalReviewEventPublisher;
import com.aivle.sellon.domain.proposal.event.ProposalReviewedEvent;
import com.aivle.sellon.domain.proposal.repository.ProposalAcceptHistoryRepository;
import com.aivle.sellon.domain.proposal.repository.ProposalEvidenceRepository;
import com.aivle.sellon.domain.proposal.repository.ProposalRepository;
import com.aivle.sellon.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProposalService {

    private final ProposalRepository proposalRepository;
    private final ProposalAcceptHistoryRepository proposalAcceptHistoryRepository;
    private final ProposalEvidenceRepository proposalEvidenceRepository;
    private final ProposalReviewEventPublisher proposalReviewEventPublisher;
    private final ProposalGenerationClient proposalGenerationClient;

    // 같은 회사 소속이면 루트/일반 계정 상관없이 조회 가능
    @Transactional(readOnly = true)
    public List<ProposalResponse> getProposals(Long companyId) {
        return proposalRepository.findByRootUser_Company_Id(companyId).stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public ProposalDetailResponse getProposalDetail(Long reportKey, Long companyId) {
        Proposal proposal = proposalRepository.findById(reportKey)
            .orElseThrow(ProposalNotFoundException::new);
        requireCompanyAccess(proposal, companyId);
        return toDetailResponse(proposal);
    }

    @Transactional
    public ProposalDetailResponse generateAndUpsertProposal(User rootUser, AlertDetectedEvent alertEvent) {
        ProposalGenerationResult result = proposalGenerationClient.generate(alertEvent.alertId());
        return upsertProposal(rootUser, alertEvent.alertId(), result);
    }

    @Transactional
    public ProposalDetailResponse regenerateProposal(Long reportKey, ProposalRegenerateRequest request, Long companyId) {
        Proposal proposal = proposalRepository.findById(reportKey)
            .orElseThrow(ProposalNotFoundException::new);
        requireCompanyAccess(proposal, companyId);

        proposalReviewEventPublisher.publish(toReviewedEvent(
            proposal,
            HitlStatus.REJECTED,
            request.processedBy(),
            LocalDateTime.now(),
            request.reasonCode(),
            request.reasonText(),
            null
        ));

        ProposalGenerationResult result = proposalGenerationClient.generate(proposal.getAlertId());
        return upsertProposal(proposal.getRootUser(), proposal.getAlertId(), result);
    }

    @Transactional
    public ProposalDetailResponse upsertProposal(User rootUser, String alertId, ProposalGenerationResult result) {
        Proposal proposal = proposalRepository.findByAlertId(alertId)
            .map(existing -> {
                existing.updateFromAi(result.recommendationId(), result.proposalUrl());
                return existing;
            })
            .orElseGet(() -> proposalRepository.save(
                Proposal.of(rootUser, alertId, result.recommendationId(), result.proposalUrl())
            ));

        proposal.applyReportContent(
            result.productSku(),
            result.productName(),
            result.csSummary(),
            result.confidenceLevel(),
            result.confidenceDescription(),
            result.similarCase(),
            result.proposedContent()
        );

        proposalEvidenceRepository.deleteByProposal_ReportKey(proposal.getReportKey());
        if (result.evidences() != null) {
            result.evidences().forEach(item -> proposalEvidenceRepository.save(
                ProposalEvidence.of(proposal, item.sourceField(), item.quoteText(), item.verified())
            ));
        }

        return toDetailResponse(proposal);
    }

    @Transactional
    public ProposalAcceptHistoryResponse acceptProposal(Long reportKey, ProposalAcceptRequest request, Long companyId) {
        Proposal proposal = proposalRepository.findById(reportKey)
            .orElseThrow(ProposalNotFoundException::new);
        requireCompanyAccess(proposal, companyId);

        boolean isEdited = request.improvedContent() != null && !request.improvedContent().isBlank();
        HitlStatus hitlStatus = isEdited ? HitlStatus.EDITED_APPROVED : HitlStatus.APPROVED;

        ProposalAcceptHistory history = ProposalAcceptHistory.ofAccept(
            proposal,
            hitlStatus,
            proposal.getProposedContent(),
            request.improvedContent(),
            request.improvedPrevContent(),
            request.processedBy()
        );
        proposalAcceptHistoryRepository.save(history);
        proposal.markReviewed(hitlStatus);

        publishReviewedEvent(proposal, history);

        return toResponse(history);
    }

    @Transactional
    public ProposalAcceptHistoryResponse rejectProposal(Long reportKey, ProposalRejectRequest request, Long companyId) {
        Proposal proposal = proposalRepository.findById(reportKey)
            .orElseThrow(ProposalNotFoundException::new);
        requireCompanyAccess(proposal, companyId);

        ProposalAcceptHistory history = ProposalAcceptHistory.ofReject(
            proposal,
            request.reasonCode(),
            request.reasonText(),
            request.processedBy()
        );
        proposalAcceptHistoryRepository.save(history);
        proposal.markReviewed(HitlStatus.REJECTED);

        publishReviewedEvent(proposal, history);

        return toResponse(history);
    }

    @Transactional(readOnly = true)
    public List<ProposalAcceptHistoryResponse> getAcceptHistory(Long reportKey, Long companyId) {
        Proposal proposal = proposalRepository.findById(reportKey)
            .orElseThrow(ProposalNotFoundException::new);
        requireCompanyAccess(proposal, companyId);

        return proposalAcceptHistoryRepository.findByProposal_ReportKey(reportKey).stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<ProposalAcceptHistoryResponse> getAllAcceptHistory(Long companyId) {
        return proposalAcceptHistoryRepository.findByProposal_RootUser_Company_IdOrderByProcessedAtDesc(companyId).stream()
            .map(this::toResponse)
            .toList();
    }

    // "되돌리기" — 실제 채널/상품 설명 반영 연동 전까지는 이력상 표시만 기록한다 (TODO: 연동 후 실제 반영 로직 추가)
    @Transactional
    public ProposalAcceptHistoryResponse rollbackAcceptHistory(Long historyKey, Long companyId) {
        ProposalAcceptHistory history = proposalAcceptHistoryRepository.findById(historyKey)
            .orElseThrow(ProposalAcceptHistoryNotFoundException::new);
        requireCompanyAccess(history.getProposal(), companyId);
        history.rollback();
        return toResponse(history);
    }

    // 같은 회사 소속인지 확인 — 다른 회사의 리포트/이력에 접근하지 못하도록 방지
    private void requireCompanyAccess(Proposal proposal, Long companyId) {
        if (!proposal.getRootUser().getCompany().getId().equals(companyId)) {
            throw new ProposalNotFoundException();
        }
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
        List<ProposalReviewedEvent.EvidenceItem> evidences = proposalEvidenceRepository
            .findByProposal_ReportKey(proposal.getReportKey()).stream()
            .map(e -> new ProposalReviewedEvent.EvidenceItem(e.getSourceField(), e.getQuoteText(), e.isVerified()))
            .toList();

        return new ProposalReviewedEvent(
            proposal.getRecommendationId(),
            proposal.getAlertId(),
            hitlStatus,
            processedAt,
            processedBy,
            rejectionReasonCode,
            rejectionReasonText,
            editedText,
            proposal.getProductSku(),
            proposal.getProductName(),
            proposal.getCsSummary(),
            proposal.getConfidenceLevel(),
            proposal.getConfidenceDescription(),
            proposal.getSimilarCase(),
            proposal.getProposedContent(),
            evidences
        );
    }

    private ProposalResponse toResponse(Proposal p) {
        return new ProposalResponse(p.getReportKey(), p.getAlertId(), p.getProposalUrl(), p.getHitlStatus());
    }

    private ProposalDetailResponse toDetailResponse(Proposal p) {
        List<ProposalEvidenceResponse> evidences = proposalEvidenceRepository
            .findByProposal_ReportKey(p.getReportKey()).stream()
            .map(e -> new ProposalEvidenceResponse(e.getSourceField(), e.getQuoteText(), e.isVerified()))
            .toList();

        return new ProposalDetailResponse(
            p.getReportKey(),
            p.getAlertId(),
            p.getProductSku(),
            p.getProductName(),
            p.getCsSummary(),
            p.getConfidenceLevel(),
            p.getConfidenceDescription(),
            p.getSimilarCase(),
            p.getProposedContent(),
            evidences,
            p.getHitlStatus()
        );
    }

    private ProposalAcceptHistoryResponse toResponse(ProposalAcceptHistory h) {
        return new ProposalAcceptHistoryResponse(
            h.getProposalAcceptHistoryKey(),
            h.getProposal().getReportKey(),
            h.getProposal().getProductName(),
            h.getHitlStatus(),
            h.getAppliedProposedContent(),
            h.getImprovedContent(),
            h.getImprovedPrevContent(),
            h.getRejectionReasonCode(),
            h.getRejectionReasonText(),
            h.getProcessedAt(),
            h.isRolledBack()
        );
    }
}
