package com.aivle.sellon.domain.proposal.dto.response;

import com.aivle.sellon.domain.proposal.enums.HitlStatus;

import java.time.LocalDateTime;

public record ProposalAcceptHistoryResponse(
    Long proposalAcceptHistoryKey,
    Long reportKey,
    String productGroupId,
    HitlStatus hitlStatus,
    String appliedProposedContent,
    String improvedContent,
    String improvedPrevContent,
    String rejectionReasonCode,
    String rejectionReasonText,
    LocalDateTime processedAt,
    boolean rolledBack
) {}
