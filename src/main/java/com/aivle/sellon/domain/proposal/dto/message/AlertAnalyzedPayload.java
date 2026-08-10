package com.aivle.sellon.domain.proposal.dto.message;

import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

import java.time.LocalDateTime;
import java.util.List;

// ai.anomaly.analyzed 이벤트 payload (mq_events.md §4.1, §4.2).
// 개선안 리포트(Proposal)에 필요한 필드만 옮겼다 — window_start/end, significant_channels,
// excluded_channels, channel_rates, sub_aspects, stats(p_value 등 셀러 노출 금지 필드 포함),
// source_signals, root_cause는 탐지 상세/대시보드 영역이라 지금 범위에서는 제외.
// verdict/main_aspect/recommended_action은 JSON에 한글 값이 실려서 String으로 받고
// 각 enum의 fromKorean()으로 변환한다(핸들러에서 수행). channel은 값이 영문이라 Jackson이 바로 바인딩한다.
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

    // type은 copy_draft/image_guide 소문자라 ProposalType.fromJson()으로 변환한다.
    public record ProposalPayload(
            String type,
            String targetField,
            String currentText,
            String proposedText,
            String rationale,
            boolean detailpageGrounded
    ) {
    }

    // 현재 AI측에서 citations는 항상 빈 배열로 온다(원문-DB PK 연결 미확인, mq_events.md §4.2 주의사항).
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
