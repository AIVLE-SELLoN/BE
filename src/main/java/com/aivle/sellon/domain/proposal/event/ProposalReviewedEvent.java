package com.aivle.sellon.domain.proposal.event;

import com.aivle.sellon.domain.proposal.enums.Channel;
import com.aivle.sellon.domain.proposal.enums.ConfidenceLevel;
import com.aivle.sellon.domain.proposal.enums.HitlStatus;
import com.aivle.sellon.domain.proposal.enums.MainAspect;
import com.aivle.sellon.domain.proposal.enums.ProposalType;
import com.aivle.sellon.domain.proposal.enums.RecommendedAction;
import com.aivle.sellon.domain.proposal.enums.Verdict;

import java.time.LocalDateTime;
import java.util.List;

public record ProposalReviewedEvent(
    String recommendationId,
    String alertId,
    HitlStatus hitlStatus,
    HitlFeedback hitlFeedback,
    AlertContext alert,
    RecommendationContext recommendation
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

    public record AlertContext(
        LocalDateTime detectedAt,
        String productGroupId,
        Channel channel,
        Verdict verdict,
        MainAspect mainAspect,
        RecommendedAction recommendedAction
    ) {}

    public record RecommendationContext(
        ProposalType proposalType,
        String targetField,
        String currentText,
        String proposedContent,
        String rationale,
        boolean detailpageGrounded,
        ConfidenceLevel confidenceLevel,
        String confidenceDescription,
        String similarCase,
        boolean cappedByDetection,
        boolean evaluatorPassed,
        List<Citation> citations
    ) {
        public record Citation(String inquiryId, String quoteText) {}
    }
}
