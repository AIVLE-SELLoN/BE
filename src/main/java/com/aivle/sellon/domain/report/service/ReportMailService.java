package com.aivle.sellon.domain.report.service;

import com.aivle.sellon.domain.report.event.ReportGeneratedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportMailService {

    private static final int MAX_ATTEMPTS = 3;

    private static final long RETRY_DELAY_MS = 1_000L;

    private final ReportMailDeliveryService deliveryService;

    /**
     * 커밋 이후에 예약을 만든다. 트랜잭션 안에서 처리하면 예약 실패가 리포트 저장까지 롤백시킨다.
     * <p>
     * 예외는 여기서 반드시 끊는다 — AFTER_COMMIT 콜백의 예외는 커밋 호출자(MQ 리스너)로 전파돼
     * 이미 저장이 확정된 메시지를 재시도하게 만든다. 다만 그냥 끊어버리면 메시지는 ack된 채
     * 예약 행이 하나도 남지 않아 메일이 조용히 유실되므로, 끊기 전에 몇 번 다시 시도한다.
     * 예약 생성은 멱등이라 재시도해도 중복 발송되지 않는다.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onReportGenerated(ReportGeneratedEvent event) {
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                deliveryService.scheduleFor(event.reportId(), event.companyId());
                return;
            } catch (DataIntegrityViolationException e) {
                // (report_id, email) 유니크 제약 위반 = 다른 컨슈머가 먼저 넣었다는 뜻.
                // 예약은 이미 존재하므로 재시도할 것도, 알릴 것도 없다.
                log.info("월간 보고서 메일 예약 생략 - 다른 처리에서 먼저 생성됨. reportId={}", event.reportId());
                return;
            } catch (Exception e) {
                if (attempt == MAX_ATTEMPTS) {
                    log.error("월간 보고서 메일 예약 실패 - {}회 시도 후 포기. reportId={}, companyId={}",
                            MAX_ATTEMPTS, event.reportId(), event.companyId(), e);
                    return;
                }

                log.warn("월간 보고서 메일 예약 실패 - 재시도 {}/{}. reportId={}",
                        attempt, MAX_ATTEMPTS, event.reportId(), e);

                if (!sleepBeforeRetry())
                    return;
            }
        }
    }

    /** @return 재시도를 이어가도 되면 true. 스레드가 인터럽트되면 중단한다 */
    private boolean sleepBeforeRetry() {
        try {
            Thread.sleep(RETRY_DELAY_MS);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
