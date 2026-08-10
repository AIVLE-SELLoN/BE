package com.aivle.sellon.domain.report.dto.message;

import com.aivle.sellon.domain.report.enums.ReportStatus;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record MonthlyReportPayload(
        String reportId,
        String reportMonth,
        ReportStatus status,
        String noticeMessage,
        JsonNode validationReport,
        PdfS3MetaPayload pdfS3Meta
) {
}
