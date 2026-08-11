package com.aivle.sellon.domain.alert.entity;

import com.aivle.sellon.domain.alert.enums.AlertChannel;
import com.aivle.sellon.domain.alert.enums.AlertStatus;
import com.aivle.sellon.domain.alert.enums.Aspect;
import com.aivle.sellon.domain.alert.enums.DetectionConfidence;
import com.aivle.sellon.domain.alert.enums.RecommendedAction;
import com.aivle.sellon.domain.alert.enums.Verdict;
import com.aivle.sellon.domain.company.entity.Company;
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
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@Table(
        indexes = {
                @Index(name = "idx_detection_alert_company_status", columnList = "company_id, alert_status"),
                @Index(name = "idx_detection_alert_detected_at", columnList = "detected_at"),
                @Index(name = "idx_detection_alert_group_channel_aspect",
                        columnList = "product_group_id, channel, main_aspect")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DetectionAlert extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 32)
    private String alertCode;

    @Column(nullable = false)
    private LocalDateTime detectedAt;

    @Column(length = 32)
    private String updatesAlertCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    // raw DB의 논리 식별자를 그대로 보존해야 하므로 JPA 외래키로 강제하지 않는다.
    @Column(nullable = false, length = 32)
    private String productGroupId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AlertChannel channel;

    @Column(nullable = false)
    private LocalDate windowStart;

    @Column(nullable = false)
    private LocalDate windowEnd;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Verdict verdict;

    @Column(columnDefinition = "TEXT")
    private String significantChannels;

    @Column(columnDefinition = "TEXT")
    private String excludedChannels;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Aspect mainAspect;

    @Column(columnDefinition = "TEXT")
    private String subAspects;

    @Embedded
    private AlertStats stats;

    @Column(columnDefinition = "TEXT")
    private String sourceSignals;

    @Embedded
    private AlertRootCause rootCause;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DetectionConfidence detectionConfidence;

    @Column(nullable = false)
    private boolean scopeIn;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RecommendedAction recommendedAction;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String evidenceInquiryIds;

    @Column(length = 32)
    private String linkedChangeId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AlertStatus alertStatus;

    @Column(columnDefinition = "TEXT")
    private String channelBreakdownSnapshot;

    private DetectionAlert(String alertCode, Company company) {
        this.alertCode = alertCode;
        this.company = company;
        this.alertStatus = AlertStatus.UNRESOLVED;
    }

    public static DetectionAlert create(String alertCode, LocalDateTime detectedAt, String updatesAlertCode,
                                        Company company, String productGroupId, AlertChannel channel,
                                        LocalDate windowStart, LocalDate windowEnd, Verdict verdict,
                                        String significantChannels, String excludedChannels, Aspect mainAspect,
                                        String subAspects, AlertStats stats, String sourceSignals,
                                        AlertRootCause rootCause, DetectionConfidence detectionConfidence,
                                        boolean scopeIn, RecommendedAction recommendedAction,
                                        String evidenceInquiryIds, String linkedChangeId,
                                        String channelBreakdownSnapshot) {
        DetectionAlert detectionAlert = new DetectionAlert(alertCode, company);
        detectionAlert.update(detectedAt, updatesAlertCode, productGroupId, channel, windowStart, windowEnd,
                verdict, significantChannels, excludedChannels, mainAspect, subAspects, stats, sourceSignals,
                rootCause, detectionConfidence, scopeIn, recommendedAction, evidenceInquiryIds, linkedChangeId,
                channelBreakdownSnapshot);
        return detectionAlert;
    }

    public void update(LocalDateTime detectedAt, String updatesAlertCode, String productGroupId,
                       AlertChannel channel, LocalDate windowStart, LocalDate windowEnd, Verdict verdict,
                       String significantChannels, String excludedChannels, Aspect mainAspect, String subAspects,
                       AlertStats stats, String sourceSignals, AlertRootCause rootCause,
                       DetectionConfidence detectionConfidence, boolean scopeIn, RecommendedAction recommendedAction,
                       String evidenceInquiryIds, String linkedChangeId,
                       String channelBreakdownSnapshot) {
        this.detectedAt = detectedAt;
        this.updatesAlertCode = updatesAlertCode;
        this.productGroupId = productGroupId;
        this.channel = channel;
        this.windowStart = windowStart;
        this.windowEnd = windowEnd;
        this.verdict = verdict;
        this.significantChannels = significantChannels;
        this.excludedChannels = excludedChannels;
        this.mainAspect = mainAspect;
        this.subAspects = subAspects;
        this.stats = stats;
        this.sourceSignals = sourceSignals;
        this.rootCause = rootCause;
        this.detectionConfidence = detectionConfidence;
        this.scopeIn = scopeIn;
        this.recommendedAction = recommendedAction;
        this.evidenceInquiryIds = evidenceInquiryIds;
        this.linkedChangeId = linkedChangeId;
        this.channelBreakdownSnapshot = channelBreakdownSnapshot;
    }
}
