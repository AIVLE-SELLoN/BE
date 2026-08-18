package com.aivle.sellon.domain.proposal.dto.request;

public record ProposalRejectRequest(
    String reasonCode,
    String reasonText,
    String processedBy
) {}
