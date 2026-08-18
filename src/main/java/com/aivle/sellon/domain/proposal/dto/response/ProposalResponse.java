package com.aivle.sellon.domain.proposal.dto.response;

import com.aivle.sellon.domain.proposal.enums.HitlStatus;
import com.aivle.sellon.domain.proposal.enums.MainAspect;

public record ProposalResponse(
    Long reportKey,
    String alertId,
    String productGroupId,
    MainAspect mainAspect,
    HitlStatus hitlStatus
) {}
