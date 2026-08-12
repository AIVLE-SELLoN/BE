package com.aivle.sellon.domain.proposal.event;

import com.aivle.sellon.global.mq.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class RabbitProposalReviewEventPublisher implements ProposalReviewEventPublisher {

    private static final String EVENT_TYPE = "feedback.recommendation.reviewed";
    private static final String SOURCE = "main-server";

    private final RabbitTemplate rabbitTemplate;

    @Override
    public void publish(ProposalReviewedEvent event) {
        Envelope envelope = new Envelope(
            UUID.randomUUID().toString(),
            EVENT_TYPE,
            Instant.now().toString(),
            SOURCE,
            UUID.randomUUID().toString(),
            toWirePayload(event)
        );

        log.info("[{}] 발행 - alertId={}, recommendationId={}, hitlStatus={}",
                EVENT_TYPE, event.alertId(), event.recommendationId(), event.hitlStatus());

        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME, EVENT_TYPE, envelope);
    }

    private Payload toWirePayload(ProposalReviewedEvent event) {
        Payload.HitlFeedback hitlFeedback = null;
        if (event.hitlFeedback() != null) {
            Payload.RejectionReason rejectionReason = event.hitlFeedback().rejectionReason() != null
                ? new Payload.RejectionReason(
                    event.hitlFeedback().rejectionReason().reasonCode(),
                    event.hitlFeedback().rejectionReason().reasonText())
                : null;

            hitlFeedback = new Payload.HitlFeedback(
                event.hitlFeedback().processedAt(),
                event.hitlFeedback().processedBy(),
                rejectionReason,
                event.hitlFeedback().editedText()
            );
        }

        return new Payload(
            event.recommendationId(),
            event.alertId(),
            event.hitlStatus().koreanValue(),
            hitlFeedback,
            toWireAlert(event.alert()),
            toWireRecommendation(event.recommendation())
        );
    }

    private Payload.Alert toWireAlert(ProposalReviewedEvent.AlertContext alert) {
        if (alert == null) return null;
        return new Payload.Alert(
            alert.detectedAt(),
            alert.productGroupId(),
            alert.channel() != null ? alert.channel().name() : null,
            alert.verdict() != null ? alert.verdict().koreanValue() : null,
            alert.mainAspect() != null ? alert.mainAspect().koreanValue() : null,
            alert.recommendedAction() != null ? alert.recommendedAction().koreanValue() : null
        );
    }

    private Payload.Recommendation toWireRecommendation(ProposalReviewedEvent.RecommendationContext recommendation) {
        if (recommendation == null) return null;

        Payload.Proposal proposal = new Payload.Proposal(
            recommendation.proposalType() != null ? recommendation.proposalType().toJson() : null,
            recommendation.targetField(),
            recommendation.currentText(),
            recommendation.proposedContent(),
            recommendation.rationale(),
            recommendation.detailpageGrounded()
        );

        List<Payload.Citation> citations = recommendation.citations() != null
            ? recommendation.citations().stream()
                .map(c -> new Payload.Citation(c.inquiryId(), c.quoteText()))
                .toList()
            : List.of();

        return new Payload.Recommendation(
            proposal,
            citations,
            new Payload.Evaluator(recommendation.evaluatorPassed()),
            recommendation.similarCase(),
            recommendation.confidenceLevel() != null ? recommendation.confidenceLevel().koreanValue() : null,
            recommendation.confidenceDescription(),
            recommendation.cappedByDetection()
        );
    }

    private record Envelope(
        String eventId,
        String eventType,
        String occurredAt,
        String source,
        String traceId,
        Payload payload
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    private record Payload(
        String recommendationId,
        String alertId,
        String hitlStatus,
        HitlFeedback hitlFeedback,
        Alert alert,
        Recommendation recommendation
    ) {
        @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
        record HitlFeedback(
            LocalDateTime processedAt,
            String processedBy,
            RejectionReason rejectionReason,
            String editedText
        ) {}

        @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
        record RejectionReason(
            String reasonCode,
            String reasonText
        ) {}

        // ai.anomaly.analyzed §4.1과 동일한 필드 구성 (백엔드가 저장해둔 원문을 그대로 되실어줌)
        @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
        record Alert(
            LocalDateTime detectedAt,
            String productGroupId,
            String channel,
            String verdict,
            String mainAspect,
            String recommendedAction
        ) {}

        // ai.anomaly.analyzed §4.2와 동일한 필드 구성
        @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
        record Recommendation(
            Proposal proposal,
            List<Citation> citations,
            Evaluator evaluator,
            String similarCase,
            String recommendationConfidence,
            String confidenceReason,
            boolean cappedByDetection
        ) {}

        @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
        record Proposal(
            String type,
            String targetField,
            String currentText,
            String proposedText,
            String rationale,
            boolean detailpageGrounded
        ) {}

        @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
        record Citation(
            String inquiryId,
            String quote
        ) {}

        @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
        record Evaluator(
            boolean passed
        ) {}
    }
}
