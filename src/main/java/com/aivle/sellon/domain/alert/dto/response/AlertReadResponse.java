package com.aivle.sellon.domain.alert.dto.response;

import com.aivle.sellon.domain.notification.entity.Notification;

public record AlertReadResponse(
        Long notificationId,
        boolean isRead,
        long unreadCount
) {
    public static AlertReadResponse of(Notification notification, long unreadCount) {
        return new AlertReadResponse(
                notification.getId(),
                notification.isRead(),
                unreadCount
        );
    }
}
