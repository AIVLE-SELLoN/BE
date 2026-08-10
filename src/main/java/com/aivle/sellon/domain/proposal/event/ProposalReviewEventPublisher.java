package com.aivle.sellon.domain.proposal.event;

// feedback.recommendation.reviewed 이벤트 발행 인터페이스.
// 구현체는 RabbitProposalReviewEventPublisher (app.events 익스체인지로 실제 발행).
public interface ProposalReviewEventPublisher {
    void publish(ProposalReviewedEvent event);
}
