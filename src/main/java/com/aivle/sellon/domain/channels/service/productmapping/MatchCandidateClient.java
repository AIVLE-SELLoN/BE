package com.aivle.sellon.domain.channels.service.productmapping;

import com.aivle.sellon.domain.channels.entity.productmapping.MasterProduct;

import java.util.List;

/**
 * 상품명 임베딩 유사도 계산은 AI 서버(Agent) 담당 — main server↔AI 통신 경계에 따라
 * 이 클라이언트는 채널 상품명과 후보 마스터 상품 목록을 넘기고 유사도 점수를 받아온다.
 * TODO: 실제 AI 서버 API 스펙 확정되면 RestClient 호출로 교체.
 */
public interface MatchCandidateClient {

    List<ScoredCandidate> rank(String channelProductName, List<MasterProduct> candidates);

    record ScoredCandidate(MasterProduct masterProduct, double similarityScore) {
    }
}
