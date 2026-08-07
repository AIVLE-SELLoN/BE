package com.aivle.sellon.domain.proposal.dto.request;

// 개선안 반려 요청.
public record ProposalRejectRequest(
    String reasonCode,
    String reasonText,
    String processedBy
) {}
