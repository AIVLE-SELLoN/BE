package com.aivle.sellon.domain.notification.entity;

import com.aivle.sellon.domain.company.entity.Company;
import com.aivle.sellon.domain.notification.enums.NotificationType;
import com.aivle.sellon.global.BaseEntity;
import jakarta.persistence.Column;
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
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(
        uniqueConstraints = @UniqueConstraint(
                name = "uk_notification_target",
                columnNames = {"type", "notification_target_id"}
        ),
        indexes = {
                @Index(name = "idx_notification_company_is_read", columnList = "company_id, is_read"),
                @Index(name = "idx_notification_notified_at", columnList = "notified_at")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, columnDefinition = "varchar(30)")
    private NotificationType type;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(nullable = false)
    private LocalDateTime notifiedAt;

    @Column(nullable = false)
    private boolean isRead;

    @Column(nullable = false)
    private Long notificationTargetId;

    private Notification(Company company, NotificationType type, String message, LocalDateTime notifiedAt,
                         Long notificationTargetId) {
        this.company = company;
        this.type = type;
        this.message = message;
        this.notifiedAt = notifiedAt;
        this.isRead = false;
        this.notificationTargetId = notificationTargetId;
    }

    public static Notification createForAlert(Company company, String message, LocalDateTime notifiedAt,
                                              Long detectionAlertId) {
        return new Notification(company, NotificationType.ANOMALY_DETECTED, message, notifiedAt, detectionAlertId);
    }

    public static Notification createForMonthlyReport(Company company, String message, LocalDateTime notifiedAt,
                                              Long reportId) {
        return new Notification(company, NotificationType.MONTHLY_REPORT_GENERATED, message, notifiedAt, reportId);
    }

    public void markAsRead() {
        if (isRead) {
            return;
        }
        this.isRead = true;
    }
}
