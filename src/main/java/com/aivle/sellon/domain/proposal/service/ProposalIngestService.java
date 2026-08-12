package com.aivle.sellon.domain.proposal.service;

import com.aivle.sellon.domain.proposal.dto.message.AlertAnalyzedPayload;
import com.aivle.sellon.domain.proposal.entity.Proposal;
import com.aivle.sellon.domain.proposal.entity.ProposalEvidence;
import com.aivle.sellon.domain.proposal.enums.Channel;
import com.aivle.sellon.domain.proposal.enums.ConfidenceLevel;
import com.aivle.sellon.domain.proposal.enums.HitlStatus;
import com.aivle.sellon.domain.proposal.enums.MainAspect;
import com.aivle.sellon.domain.proposal.enums.ProposalType;
import com.aivle.sellon.domain.proposal.enums.RecommendedAction;
import com.aivle.sellon.domain.proposal.enums.Verdict;
import com.aivle.sellon.domain.proposal.repository.ProposalEvidenceRepository;
import com.aivle.sellon.domain.proposal.repository.ProposalRepository;
import com.aivle.sellon.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// ai.anomaly.analyzed 이벤트로 들어온 개선안을 alertId 기준으로 upsert. AlertDetectedHandler 전용 진입점.
@Slf4j
@Service
@RequiredArgsConstructor
public class ProposalIngestService {

    private final ProposalRepository proposalRepository;
    private final ProposalEvidenceRepository proposalEvidenceRepository;

    @Transactional
    public void ingestAnalyzedAlert(User rootUser, AlertAnalyzedPayload payload) {
        Proposal proposal = proposalRepository.findByAlertId(payload.alertId())
            .orElseGet(() -> proposalRepository.save(Proposal.of(rootUser, payload.alertId())));

        // 같은 alert_id로 재발행이 와도, 셀러가 이미 처리(승인/수정후승인/반려)한 개선안은 덮어쓰지 않는다.
        // (AI팀 확인 요청 반영 — PENDING일 때만 분석 결과를 반영한다)
        if (proposal.getHitlStatus() != HitlStatus.PENDING) {
            log.info("이미 처리된 개선안이라 재발행 무시 - alertId={}, hitlStatus={}",
                    payload.alertId(), proposal.getHitlStatus());
            return;
        }

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
