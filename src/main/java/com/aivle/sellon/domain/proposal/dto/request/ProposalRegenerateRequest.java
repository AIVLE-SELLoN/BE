package com.aivle.sellon.domain.proposal.dto.request;

// 개선안 재생성 요청.
public record ProposalRegenerateRequest(
    String reasonCode,
    String reasonText,
    String processedBy
) {}
