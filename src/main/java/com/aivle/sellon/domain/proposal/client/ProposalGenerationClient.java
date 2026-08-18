package com.aivle.sellon.domain.proposal.client;

// AI 노드의 개선안 생성 API(/generate) 호출 클라이언트.
public interface ProposalGenerationClient {
    ProposalGenerationResult generate(String alertId);
}
