package com.aivle.sellon.domain.proposal.event;

import com.aivle.sellon.domain.proposal.enums.HitlStatus;

import java.time.LocalDateTime;

// AI가 HITL 학습 사례를 벡터DB에 적재할 수 있도록,
// ai.anomaly.analyzed로 받은 payload 원문을 가공 없이 그대로 되실어준다("alert": payload
// 최상위 그대로, "recommendation": 그 payload의 recommendation 객체 그대로). 그래서 여기서도
// 타입을 잘게 쪼갠 커스텀 구조 대신 저장해둔 원본 JSON 텍스트를 그대로 들고 다닌다.
public record ProposalReviewedEvent(
    String recommendationId,
    String alertId,
    HitlStatus hitlStatus,
    HitlFeedback hitlFeedback,
    String rawPayloadJson
) {
    public record HitlFeedback(
        LocalDateTime processedAt,
        String processedBy,
        RejectionReason rejectionReason,
        String editedText
    ) {}

    public record RejectionReason(
        String reasonCode,
        String reasonText
    ) {}
}
