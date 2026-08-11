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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Entity
@Getter
@Table(uniqueConstraints = @UniqueConstraint(
        name = "uk_report_company_report_id",
        columnNames = {"company_id", "report_id"}
))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Report extends BaseEntity {

    // 큐로 오는 S3 메타 시각은 Instant(UTC)라, BaseEntity 감사 컬럼과 같은 KST 벽시계로 맞춰 저장한다
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    // 월 1건 합본이라 reportId 형식은 RPT-{YYYYMM}. 회사 구분자가 없어 단독으로는 유일하지 않다
    @Column(nullable = false)
    private String reportId;

    @Column(nullable = false)
    private String reportMonth;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportStatus status;

    @Column(length = 1000)
    private String noticeMessage;

    @Column(columnDefinition = "TEXT")
    private String validationReport;

    @Embedded
    private PdfS3Meta pdfS3Meta;

    private Report(Company company, String reportId, String reportMonth) {
        this.company = company;
        this.reportId = reportId;
        this.reportMonth = reportMonth;
    }

    public static Report create(MonthlyReportPayload payload, Company company) {
        Report report = new Report(company, payload.reportId(), payload.reportMonth());
        report.update(payload);
        return report;
    }

    public void update(MonthlyReportPayload payload) {
        this.status = payload.status();
        this.noticeMessage = payload.noticeMessage();
        this.validationReport = payload.validationReport() != null ? payload.validationReport().toString() : null;

        // FAILED_* 페이로드는 pdf_s3_meta가 비어 있다.
        // 그대로 대입하면 앞서 SUCCESS로 만들어둔 PDF 참조가 지워지는데, S3의 파일은 그대로 살아있고 발송을 기다리던 메일 예약도 그 PDF를 가리키고 있다.
        // 실제로 새 PDF가 왔을 때만 교체한다.
        PdfS3Meta incoming = toPdfS3Meta(payload.pdfS3Meta());
        if (incoming != null)
            this.pdfS3Meta = incoming;
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
                payload.fileSizeBytes(),
                payload.presignedUrl(),
                toLocalDateTime(payload.createdAt()),
                toLocalDateTime(payload.presignedExpiresAt()),
                toLocalDateTime(payload.objectExpiresAt())
        );
    }

    private static LocalDateTime toLocalDateTime(Instant instant) {
        return instant != null ? LocalDateTime.ofInstant(instant, KST) : null;
    }
}
