package com.aivle.sellon.domain.proposal.dto.response;

import com.aivle.sellon.domain.proposal.enums.ConfidenceLevel;
import com.aivle.sellon.domain.proposal.enums.HitlStatus;

import java.util.List;

// AI 인사이트 리포트 첫 페이지 상세 조회 응답
public record ProposalDetailResponse(
    Long reportKey,
    String alertId,
    String productSku,
    String productName,
    String csSummary,
    ConfidenceLevel confidenceLevel,
    String confidenceDescription,
    String similarCase,
    String proposedContent, // 개선안 입력창 초기값
    List<ProposalEvidenceResponse> evidences,
    HitlStatus hitlStatus
) {}
