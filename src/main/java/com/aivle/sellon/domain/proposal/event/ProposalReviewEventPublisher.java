package com.aivle.sellon.domain.proposal.event;

public interface ProposalReviewEventPublisher {
    void publish(ProposalReviewedEvent event);
}
