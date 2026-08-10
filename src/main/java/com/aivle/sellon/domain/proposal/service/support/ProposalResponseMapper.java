package com.aivle.sellon.domain.proposal.service.support;

import com.aivle.sellon.domain.proposal.dto.response.ProposalAcceptHistoryResponse;
import com.aivle.sellon.domain.proposal.dto.response.ProposalDetailResponse;
import com.aivle.sellon.domain.proposal.dto.response.ProposalEvidenceResponse;
import com.aivle.sellon.domain.proposal.dto.response.ProposalResponse;
import com.aivle.sellon.domain.proposal.entity.Proposal;
import com.aivle.sellon.domain.proposal.entity.ProposalAcceptHistory;
import com.aivle.sellon.domain.proposal.repository.ProposalEvidenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

// Proposal/ProposalAcceptHistory 엔티티 -> 응답 DTO 매핑. Query/Ingest/Review/History 네 서비스가 공통으로 쓴다.
@Component
@RequiredArgsConstructor
public class ProposalResponseMapper {

    private final ProposalEvidenceRepository proposalEvidenceRepository;

    public ProposalResponse toResponse(Proposal p) {
        return new ProposalResponse(p.getReportKey(), p.getAlertId(), p.getProductGroupId(), p.getMainAspect(), p.getHitlStatus());
    }

    public ProposalDetailResponse toDetailResponse(Proposal p) {
        List<ProposalEvidenceResponse> evidences = proposalEvidenceRepository
            .findByProposal_ReportKey(p.getReportKey()).stream()
            .map(e -> new ProposalEvidenceResponse(e.getInquiryId(), e.getQuoteText()))
            .toList();

        return new ProposalDetailResponse(
            p.getReportKey(),
            p.getAlertId(),
            p.getDetectedAt(),
            p.getProductGroupId(),
            p.getChannel(),
            p.getVerdict(),
            p.getMainAspect(),
            p.getProposalType(),
            p.getTargetField(),
            p.getCurrentText(),
            p.getRationale(),
            p.getConfidenceLevel(),
            p.getConfidenceDescription(),
            p.isCappedByDetection(),
            p.getSimilarCase(),
            p.isDetailpageGrounded(),
            p.isEvaluatorPassed(),
            p.getProposedContent(),
            evidences,
            p.getHitlStatus()
        );
    }

    public ProposalAcceptHistoryResponse toResponse(ProposalAcceptHistory h) {
        return new ProposalAcceptHistoryResponse(
            h.getProposalAcceptHistoryKey(),
            h.getProposal().getReportKey(),
            h.getProposal().getProductGroupId(),
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
