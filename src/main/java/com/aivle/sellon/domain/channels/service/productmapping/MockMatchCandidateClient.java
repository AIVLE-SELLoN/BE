package com.aivle.sellon.domain.channels.service.productmapping;

import com.aivle.sellon.domain.channels.entity.productmapping.MasterProduct;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * MatchCandidateClient의 임시 구현. 실제 AI 서버 연동 전까지는 후보를 내려주지 않는다.
 * TODO: AI 서버 스펙 확정되면 실제 RestClient 기반 구현으로 교체.
 */
@Component
public class MockMatchCandidateClient implements MatchCandidateClient {

    @Override
    public List<ScoredCandidate> rank(String channelProductName, List<MasterProduct> candidates) {
        return List.of();
    }
}
