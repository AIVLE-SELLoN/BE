package com.aivle.sellon.domain.report.mq;

import com.aivle.sellon.domain.report.dto.message.MonthlyReportGeneratedMessage;
import com.aivle.sellon.domain.report.entity.Report;
import com.aivle.sellon.domain.report.service.ReportMailService;
import com.aivle.sellon.domain.report.service.ReportService;
import com.aivle.sellon.global.mq.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class MonthlyReportListener {

    private final ReportService reportService;
    private final ReportMailService reportMailService;

    @RabbitListener(queues = RabbitMQConfig.MONTHLY_REPORT_QUEUE)
    public void onMonthlyReportGenerated(MonthlyReportGeneratedMessage message) {
        List<Report> savedReports = reportService.saveGeneratedReports(message);
        reportMailService.sendCompletionMail(savedReports);
    }
}
