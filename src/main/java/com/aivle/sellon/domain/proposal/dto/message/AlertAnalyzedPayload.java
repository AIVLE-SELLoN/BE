package com.aivle.sellon.domain.proposal.dto.message;

import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

import java.time.OffsetDateTime;
import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record AlertAnalyzedPayload(
        String alertId,
        // AI가 오프셋을 붙여 발행한다(예: +09:00). LocalDateTime으로 받으면 파싱에
        // 실패해 DLQ로 떨어지므로 OffsetDateTime으로 받고, 저장 시점에 KST로 변환한다.
        OffsetDateTime detectedAt,
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

    // @JsonNaming은 하위 타입에 상속되지 않아, 중첩 record 각각에 별도로 붙여야
    // snake_case JSON(target_field, detailpage_grounded 등)이 정상 매핑된다.
    // (RabbitMQ로 실제 발행해보고서야 발견된 버그 — 전에는 다 null로 조용히 깨지고 있었음)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record EvidencePayload(
            List<String> inquiryIds,
            String linkedChangeId
    ) {
    }

    // recommended_action == "개선안 생성"일 때만 non-null (§4.2)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record RecommendationPayload(
            String recommendationId,
            String alertId,
            // 이 값은 어디서도 소비하지 않고 파싱만 되면 되지만, 오프셋이 붙어 오므로
            // 타입을 맞춰두지 않으면 이것 하나 때문에 메시지 전체가 DLQ로 떨어진다.
            OffsetDateTime createdAt,
            ProposalPayload proposal,
            List<CitationPayload> citations,
            EvaluatorPayload evaluator,
            String similarCase,
            String recommendationConfidence,
            String confidenceReason,
            boolean cappedByDetection
    ) {
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record ProposalPayload(
            String type,
            String targetField,
            String currentText,
            String proposedText,
            String rationale,
            boolean detailpageGrounded
    ) {
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record CitationPayload(
            String inquiryId,
            String quote
    ) {
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record EvaluatorPayload(
            boolean passed,
            int attempts,
            ChecksPayload checks,
            String failureReason
    ) {
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record ChecksPayload(
            boolean grounding,
            boolean consistency,
            boolean actionability
    ) {
    }
}
