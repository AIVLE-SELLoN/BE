package com.aivle.sellon.domain.guideline.entity;

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

    // Guideline의 company FK(company_id)와 컬럼명이 겹쳐서 명시적으로 구분한다
    @Column(name = "pdf_company_id")
    private String companyId;

    @Column(name = "pdf_company_name")
    private String companyName;

    private String s3BucketName;
    private String s3FilePath;
    private String originalFileName;
    private String newFileName;
    private String s3FullKey;
    private Long fileSizeBytes;

    @Column(length = 2048)
    private String presignedUrl;

    // BaseEntity의 감사 컬럼(created_date)과 구분하기 위해 명시적으로 이름을 준다
    @Column(name = "pdf_created_at")
    private LocalDateTime createdAt;

    private LocalDateTime presignedExpiresAt;
    private LocalDateTime objectExpiresAt;

    private PdfS3Meta(String companyId, String companyName, String s3BucketName, String s3FilePath,
                       String originalFileName, String newFileName, String s3FullKey, Long fileSizeBytes,
                       String presignedUrl, LocalDateTime createdAt, LocalDateTime presignedExpiresAt,
                       LocalDateTime objectExpiresAt) {
        this.companyId = companyId;
        this.companyName = companyName;
        this.s3BucketName = s3BucketName;
        this.s3FilePath = s3FilePath;
        this.originalFileName = originalFileName;
        this.newFileName = newFileName;
        this.s3FullKey = s3FullKey;
        this.fileSizeBytes = fileSizeBytes;
        this.presignedUrl = presignedUrl;
        this.createdAt = createdAt;
        this.presignedExpiresAt = presignedExpiresAt;
        this.objectExpiresAt = objectExpiresAt;
    }

    public static PdfS3Meta of(String companyId, String companyName, String s3BucketName, String s3FilePath,
                                String originalFileName, String newFileName, String s3FullKey, Long fileSizeBytes,
                                String presignedUrl, LocalDateTime createdAt, LocalDateTime presignedExpiresAt,
                                LocalDateTime objectExpiresAt) {
        return new PdfS3Meta(companyId, companyName, s3BucketName, s3FilePath, originalFileName, newFileName,
                s3FullKey, fileSizeBytes, presignedUrl, createdAt, presignedExpiresAt, objectExpiresAt);
    }

    /**
     * 보관 기한이 지나 다시 만든 파일을 가리키는 메타를 새로 만든다.
     * 사용자에게 보이는 원본 파일명과 저장 위치는 그대로 두고, 실제 객체를 가리키는 값과 시각만 갱신한다.
     * presigned URL은 조회 시점마다 다시 발급하므로 굳이 채우지 않는다.
     */
    public PdfS3Meta withRegeneratedFile(String newFileName, String s3FullKey, long fileSizeBytes,
                                          LocalDateTime createdAt, LocalDateTime objectExpiresAt) {
        return new PdfS3Meta(companyId, companyName, s3BucketName, s3FilePath, originalFileName, newFileName,
                s3FullKey, fileSizeBytes, null, createdAt, null, objectExpiresAt);
    }
}
