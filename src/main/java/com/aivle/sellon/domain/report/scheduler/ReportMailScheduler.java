package com.aivle.sellon.domain.report.scheduler;

import com.aivle.sellon.domain.report.repository.ReportMailDeliveryRepository;
import com.aivle.sellon.domain.report.service.ReportMailDeliveryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 예약 시각이 지난 월간 보고서 메일을 발송한다.
 * sendTime이 분 단위라 주기를 짧게 두되, 재시도 간격 자체는 예약 시각(다음 날)이 결정한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReportMailScheduler {

    private static final int BATCH_LIMIT = 100;

    private final ReportMailDeliveryRepository deliveryRepository;
    private final ReportMailDeliveryService deliveryService;

    @Scheduled(cron = "${report.mail.scheduler-cron}")
    public void sendDueMails() {
        List<Long> dueIds = deliveryRepository.findDueIds(LocalDateTime.now(), BATCH_LIMIT);
        if (dueIds.isEmpty())
            return;

        log.info("월간 보고서 메일 발송 시작 - 대상 {}건", dueIds.size());

        // 건별로 트랜잭션을 나눠, 한 건이 실패해도 나머지가 함께 롤백되지 않게 한다.
        // process 안에서 잡지 못하는 커밋 단계 예외까지 여기서 막아야 배치가 중단되지 않는다.
        for (Long deliveryId : dueIds) {
            try {
                deliveryService.process(deliveryId);
            } catch (Exception e) {
                log.error("월간 보고서 메일 처리 중 예외. deliveryId={}", deliveryId, e);
            }
        }
    }
}
