package com.aivle.sellon.domain.report.dto.message;

import java.util.List;

public record MonthlyReportGeneratedMessage(
        Long companyId,
        List<ReportFileInfo> files
) {
}
