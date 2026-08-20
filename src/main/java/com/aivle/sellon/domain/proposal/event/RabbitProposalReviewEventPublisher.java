package com.aivle.sellon.domain.proposal.event;

import com.aivle.sellon.global.mq.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class RabbitProposalReviewEventPublisher implements ProposalReviewEventPublisher {

    private static final String EVENT_TYPE = "feedback.recommendation.reviewed";
    private static final String SOURCE = "main-server";

    private final RabbitTemplate rabbitTemplate;
    private final JsonMapper jsonMapper;

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

    // alert/recommendation은 가공하지 않고 저장해둔 원본 그대로
    // 되실어야 해서, 커스텀 DTO로 재조합하지 않고 원본 JSON을 파싱한 JsonNode를 그대로 얹는다.
    // "alert"는 ai.anomaly.analyzed payload 최상위 전체(그 안의 recommendation 포함) 그대로,
    // "recommendation"은 그 payload의 recommendation 객체만 따로 뽑은 것.
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

        JsonNode alertNode = event.rawPayloadJson() != null ? jsonMapper.readTree(event.rawPayloadJson()) : null;
        JsonNode recommendationNode = alertNode != null ? alertNode.get("recommendation") : null;

        return new Payload(
            event.recommendationId(),
            event.alertId(),
            event.hitlStatus().koreanValue(),
            hitlFeedback,
            alertNode,
            recommendationNode
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
        JsonNode alert,
        JsonNode recommendation
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
    }
}
