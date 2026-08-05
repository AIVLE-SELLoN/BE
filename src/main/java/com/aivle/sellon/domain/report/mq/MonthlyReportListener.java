package com.aivle.sellon.domain.report.mq;

import com.aivle.sellon.domain.report.dto.message.MonthlyReportPayload;
import com.aivle.sellon.domain.report.service.ReportService;
import com.aivle.sellon.global.mq.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Component
@RequiredArgsConstructor
public class MonthlyReportListener {

    private static final String REPORT_GENERATED_EVENT_TYPE = "ai.report.generated";

    private final ReportService reportService;
    private final JsonMapper jsonMapper;

    @RabbitListener(queues = RabbitMQConfig.MAIN_INBOUND_QUEUE)
    public void onAiEvent(JsonNode envelope) {
        String eventType = envelope.path("eventType").asString(null);
        if (!REPORT_GENERATED_EVENT_TYPE.equals(eventType))
            return;

        MonthlyReportPayload payload = jsonMapper.treeToValue(envelope.get("payload"), MonthlyReportPayload.class);
        reportService.saveGeneratedReport(payload);
    }
}
