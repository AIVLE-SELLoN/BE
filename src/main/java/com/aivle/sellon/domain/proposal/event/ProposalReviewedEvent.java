package com.aivle.sellon.domain.proposal.event;

import com.aivle.sellon.domain.proposal.enums.HitlStatus;

import java.time.LocalDateTime;

public record ProposalReviewedEvent(
    String recommendationId,
    String alertId,
    HitlStatus hitlStatus,
    HitlFeedback hitlFeedback
) {
    public record HitlFeedback(
        LocalDateTime processedAt,
        String processedBy,
        RejectionReason rejectionReason,
        String editedText
    ) {}

    // 승인(수정 없이)일 때는 사유가 없으므로 rejectionReason 자체가 null.
    public record RejectionReason(
        String reasonCode,
        String reasonText
    ) {}
}
