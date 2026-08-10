package com.aivle.sellon.domain.proposal.dto.request;

public record ProposalRegenerateRequest(
    String reasonCode,
    String reasonText,
    String processedBy
) {}
