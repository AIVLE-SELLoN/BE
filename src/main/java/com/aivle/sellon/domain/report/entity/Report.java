package com.aivle.sellon.domain.report.entity;

import com.aivle.sellon.domain.company.entity.Company;
import com.aivle.sellon.domain.report.dto.message.MonthlyReportPayload;
import com.aivle.sellon.domain.report.dto.message.PdfS3MetaPayload;
import com.aivle.sellon.domain.report.enums.ReportStatus;
import com.aivle.sellon.global.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Report extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // product_group_id -> company 매핑이 아직 없어 항상 null (도메인 추가 전까지 보류)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company company;

    @Column(nullable = false, unique = true)
    private String reportId;

    @Column(nullable = false)
    private String productGroupId;

    @Column(nullable = false)
    private String reportMonth;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportStatus status;

    private String noticeMessage;

    @Column(columnDefinition = "TEXT")
    private String validationReport;

    @Embedded
    private PdfS3Meta pdfS3Meta;

    private Report(String reportId, String productGroupId, String reportMonth) {
        this.reportId = reportId;
        this.productGroupId = productGroupId;
        this.reportMonth = reportMonth;
    }

    public static Report create(MonthlyReportPayload payload) {
        Report report = new Report(payload.reportId(), payload.productGroupId(), payload.reportMonth());
        report.update(payload);
        return report;
    }

    public void update(MonthlyReportPayload payload) {
        this.status = payload.status();
        this.noticeMessage = payload.noticeMessage();
        this.validationReport = payload.validationReport() != null ? payload.validationReport().toString() : null;
        this.pdfS3Meta = toPdfS3Meta(payload.pdfS3Meta());
    }

    private static PdfS3Meta toPdfS3Meta(PdfS3MetaPayload payload) {
        if (payload == null)
            return null;

        return PdfS3Meta.of(
                payload.s3BucketName(),
                payload.s3FilePath(),
                payload.originalFileName(),
                payload.newFileName(),
                payload.s3FullKey(),
                payload.fileExtension(),
                payload.fileSizeBytes(),
                payload.presignedUrl(),
                toLocalDateTime(payload.presignedExpiresAt()),
                toLocalDateTime(payload.objectExpiresAt())
        );
    }

    private static LocalDateTime toLocalDateTime(Instant instant) {
        return instant != null ? LocalDateTime.ofInstant(instant, ZoneOffset.UTC) : null;
    }
}
