package com.aivle.sellon.domain.alert.listener;

import com.aivle.sellon.domain.alert.service.AlertResolveService;
import com.aivle.sellon.domain.proposal.event.ProposalAcceptedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 개선안 승인 → DetectionAlert.alertStatus RESOLVED 전환.
 * <p>
 * ReportNotificationListener와 동일한 AFTER_COMMIT + REQUIRES_NEW + try-catch 패턴.
 * 개선안 승인은 그 자체로 완결된 트랜잭션이어야 한다 — 상태 전환 실패로
 * 방금의 승인 처리(이력 저장, hitl_status 변경)까지 롤백되면 안 된다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AlertResolveListener {

    private final AlertResolveService alertResolveService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onProposalAccepted(ProposalAcceptedEvent event) {
        try {
            alertResolveService.resolve(event.companyId(), event.alertCode());
        } catch (Exception e) {
            log.error("알림 해결 처리 실패. companyId={}, alertCode={}",
                    event.companyId(), event.alertCode(), e);
        }
    }
}
