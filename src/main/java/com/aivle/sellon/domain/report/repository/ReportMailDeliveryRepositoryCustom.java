package com.aivle.sellon.domain.report.repository;

import com.aivle.sellon.domain.report.entity.ReportMailDelivery;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ReportMailDeliveryRepositoryCustom {

    /**
     * 발송 시각이 도래했고 아직 시도 횟수가 남은 예약의 ID 목록.
     * 건별로 트랜잭션을 나눠 처리하기 위해 엔티티가 아니라 ID만 가져온다.

     * 여기서는 선점하지 않는다. 인스턴스가 여러 대면 같은 ID가 중복으로 잡히지만,
     * 실제 발송 직전에 {@link #findPendingForUpdate}가 행 락으로 한 대만 통과시킨다.
     */
    List<Long> findDueIds(LocalDateTime now, int limit);

    /**
     * 발송 대상 예약을 행 락과 함께 가져온다. 다른 인스턴스가 이미 잡고 있으면(SKIP LOCKED)
     * 비어서 돌아오므로, 같은 예약이 인스턴스 수만큼 중복 발송되지 않는다.

     * @return PENDING이면서 락을 획득한 경우에만 값이 있다
     */
    Optional<ReportMailDelivery> findPendingForUpdate(Long deliveryId);

    /**
     * 해당 리포트의 모든 예약. 큐 재전달 시 이미 예약된 수신자를 가려내는 데 쓴다.
     * <p>
     * deleted_at을 거르지 않는 이유: 중복 판정 기준이 유니크 제약 {@code (report_id, email)}과
     * 같아야 한다. 소프트 삭제된 행도 제약에는 그대로 걸리므로, 걸러내면 INSERT가 터진다.
     */
    List<ReportMailDelivery> findAllByReportId(Long reportId);
}
