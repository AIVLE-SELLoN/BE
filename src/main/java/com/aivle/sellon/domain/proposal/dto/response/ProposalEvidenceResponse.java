package com.aivle.sellon.domain.proposal.dto.response;

public record ProposalEvidenceResponse(
    String sourceField,
    String quoteText,
    boolean verified
) {}
