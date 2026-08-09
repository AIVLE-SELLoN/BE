package com.aivle.sellon.domain.report.repository;

import com.aivle.sellon.domain.report.entity.QReportMailDelivery;
import com.aivle.sellon.domain.report.entity.ReportMailDelivery;
import com.aivle.sellon.domain.report.enums.ReportMailDeliveryStatus;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import org.hibernate.Timeouts;
import org.hibernate.jpa.AvailableHints;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public class ReportMailDeliveryRepositoryImpl implements ReportMailDeliveryRepositoryCustom {

    private static final QReportMailDelivery delivery = QReportMailDelivery.reportMailDelivery;

    private final JPAQueryFactory queryFactory;

    @Override
    public List<Long> findDueIds(LocalDateTime now, int limit) {
        return queryFactory
                .select(delivery.id)
                .from(delivery)
                .where(
                        delivery.status.eq(ReportMailDeliveryStatus.PENDING),
                        delivery.scheduledAt.loe(now),
                        delivery.attemptCount.lt(ReportMailDelivery.MAX_ATTEMPTS),
                        delivery.deletedAt.isNull()
                )
                .orderBy(delivery.scheduledAt.asc())
                .limit(limit)
                .fetch();
    }

    @Override
    public Optional<ReportMailDelivery> findPendingForUpdate(Long deliveryId) {
        // status 조건을 락 쿼리에 함께 두는 게 중요하다. SKIP LOCKED가 적용되지 않는 환경이면
        // 락을 기다렸다가 잡게 되는데, 그때 재평가되는 조건이 없으면 앞선 인스턴스가
        // 이미 SENT로 만든 행을 그대로 한 번 더 발송하게 된다.
        ReportMailDelivery locked = queryFactory
                .selectFrom(delivery)
                .where(
                        delivery.id.eq(deliveryId),
                        delivery.status.eq(ReportMailDeliveryStatus.PENDING),
                        delivery.attemptCount.lt(ReportMailDelivery.MAX_ATTEMPTS),
                        delivery.deletedAt.isNull()
                )
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .setHint(AvailableHints.HINT_SPEC_LOCK_TIMEOUT, Timeouts.SKIP_LOCKED_MILLI)
                .fetchFirst();

        return Optional.ofNullable(locked);
    }

    @Override
    public List<ReportMailDelivery> findAllByReportId(Long reportId) {
        return queryFactory
                .selectFrom(delivery)
                .where(delivery.report.id.eq(reportId))
                .fetch();
    }
}
