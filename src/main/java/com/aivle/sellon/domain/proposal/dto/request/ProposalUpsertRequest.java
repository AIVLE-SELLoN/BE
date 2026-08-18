package com.aivle.sellon.domain.proposal.dto.request;

public record ProposalUpsertRequest(
    String alertId,
    String recommendationId,
    String proposalUrl
) {}
