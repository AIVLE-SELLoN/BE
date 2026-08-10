package com.aivle.sellon.domain.proposal.dto.message;

import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

import java.time.LocalDateTime;
import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record AlertAnalyzedPayload(
        String alertId,
        LocalDateTime detectedAt,
        String updatesAlertId,
        String productGroupId,
        String channel,
        String verdict,
        String mainAspect,
        boolean scopeIn,
        String recommendedAction,
        EvidencePayload evidence,
        RecommendationPayload recommendation
) {

    public record EvidencePayload(
            List<String> inquiryIds,
            String linkedChangeId
    ) {
    }

    // recommended_action == "개선안 생성"일 때만 non-null (§4.2)
    public record RecommendationPayload(
            String recommendationId,
            String alertId,
            LocalDateTime createdAt,
            ProposalPayload proposal,
            List<CitationPayload> citations,
            EvaluatorPayload evaluator,
            String similarCase,
            String recommendationConfidence,
            String confidenceReason,
            boolean cappedByDetection
    ) {
    }

    public record ProposalPayload(
            String type,
            String targetField,
            String currentText,
            String proposedText,
            String rationale,
            boolean detailpageGrounded
    ) {
    }

    public record CitationPayload(
            String inquiryId,
            String quote
    ) {
    }

    public record EvaluatorPayload(
            boolean passed,
            int attempts,
            ChecksPayload checks,
            String failureReason
    ) {
    }

    public record ChecksPayload(
            boolean grounding,
            boolean consistency,
            boolean actionability
    ) {
    }
}
