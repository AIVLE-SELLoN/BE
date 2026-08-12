package com.aivle.sellon.domain.alert.mq;

import com.aivle.sellon.domain.alert.dto.message.AlertAnalyzedPayload;
import com.aivle.sellon.domain.alert.service.AlertIngestService;
import com.aivle.sellon.domain.company.exception.CompanyNotFoundException;
import com.aivle.sellon.global.mq.enums.EventFailureReason;
import com.aivle.sellon.global.mq.listener.MqEventHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class AlertAnalyzedEventHandler implements MqEventHandler {

    private static final String ANOMALY_ANALYZED_EVENT_TYPE = "ai.anomaly.analyzed";

    private final AlertIngestService alertIngestService;
    private final JsonMapper jsonMapper;

    @Override
    public boolean supports(String eventType) {
        return ANOMALY_ANALYZED_EVENT_TYPE.equals(eventType);
    }

    @Override
    public void handle(JsonNode envelope, String eventId, String traceId) {
        String companyKey = envelope.path("companyId").asString(null);

        AlertAnalyzedPayload payload;
        try {
            payload = readPayload(envelope);
        } catch (JacksonException | IllegalArgumentException e) {
            throw deadLetter(EventFailureReason.MALFORMED_PAYLOAD, eventId, traceId, e);
        }

        try {
            alertIngestService.ingest(payload, companyKey);
        } catch (CompanyNotFoundException e) {
            throw deadLetter(EventFailureReason.UNKNOWN_COMPANY, eventId, traceId, e);
        }
    }

    private AlertAnalyzedPayload readPayload(JsonNode envelope) {
        JsonNode payloadNode = envelope.get("payload");
        if (payloadNode == null || payloadNode.isNull()) {
            throw new IllegalArgumentException("payload is missing");
        }

        AlertAnalyzedPayload payload = jsonMapper.treeToValue(payloadNode, AlertAnalyzedPayload.class);
        validate(payload);
        return payload;
    }

    private void validate(AlertAnalyzedPayload payload) {
        requireText(payload.alertId(), "alert_id");
        requireValue(payload.detectedAt(), "detected_at");
        requireText(payload.productGroupId(), "product_group_id");
        requireValue(payload.channel(), "channel");
        requireValue(payload.windowStart(), "window_start");
        requireValue(payload.windowEnd(), "window_end");
        requireValue(payload.verdict(), "verdict");
        requireValue(payload.mainAspect(), "main_aspect");
        requireValue(payload.stats(), "stats");
        requireValue(payload.stats().source(), "stats.source");
        requireValue(payload.stats().curRate(), "stats.cur_rate");
        requireValue(payload.stats().pastRate(), "stats.past_rate");
        requireValue(payload.stats().delta(), "stats.delta");
        requireValue(payload.stats().bhSignificant(), "stats.bh_significant");
        requireValue(payload.stats().curTotal(), "stats.cur_total");
        requireValue(payload.detectionConfidence(), "detection_confidence");
        requireValue(payload.scopeIn(), "scope_in");
        requireValue(payload.recommendedAction(), "recommended_action");
        requireValue(payload.evidence(), "evidence");
        requireValue(payload.evidence().inquiryIds(), "evidence.inquiry_ids");
    }

    private void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is missing");
        }
    }

    private void requireValue(Object value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " is missing");
        }
    }

    private AmqpRejectAndDontRequeueException deadLetter(
            EventFailureReason reason, String eventId, String traceId, Exception cause
    ) {
        log.error("Event moved to DLQ - reason={}, detail={}, eventType={}, eventId={}, traceId={}",
                reason, reason.getDescription(), ANOMALY_ANALYZED_EVENT_TYPE, eventId, traceId, cause);
        return new AmqpRejectAndDontRequeueException("%s (eventId=%s)".formatted(reason, eventId), cause);
    }
}
