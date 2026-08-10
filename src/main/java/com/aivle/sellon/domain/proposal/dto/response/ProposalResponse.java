package com.aivle.sellon.domain.proposal.dto.response;

import com.aivle.sellon.domain.proposal.enums.HitlStatus;

public record ProposalResponse(
    Long reportKey,
    String alertId,
    String proposalUrl,
    HitlStatus hitlStatus
) {}
