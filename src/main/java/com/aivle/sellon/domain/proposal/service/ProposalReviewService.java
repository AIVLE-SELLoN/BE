package com.aivle.sellon.domain.proposal.service;

import com.aivle.sellon.domain.proposal.dto.request.ProposalAcceptRequest;
import com.aivle.sellon.domain.proposal.dto.request.ProposalRegenerateRequest;
import com.aivle.sellon.domain.proposal.dto.request.ProposalRejectRequest;
import com.aivle.sellon.domain.proposal.dto.response.ProposalAcceptHistoryResponse;
import com.aivle.sellon.domain.proposal.dto.response.ProposalDetailResponse;
import com.aivle.sellon.domain.proposal.entity.Proposal;
import com.aivle.sellon.domain.proposal.entity.ProposalAcceptHistory;
import com.aivle.sellon.domain.proposal.enums.HitlStatus;
import com.aivle.sellon.domain.proposal.event.ProposalReviewEventPublisher;
import com.aivle.sellon.domain.proposal.event.ProposalReviewedEvent;
import com.aivle.sellon.domain.proposal.exception.ProposalNotFoundException;
import com.aivle.sellon.domain.proposal.repository.ProposalAcceptHistoryRepository;
import com.aivle.sellon.domain.proposal.repository.ProposalRepository;
import com.aivle.sellon.domain.proposal.service.support.ProposalAccessGuard;
import com.aivle.sellon.domain.proposal.service.support.ProposalProductDescriptionApplier;
import com.aivle.sellon.domain.proposal.service.support.ProposalResponseMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

// 셀러의 개선안 검토 액션(재요청/승인/반려) — feedback.recommendation.reviewed 발행까지 담당.
@Service
@RequiredArgsConstructor
public class ProposalReviewService {

    private final ProposalRepository proposalRepository;
    private final ProposalAcceptHistoryRepository proposalAcceptHistoryRepository;
    private final ProposalReviewEventPublisher proposalReviewEventPublisher;
    private final ProposalProductDescriptionApplier productDescriptionApplier;
    private final ProposalAccessGuard accessGuard;
    private final ProposalResponseMapper responseMapper;

    // [확정] "분석 재요청" — AI에게 "지금 당장 다시 만들어달라"고 동기 호출하는 API는 없다.
    // 반려 사유를 실어 feedback.recommendation.reviewed를 발행하는 것 자체가 트랜스포트다 — AI는 이
    // 이벤트 이력으로 반려 사례 벡터DB를 쌓고 resolved_alert_ids를 자체 판단해 다음 배치에서 재분석한다(§8).
    // 백엔드가 별도로 만들 API는 없다. 새 리포트는 다음 ai.anomaly.analyzed 이벤트가 오면
    // ProposalIngestService가 갱신한다.
    @Transactional
    public ProposalDetailResponse regenerateProposal(Long reportKey, ProposalRegenerateRequest request, Long companyId) {
        Proposal proposal = proposalRepository.findById(reportKey)
            .orElseThrow(ProposalNotFoundException::new);
        accessGuard.requireCompanyAccess(proposal, companyId);

        proposal.markReviewed(HitlStatus.REJECTED);

        proposalReviewEventPublisher.publish(toReviewedEvent(
            proposal,
            HitlStatus.REJECTED,
            request.processedBy(),
            LocalDateTime.now(),
            request.reasonCode(),
            request.reasonText(),
            null
        ));

        return responseMapper.toDetailResponse(proposal);
    }

    @Transactional
    public ProposalAcceptHistoryResponse acceptProposal(Long reportKey, ProposalAcceptRequest request, Long companyId) {
        Proposal proposal = proposalRepository.findById(reportKey)
            .orElseThrow(ProposalNotFoundException::new);
        accessGuard.requireCompanyAccess(proposal, companyId);

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

        // 실제 반영 — 수정 문구가 있으면 그걸, 없으면 AI 제안 원문을 상품 설명에 저장한다.
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

    // mq_events.md §8 — recommendation_id/alert_id/hitl_status/hitl_feedback 4개 최상위 필드로 발행.
    // 승인(수정 없이)일 때는 rejectionReason이 없어 null로 둔다.
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
            hitlFeedback
        );
    }
}
