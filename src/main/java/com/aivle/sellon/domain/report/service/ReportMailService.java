package com.aivle.sellon.domain.report.service;

import com.aivle.sellon.domain.report.event.ReportGeneratedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportMailService {

    private final ReportMailDeliveryService deliveryService;

    /**
     * 커밋 이후에 예약을 만든다. 트랜잭션 안에서 처리하면 예약 실패가 리포트 저장까지 롤백시킨다.
     * <p>
     * 예외는 여기서 반드시 끊는다 — AFTER_COMMIT 콜백의 예외는 커밋 호출자(MQ 리스너)로 전파돼
     * 이미 저장이 확정된 메시지를 재시도하게 만든다.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onReportGenerated(ReportGeneratedEvent event) {
        try {
            deliveryService.scheduleFor(event.reportId(), event.companyId());
        } catch (Exception e) {
            log.error("월간 보고서 메일 예약 실패. reportId={}, companyId={}",
                    event.reportId(), event.companyId(), e);
        }
    }
}
