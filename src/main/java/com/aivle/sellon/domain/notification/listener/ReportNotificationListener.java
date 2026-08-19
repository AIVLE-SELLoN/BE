package com.aivle.sellon.domain.notification.listener;

import com.aivle.sellon.domain.notification.service.NotificationService;
import com.aivle.sellon.domain.report.event.ReportGeneratedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 월간 리포트 완료 알림. ReportService는 무수정 — 이벤트 구독만으로 붙는다.
 * <p>
 * ReportMailService와 동일한 AFTER_COMMIT + REQUIRES_NEW + try-catch 패턴.
 * 재시도는 두지 않는다(메일과 달리 알림 유실은 치명도가 낮다고 판단해 log만 남기기로 함,
 * 2026-08-11 논의). 유실이 실제로 관측되면 재시도 → outbox로 격상 검토.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReportNotificationListener {

    private final NotificationService notificationService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onReportGenerated(ReportGeneratedEvent event) {
        try {
            notificationService.createForMonthlyReport(
                    event.companyId(), event.reportId(), event.reportMonth());
        } catch (DataIntegrityViolationException e) {
            // uk_notification_target 위반만 "정상적인 중복(재전송)"이다. 다른 제약 위반
            // (예: CHECK 제약, NOT NULL 등)까지 여기서 삼키면 진짜 실패가 조용히 묻힌다.
            if (isUniqueTargetViolation(e)) {
                log.info("월간 리포트 알림 생성 생략 - 이미 존재함(재전송). reportId={}", event.reportId());
            } else {
                log.error("월간 리포트 알림 생성 실패 - 무결성 제약 위반(중복 아님). reportId={}, companyId={}",
                        event.reportId(), event.companyId(), e);
            }
        } catch (Exception e) {
            log.error("월간 리포트 알림 생성 실패. reportId={}, companyId={}",
                    event.reportId(), event.companyId(), e);
        }
    }

    private boolean isUniqueTargetViolation(DataIntegrityViolationException e) {
        String message = e.getMostSpecificCause().getMessage();
        return message != null && message.contains("uk_notification_target");
    }
}
