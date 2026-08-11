package com.aivle.sellon.domain.notification.enums;

import com.aivle.sellon.domain.alert.entity.DetectionAlert;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NotificationType {
    ANOMALY_DETECTED(DetectionAlert.class);

    private final Class<?> targetClass;
}

