package com.aivle.sellon.domain.report.dto.response;

import com.aivle.sellon.domain.report.entity.Report;

import java.time.LocalDateTime;

public record ReportResponse(
        Long id,
        String originalFileName,
        Long fileSize,
        LocalDateTime generatedAt
) {
    public static ReportResponse of(Report report) {
        return new ReportResponse(report.getId(), report.getOriginalFileName(), report.getFileSize(), report.getGeneratedAt());
    }
}
