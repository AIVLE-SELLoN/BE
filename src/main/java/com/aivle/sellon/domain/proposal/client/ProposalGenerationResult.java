package com.aivle.sellon.domain.proposal.client;

import com.aivle.sellon.domain.proposal.enums.ConfidenceLevel;

import java.util.List;

// TODO: 실제 개선안 API 명세서 확정 후 필드명/구조 재확인 필요
public record ProposalGenerationResult(
    String recommendationId,
    String proposalUrl,
    String productSku,
    String productName,
    String csSummary,
    ConfidenceLevel confidenceLevel,
    String confidenceDescription,
    String similarCase,
    String proposedContent,
    List<EvidenceItem> evidences
) {
    // 근거 데이터(상세페이지 인용) 한 건
    public record EvidenceItem(
        String sourceField,
        String quoteText,
        boolean verified
    ) {}
}
