package com.aivle.sellon.domain.guideline.entity;

import com.aivle.sellon.global.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * source_payload 전문(JSON) 안에서 자주 조회할 필드만 뽑아 별도 테이블에 색인처럼 보관한다.
 * 원본은 {@link Guideline#getSourcePayload()}에 그대로 남아있으니, 여기 값은 언제든
 * 원본에서 다시 뽑아 재구성할 수 있는 파생 데이터다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GuidelineSummary extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guideline_id", nullable = false, unique = true)
    private Guideline guideline;

    private LocalDateTime detectedAt;

    private String productGroupId;

    private String productName;

    // "[{alert_id}] | {channel} - {root_cause.label}" 형식의 화면 표시용 제목
    @Column(length = 500)
    private String title;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String linkedInquiries;

    private GuidelineSummary(Guideline guideline, LocalDateTime detectedAt, String productGroupId,
                              String productName, String title, String linkedInquiries) {
        this.guideline = guideline;
        this.detectedAt = detectedAt;
        this.productGroupId = productGroupId;
        this.productName = productName;
        this.title = title;
        this.linkedInquiries = linkedInquiries;
    }

    public static GuidelineSummary create(Guideline guideline, LocalDateTime detectedAt, String productGroupId,
                                           String productName, String title, String linkedInquiries) {
        return new GuidelineSummary(guideline, detectedAt, productGroupId, productName, title, linkedInquiries);
    }

    public void update(LocalDateTime detectedAt, String productGroupId, String productName,
                        String title, String linkedInquiries) {
        this.detectedAt = detectedAt;
        this.productGroupId = productGroupId;
        this.productName = productName;
        this.title = title;
        this.linkedInquiries = linkedInquiries;
    }
}
