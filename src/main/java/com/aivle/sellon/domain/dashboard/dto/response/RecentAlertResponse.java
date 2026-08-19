package com.aivle.sellon.domain.dashboard.dto.response;

import com.aivle.sellon.domain.alert.enums.AlertChannel;
import com.aivle.sellon.domain.alert.enums.AlertStatus;
import com.aivle.sellon.domain.alert.enums.Aspect;

import java.time.LocalDateTime;

public record RecentAlertResponse(
        String alertCode,
        String productGroupId,
        String productName,
        AlertChannel channel,
        String channelName,
        Aspect mainAspect,
        AlertStatus alertStatus,
        LocalDateTime detectedAt
) {
    public static RecentAlertResponse of(String alertCode, String productGroupId, String productName,
                                         AlertChannel channel, String channelName,
                                         Aspect mainAspect, AlertStatus alertStatus, LocalDateTime detectedAt) {
        return new RecentAlertResponse(alertCode, productGroupId, productName, channel, channelName,
                mainAspect, alertStatus, detectedAt);
    }
}
