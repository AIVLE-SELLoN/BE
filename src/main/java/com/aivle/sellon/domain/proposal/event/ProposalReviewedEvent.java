package com.aivle.sellon.domain.proposal.event;

import com.aivle.sellon.domain.proposal.enums.ConfidenceLevel;
import com.aivle.sellon.domain.proposal.enums.HitlStatus;

import java.time.LocalDateTime;
import java.util.List;

public record ProposalReviewedEvent(
    String recommendationId,
    String alertId,
    HitlStatus hitlStatus,
    LocalDateTime processedAt,
    String processedBy,
    String rejectionReasonCode,
    String rejectionReasonText,
    String editedText,

    String productSku,
    String productName,
    String csSummary,
    ConfidenceLevel confidenceLevel,
    String confidenceDescription,
    String similarCase,
    String proposedContent,
    List<EvidenceItem> evidences
) {
    public record EvidenceItem(
        String sourceField,
        String quoteText,
        boolean verified
    ) {}
}
