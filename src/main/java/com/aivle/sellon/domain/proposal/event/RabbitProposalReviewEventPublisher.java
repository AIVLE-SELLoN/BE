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
import java.util.UUID;

// feedback.recommendation.reviewed 실제 발행 구현체 (mq_events.md §3, §8).
// main.inbound(수신)와 반대로 여긴 우리가 app.events 익스체인지에 직접 발행한다 —
// 큐/바인딩(ai.inbound에 feedback.# 추가)은 인프라 CRD가 만드는 대상이라 여기서 선언하지 않는다(§2.1).
// 참고할 기존 발행 예제가 프로젝트에 없어서(월간 리포트는 수신만 구현됨) RabbitMQConfig의
// exchange 정보만으로 새로 작성했다.
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

        // 라우팅 키 = eventType. 토픽 익스체인지 바인딩(feedback.#)이 ai.inbound로 넘겨준다.
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME, EVENT_TYPE, envelope);
    }

    // 도메인 이벤트(HitlStatus enum)를 계약 그대로의 와이어 포맷(payload는 snake_case,
    // hitl_status는 한글 문자열)으로 변환한다.
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
            hitlFeedback
        );
    }

    // envelope 자체는 camelCase(§3) 그대로라 어노테이션이 필요 없다.
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
        HitlFeedback hitlFeedback
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
