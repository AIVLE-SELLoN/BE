package com.aivle.sellon.domain.report.dto.message;

import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

import java.time.Instant;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record PdfS3MetaPayload(
        String s3BucketName,
        String s3FilePath,
        String originalFileName,
        String newFileName,
        String s3FullKey,
        String fileExtension,
        Long fileSizeBytes,
        String presignedUrl,
        Instant presignedExpiresAt,
        Instant objectExpiresAt
) {
}
