package com.aivle.sellon.domain.proposal.client;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

// TODO: 실제 AI 서비스 URL/요청·응답 스펙(개선안 API 명세서) 확정 후 교체 필요
@Component
public class RestProposalGenerationClient implements ProposalGenerationClient {

    private final RestClient restClient = RestClient.create("http://localhost:8082");

    @Override
    public ProposalGenerationResult generate(String alertId) {
        return restClient.post()
            .uri("/recommendations/generate")
            .body(Map.of("alert_id", alertId))
            .retrieve()
            .body(ProposalGenerationResult.class);
    }
}
