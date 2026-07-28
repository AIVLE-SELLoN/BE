package com.aivle.sellon.domain.report.dto.response;

import com.aivle.sellon.domain.report.entity.Report;

public record ReportResponse(
        Long id,
        String title,
        String fileUrl
) {
    public static ReportResponse of(Report report) {
        return new ReportResponse(report.getId(), report.getTitle(), report.getFileUrl());
    }
}
