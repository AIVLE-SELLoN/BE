package com.aivle.sellon.domain.notification.enums;

import com.aivle.sellon.domain.alert.entity.DetectionAlert;
import com.aivle.sellon.domain.report.entity.Report;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NotificationType {
    ANOMALY_DETECTED(DetectionAlert.class),
    MONTHLY_REPORT_GENERATED(Report.class);

    private final Class<?> targetClass;
}
