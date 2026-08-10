package com.aivle.sellon.domain.proposal.dto.response;

import com.aivle.sellon.domain.proposal.enums.HitlStatus;

import java.time.LocalDateTime;

public record ProposalAcceptHistoryResponse(
    Long proposalAcceptHistoryKey,
    Long reportKey,
    String productName, // 개선안 히스토리 목록 표시용
    HitlStatus hitlStatus,
    String appliedProposedContent, // 적용된 개선안(승인 시점 스냅샷)
    String improvedContent,
    String improvedPrevContent,
    String rejectionReasonCode,
    String rejectionReasonText,
    LocalDateTime processedAt,
    boolean rolledBack
) {}
