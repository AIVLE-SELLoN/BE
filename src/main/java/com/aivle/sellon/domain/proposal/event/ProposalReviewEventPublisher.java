package com.aivle.sellon.domain.proposal.event;

// feedback.recommendation.reviewed 이벤트 발행 인터페이스.
// TODO: 실제 브로커 연동 구현체로 교체 — 그 전까지는 로그만 남기는 LoggingProposalReviewEventPublisher 사용
public interface ProposalReviewEventPublisher {
    void publish(ProposalReviewedEvent event);
}
