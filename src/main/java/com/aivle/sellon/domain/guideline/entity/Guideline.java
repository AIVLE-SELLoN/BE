package com.aivle.sellon.domain.guideline.entity;

import com.aivle.sellon.domain.company.entity.Company;
import com.aivle.sellon.domain.guideline.dto.message.GuidelinePayload;
import com.aivle.sellon.domain.guideline.dto.message.PdfS3MetaPayload;
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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Entity
@Getter
@Table(uniqueConstraints = @UniqueConstraint(
        name = "uk_guideline_company_guideline_id",
        columnNames = {"company_id", "guideline_id"}
))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Guideline extends BaseEntity {

    // 큐로 오는 S3 메타 시각은 Instant(UTC)라, BaseEntity 감사 컬럼과 같은 KST 벽시계로 맞춰 저장한다
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    // alert_id의 ALT- 접두어를 GD-로 바꾼 값. 알림과 1:1이라 재생성해도 같은 ID (노션 §4.6, 2026-08-10)
    @Column(nullable = false)
    private String guidelineId;

    @Column(nullable = false)
    private String alertId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportStatus status;

    @Column(length = 1000)
    private String noticeMessage;

    @Column(columnDefinition = "TEXT")
    private String validationReport;

    // 입력+출력 JSON 원본. PDF가 7일 뒤 사라진 다음에도 이 원본으로 재컴파일한다
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private String sourcePayload;

    @Embedded
    private PdfS3Meta pdfS3Meta;

    private Guideline(Company company, String guidelineId, String alertId) {
        this.company = company;
        this.guidelineId = guidelineId;
        this.alertId = alertId;
    }

    public static Guideline create(GuidelinePayload payload, Company company) {
        Guideline guideline = new Guideline(company, payload.guidelineId(), payload.alertId());
        guideline.update(payload);
        return guideline;
    }

    public void update(GuidelinePayload payload) {
        this.status = payload.status();
        this.noticeMessage = payload.noticeMessage();
        this.validationReport = payload.validationReport() != null ? payload.validationReport().toString() : null;
        this.sourcePayload = payload.sourcePayload().toString();

        // FAILED_* 페이로드는 pdf_s3_meta가 비어 있다. 재전달로 다시 update가 불려도
        // 이미 저장된 SUCCESS의 PDF 참조를 지우지 않도록, 새 값이 있을 때만 교체한다.
        PdfS3Meta incoming = toPdfS3Meta(payload.pdfS3Meta());
        if (incoming != null)
            this.pdfS3Meta = incoming;
    }

    private static PdfS3Meta toPdfS3Meta(PdfS3MetaPayload payload) {
        if (payload == null)
            return null;

        return PdfS3Meta.of(
                payload.companyId(),
                payload.companyName(),
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
