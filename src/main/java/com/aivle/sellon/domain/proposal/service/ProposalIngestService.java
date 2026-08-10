package com.aivle.sellon.domain.proposal.service;

import com.aivle.sellon.domain.proposal.dto.message.AlertAnalyzedPayload;
import com.aivle.sellon.domain.proposal.entity.Proposal;
import com.aivle.sellon.domain.proposal.entity.ProposalEvidence;
import com.aivle.sellon.domain.proposal.enums.Channel;
import com.aivle.sellon.domain.proposal.enums.ConfidenceLevel;
import com.aivle.sellon.domain.proposal.enums.MainAspect;
import com.aivle.sellon.domain.proposal.enums.ProposalType;
import com.aivle.sellon.domain.proposal.enums.RecommendedAction;
import com.aivle.sellon.domain.proposal.enums.Verdict;
import com.aivle.sellon.domain.proposal.repository.ProposalEvidenceRepository;
import com.aivle.sellon.domain.proposal.repository.ProposalRepository;
import com.aivle.sellon.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// ai.anomaly.analyzed 이벤트로 들어온 개선안을 alertId 기준으로 upsert. AlertDetectedHandler 전용 진입점.
@Service
@RequiredArgsConstructor
public class ProposalIngestService {

    private final ProposalRepository proposalRepository;
    private final ProposalEvidenceRepository proposalEvidenceRepository;

    // AI가 배치에서 생성까지 끝낸 뒤 발행하는 구조라 여기서 AI를 추가로 호출하지 않는다.
    // 컨슈머(AlertDetectedHandler)에서만 호출되고 응답을 돌려줄 곳이 없어 void — 반환값이 필요하면
    // ProposalQueryService.getProposalDetail()로 별도 조회한다.
    @Transactional
    public void ingestAnalyzedAlert(User rootUser, AlertAnalyzedPayload payload) {
        Proposal proposal = proposalRepository.findByAlertId(payload.alertId())
            .orElseGet(() -> proposalRepository.save(Proposal.of(rootUser, payload.alertId())));

        AlertAnalyzedPayload.RecommendationPayload recommendation = payload.recommendation();
        AlertAnalyzedPayload.ProposalPayload proposalContent = recommendation.proposal();
        AlertAnalyzedPayload.EvaluatorPayload evaluator = recommendation.evaluator();

        proposal.applyAnalysisResult(
            payload.detectedAt(),
            payload.productGroupId(),
            Channel.valueOf(payload.channel()),
            Verdict.fromKorean(payload.verdict()),
            MainAspect.fromKorean(payload.mainAspect()),
            RecommendedAction.fromKorean(payload.recommendedAction()),
            recommendation.recommendationId(),
            proposalContent != null ? ProposalType.fromJson(proposalContent.type()) : null,
            proposalContent != null ? proposalContent.targetField() : null,
            proposalContent != null ? proposalContent.currentText() : null,
            proposalContent != null ? proposalContent.proposedText() : null,
            proposalContent != null ? proposalContent.rationale() : null,
            proposalContent != null && proposalContent.detailpageGrounded(),
            ConfidenceLevel.fromKorean(recommendation.recommendationConfidence()),
            recommendation.confidenceReason(),
            recommendation.similarCase(),
            recommendation.cappedByDetection(),
            evaluator != null && evaluator.passed()
        );

        proposalEvidenceRepository.deleteByProposal_ReportKey(proposal.getReportKey());
        if (recommendation.citations() != null) {
            recommendation.citations().forEach(c -> proposalEvidenceRepository.save(
                ProposalEvidence.of(proposal, c.inquiryId(), c.quote())
            ));
        }
    }
}
