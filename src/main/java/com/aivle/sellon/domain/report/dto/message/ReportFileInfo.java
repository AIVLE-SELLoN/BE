package com.aivle.sellon.domain.report.dto.message;

import java.time.LocalDateTime;

public record ReportFileInfo(
        String originalFileName,
        String storedFileName,
        Long fileSize,
        LocalDateTime generatedAt
) {
}
