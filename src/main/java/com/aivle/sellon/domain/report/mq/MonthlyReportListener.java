package com.aivle.sellon.domain.report.mq;

import com.aivle.sellon.domain.company.exception.CompanyNotFoundException;
import com.aivle.sellon.domain.report.dto.message.MonthlyReportPayload;
import com.aivle.sellon.domain.report.service.ReportService;
import com.aivle.sellon.global.mq.config.RabbitMQConfig;
import com.aivle.sellon.global.mq.enums.EventFailureReason;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class MonthlyReportListener {

    private static final String REPORT_GENERATED_EVENT_TYPE = "ai.report.generated";

    private final ReportService reportService;
    private final JsonMapper jsonMapper;

    @RabbitListener(queues = RabbitMQConfig.MAIN_INBOUND_QUEUE)
    public void onAiEvent(JsonNode envelope) {
        String eventType = envelope.path("eventType").asString(null);
        String eventId = envelope.path("eventId").asString(null);
        String traceId = envelope.path("traceId").asString(null);

        // main.inbound는 ai.# 전체를 받는데 현재 컨슈머는 이것 하나뿐이라, 처리하지 않는 이벤트는
        // ack 후 사라진다. 나중에 컨슈머가 추가되기 전까지 무엇이 유실되는지 남겨둔다.
        if (!REPORT_GENERATED_EVENT_TYPE.equals(eventType)) {
            log.warn("처리 대상이 아닌 이벤트 폐기 - eventType={}, eventId={}, traceId={}",
                    eventType, eventId, traceId);
            return;
        }

        // 파싱과 저장을 한 try에 두면 서비스 내부에서 난 IllegalArgumentException이
        // MALFORMED_PAYLOAD로 오분류돼 재시도 없이 DLQ로 직행한다. 범위를 나눠 둔다.
        MonthlyReportPayload payload;
        try {
            payload = readPayload(envelope);
        } catch (JacksonException | IllegalArgumentException e) {
            throw deadLetter(EventFailureReason.MALFORMED_PAYLOAD, eventId, traceId, e);
        }

        try {
            reportService.saveGeneratedReport(payload);
        } catch (CompanyNotFoundException e) {
            throw deadLetter(EventFailureReason.UNKNOWN_COMPANY, eventId, traceId, e);
        }
    }

    private MonthlyReportPayload readPayload(JsonNode envelope) {
        JsonNode payloadNode = envelope.get("payload");
        if (payloadNode == null || payloadNode.isNull())
            throw new IllegalArgumentException("payload 없음");

        MonthlyReportPayload payload = jsonMapper.treeToValue(payloadNode, MonthlyReportPayload.class);
        validate(payload);
        return payload;
    }

    /**
     * 필수 필드가 비면 저장 단계에서 제약 위반으로 터지는데, 그러면 일시적 장애와 구분되지 않아
     * 5회 재시도를 낭비한다. 여기서 걸러 곧장 DLQ로 보낸다.
     */
    private void validate(MonthlyReportPayload payload) {
        if (payload.reportId() == null || payload.reportId().isBlank())
            throw new IllegalArgumentException("report_id 없음");

        if (payload.reportMonth() == null || payload.reportMonth().isBlank())
            throw new IllegalArgumentException("report_month 없음");

        if (payload.status() == null)
            throw new IllegalArgumentException("status 없음");
    }

    /**
     * 재시도가 무의미한 실패는 requeue 없이 곧장 DLX로 보낸다.
     * 브로커의 x-death 헤더는 rejected/expired만 구분하므로 사유는 로그로 남긴다.
     */
    private AmqpRejectAndDontRequeueException deadLetter(
            EventFailureReason reason, String eventId, String traceId, Exception cause
    ) {
        log.error("이벤트 DLQ 이동 - reason={}, detail={}, eventType={}, eventId={}, traceId={}",
                reason, reason.getDescription(), REPORT_GENERATED_EVENT_TYPE, eventId, traceId, cause);

        return new AmqpRejectAndDontRequeueException(
                "%s (eventId=%s)".formatted(reason, eventId), cause);
    }
}
