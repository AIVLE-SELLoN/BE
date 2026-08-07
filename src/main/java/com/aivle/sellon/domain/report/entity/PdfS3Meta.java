package com.aivle.sellon.domain.report.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PdfS3Meta {

    private String s3BucketName;
    private String s3FilePath;
    private String originalFileName;
    private String newFileName;
    private String s3FullKey;
    private String fileExtension;
    private Long fileSizeBytes;

    @Column(length = 2048)
    private String presignedUrl;

    // BaseEntity의 감사 컬럼(created_date)과 구분하기 위해 명시적으로 이름을 준다
    @Column(name = "pdf_created_at")
    private LocalDateTime createdAt;

    private LocalDateTime presignedExpiresAt;
    private LocalDateTime objectExpiresAt;

    private PdfS3Meta(String s3BucketName, String s3FilePath, String originalFileName, String newFileName,
                       String s3FullKey, String fileExtension, Long fileSizeBytes, String presignedUrl,
                       LocalDateTime createdAt, LocalDateTime presignedExpiresAt, LocalDateTime objectExpiresAt) {
        this.s3BucketName = s3BucketName;
        this.s3FilePath = s3FilePath;
        this.originalFileName = originalFileName;
        this.newFileName = newFileName;
        this.s3FullKey = s3FullKey;
        this.fileExtension = fileExtension;
        this.fileSizeBytes = fileSizeBytes;
        this.presignedUrl = presignedUrl;
        this.createdAt = createdAt;
        this.presignedExpiresAt = presignedExpiresAt;
        this.objectExpiresAt = objectExpiresAt;
    }

    public static PdfS3Meta of(String s3BucketName, String s3FilePath, String originalFileName, String newFileName,
                                String s3FullKey, String fileExtension, Long fileSizeBytes, String presignedUrl,
                                LocalDateTime createdAt, LocalDateTime presignedExpiresAt,
                                LocalDateTime objectExpiresAt) {
        return new PdfS3Meta(s3BucketName, s3FilePath, originalFileName, newFileName, s3FullKey, fileExtension,
                fileSizeBytes, presignedUrl, createdAt, presignedExpiresAt, objectExpiresAt);
    }
}
