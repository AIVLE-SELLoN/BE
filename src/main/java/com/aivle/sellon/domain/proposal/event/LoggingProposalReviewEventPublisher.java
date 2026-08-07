package com.aivle.sellon.domain.proposal.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

// 실제 브로커(RabbitMQ 등) 연동 코드가 아직 없어 그 전까지 로그로만 남긴다.
@Slf4j
@Component
public class LoggingProposalReviewEventPublisher implements ProposalReviewEventPublisher {

    @Override
    public void publish(ProposalReviewedEvent event) {
        log.info("[feedback.recommendation.reviewed] {}", event);
    }
}
