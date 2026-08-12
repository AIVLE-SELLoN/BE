package com.aivle.sellon.domain.alert.dto.response;

import com.aivle.sellon.domain.notification.entity.Notification;
import com.aivle.sellon.domain.notification.enums.NotificationType;

import java.time.LocalDateTime;

public record AlertSummaryResponse(
        Long notificationId,
        NotificationType type,
        String message,
        LocalDateTime notifiedAt,
        boolean isRead,
        Long notificationTargetId
) {
    public static AlertSummaryResponse of(Notification notification) {
        return new AlertSummaryResponse(
                notification.getId(),
                notification.getType(),
                notification.getMessage(),
                notification.getNotifiedAt(),
                notification.isRead(),
                notification.getType() == NotificationType.ANOMALY_DETECTED
                        ? notification.getNotificationTargetId()
                        : null
        );
    }
}
