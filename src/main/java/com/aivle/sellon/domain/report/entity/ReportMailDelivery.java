package com.aivle.sellon.domain.report.entity;

import com.aivle.sellon.domain.report.enums.ReportMailDeliveryStatus;
import com.aivle.sellon.global.BaseEntity;
import jakarta.persistence.Column;
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

import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * 월간 리포트 완료 메일의 발송 예약 1건(수신자 1명 기준).
 * 큐 수신 즉시 보내지 않고 행으로 남겨, 실패해도 다음 날 재시도할 수 있게 한다.
 */
@Entity
@Getter
@Table(uniqueConstraints = @UniqueConstraint(
        name = "uk_report_mail_delivery_report_email",
        columnNames = {"report_id", "email"}
))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReportMailDelivery extends BaseEntity {

    public static final int MAX_ATTEMPTS = 5;

    private static final int MAX_ERROR_LENGTH = 500;

    // scheduledAt이 KST 벽시계 기준(ReportMailDeliveryService)이라, 재예약도 같은 기준으로 맞춘다.
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_id", nullable = false)
    private Report report;

    @Column(nullable = false, length = 255)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportMailDeliveryStatus status;

    @Column(nullable = false)
    private LocalDateTime scheduledAt;

    @Column(nullable = false)
    private int attemptCount;

    @Column(length = MAX_ERROR_LENGTH)
    private String lastError;

    private LocalDateTime sentAt;

    private ReportMailDelivery(Report report, String email, LocalDateTime scheduledAt) {
        this.report = report;
        this.email = email;
        this.scheduledAt = scheduledAt;
        this.status = ReportMailDeliveryStatus.PENDING;
        this.attemptCount = 0;
    }

    public static ReportMailDelivery schedule(Report report, String email, LocalDateTime scheduledAt) {
        return new ReportMailDelivery(report, email, scheduledAt);
    }

    public void markSent() {
        this.attemptCount++;
        this.status = ReportMailDeliveryStatus.SENT;
        this.sentAt = LocalDateTime.now(KST);
        this.lastError = null;
    }

    /**
     * 실패를 기록하고 다음 날 같은 시각으로 재예약한다. {@link #MAX_ATTEMPTS}회를 채우면 포기한다.
     */
    public void markFailed(String error) {
        this.attemptCount++;
        this.lastError = truncate(error);

        if (this.attemptCount >= MAX_ATTEMPTS)
            this.status = ReportMailDeliveryStatus.GIVEN_UP;
        else
            this.scheduledAt = LocalDateTime.now(KST).plusDays(1);
    }

    /**
     * 포기했던 예약을 같은 행에서 다시 살린다.

     * 새 행을 만들지 않는 이유는 유니크 제약 (report_id, email) 때문이고,
     * 시도 횟수를 0으로 되돌리는 이유는 MAX_ATTEMPTS 를 이미 채운 행이면
     * 되살려도 발송 대상 조회에 걸리지 않기 때문이다.
     */
    public void reschedule(LocalDateTime scheduledAt) {
        this.status = ReportMailDeliveryStatus.PENDING;
        this.scheduledAt = scheduledAt;
        this.attemptCount = 0;
        this.lastError = null;
    }

    /**
     * 재시도해도 결과가 같은 실패(PDF가 이미 삭제된 경우 등)는 시도 1회로 기록하고 즉시 포기한다.
     */
    public void giveUp(String reason) {
        this.attemptCount++;
        this.status = ReportMailDeliveryStatus.GIVEN_UP;
        this.lastError = truncate(reason);
    }

    private static String truncate(String value) {
        if (value == null)
            return null;

        return value.length() <= MAX_ERROR_LENGTH ? value : value.substring(0, MAX_ERROR_LENGTH);
    }
}
