package com.aivle.sellon.domain.report.repository;

import com.aivle.sellon.domain.report.entity.QReportMailDelivery;
import com.aivle.sellon.domain.report.entity.ReportMailDelivery;
import com.aivle.sellon.domain.report.enums.ReportMailDeliveryStatus;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

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
    public List<String> findScheduledEmails(Long reportId) {
        return queryFactory
                .select(delivery.email)
                .from(delivery)
                .where(
                        delivery.report.id.eq(reportId),
                        delivery.deletedAt.isNull()
                )
                .fetch();
    }
}
