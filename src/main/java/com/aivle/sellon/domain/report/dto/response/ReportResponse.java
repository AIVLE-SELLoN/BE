package com.aivle.sellon.domain.report.dto.response;

import com.aivle.sellon.domain.report.entity.PdfS3Meta;
import com.aivle.sellon.domain.report.entity.Report;
import com.aivle.sellon.domain.report.enums.ReportStatus;

public record ReportResponse(
        Long id,
        String reportId,
        String productGroupId,
        String reportMonth,
        ReportStatus status,
        String noticeMessage,
        String originalFileName,
        String presignedUrl
) {
    public static ReportResponse of(Report report) {
        PdfS3Meta meta = report.getPdfS3Meta();

        return new ReportResponse(
                report.getId(),
                report.getReportId(),
                report.getProductGroupId(),
                report.getReportMonth(),
                report.getStatus(),
                report.getNoticeMessage(),
                meta != null ? meta.getOriginalFileName() : null,
                meta != null ? meta.getPresignedUrl() : null
        );
    }
}
