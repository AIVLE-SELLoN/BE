package com.aivle.sellon.domain.notification.repository;

import com.aivle.sellon.domain.notification.entity.Notification;
import com.aivle.sellon.domain.notification.entity.QNotification;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@RequiredArgsConstructor
public class NotificationRepositoryImpl implements NotificationRepositoryCustom {

    private static final QNotification notification = QNotification.notification;

    private final JPAQueryFactory queryFactory;

    @Override
    public List<Notification> findAllByCompanyId(Long companyId, Long cursorId, boolean unreadOnly, int limit) {
        BooleanBuilder conditions = new BooleanBuilder(notification.company.id.eq(companyId));

        if (unreadOnly) {
            conditions.and(notification.isRead.isFalse());
        }

        if (cursorId != null) {
            LocalDateTime cursorNotifiedAt = findNotifiedAtByIdAndCompanyId(cursorId, companyId);
            if (cursorNotifiedAt == null) {
                return List.of();
            }
            conditions.and(isAfterCursor(cursorNotifiedAt, cursorId));
        }

        return queryFactory
                .selectFrom(notification)
                .where(conditions)
                .orderBy(notification.notifiedAt.desc(), notification.id.desc())
                .limit(limit)
                .fetch();
    }

    @Override
    public long countByCompanyId(Long companyId) {
        Long count = queryFactory
                .select(notification.count())
                .from(notification)
                .where(notification.company.id.eq(companyId))
                .fetchOne();
        return count != null ? count : 0;
    }

    @Override
    public long countUnreadByCompanyId(Long companyId) {
        Long count = queryFactory
                .select(notification.count())
                .from(notification)
                .where(
                        notification.company.id.eq(companyId),
                        notification.isRead.isFalse()
                )
                .fetchOne();
        return count != null ? count : 0;
    }

    private LocalDateTime findNotifiedAtByIdAndCompanyId(Long notificationId, Long companyId) {
        return queryFactory
                .select(notification.notifiedAt)
                .from(notification)
                .where(
                        notification.id.eq(notificationId),
                        notification.company.id.eq(companyId)
                )
                .fetchOne();
    }

    private BooleanExpression isAfterCursor(LocalDateTime cursorNotifiedAt, Long cursorId) {
        return notification.notifiedAt.lt(cursorNotifiedAt)
                .or(notification.notifiedAt.eq(cursorNotifiedAt).and(notification.id.lt(cursorId)));
    }
}
