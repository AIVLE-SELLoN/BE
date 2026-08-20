package com.aivle.sellon.domain.notification.repository;

import com.aivle.sellon.domain.notification.entity.Notification;

import java.util.List;

public interface NotificationRepositoryCustom {
    List<Notification> findAllByCompanyId(Long companyId, Long cursorId, boolean unreadOnly, int limit);

    long countByCompanyId(Long companyId);

    long countUnreadByCompanyId(Long companyId);
}
