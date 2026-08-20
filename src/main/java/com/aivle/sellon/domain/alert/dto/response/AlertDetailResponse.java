package com.aivle.sellon.domain.alert.dto.response;

import com.aivle.sellon.domain.alert.entity.AlertRootCause;
import com.aivle.sellon.domain.alert.entity.AlertStats;
import com.aivle.sellon.domain.alert.entity.DetectionAlert;
import com.aivle.sellon.domain.alert.enums.AlertChannel;
import com.aivle.sellon.domain.alert.enums.AlertStatus;
import com.aivle.sellon.domain.alert.enums.Aspect;
import com.aivle.sellon.domain.alert.enums.DetectionConfidence;
import com.aivle.sellon.domain.alert.enums.RecommendedAction;
import com.aivle.sellon.domain.alert.enums.StatsSource;
import com.aivle.sellon.domain.alert.enums.Verdict;
import com.aivle.sellon.domain.notification.entity.Notification;
import com.aivle.sellon.domain.notification.enums.NotificationType;
import com.fasterxml.jackson.annotation.JsonAlias;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record AlertDetailResponse(
        Long notificationId,
        NotificationType type,
        String message,
        LocalDateTime notifiedAt,
        boolean isRead,
        Long notificationTargetId,
        AlertResponse alert
) {
    public static AlertDetailResponse of(Notification notification, AlertResponse alert) {
        return new AlertDetailResponse(
                notification.getId(),
                notification.getType(),
                notification.getMessage(),
                notification.getNotifiedAt(),
                notification.isRead(),
                notification.getType() == NotificationType.ANOMALY_DETECTED
                        ? notification.getNotificationTargetId()
                        : null,
                alert
        );
    }

    public record AlertResponse(
            String alertCode,
            String updatesAlertCode,
            LocalDateTime detectedAt,
            String productGroupId,
            AlertChannel channel,
            Aspect mainAspect,
            List<SubAspectResponse> subAspects,
            LocalDate windowStart,
            LocalDate windowEnd,
            Verdict verdict,
            DetectionConfidence detectionConfidence,
            RecommendedAction recommendedAction,
            AlertStatus alertStatus,
            boolean scopeIn,
            StatsResponse stats,
            RootCauseResponse rootCause,
            List<Object> significantChannels,
            List<Object> excludedChannels,
            List<ChannelRateResponse> channelRates,
            SourceSignalsResponse sourceSignals,
            List<Object> evidenceInquiryIds,
            String linkedChangeId
    ) {
        public static AlertResponse of(DetectionAlert alert, List<SubAspectResponse> subAspects,
                                       List<Object> significantChannels, List<Object> excludedChannels,
                                       List<ChannelRateResponse> channelRates,
                                       SourceSignalsResponse sourceSignals, List<Object> evidenceInquiryIds) {
            return new AlertResponse(
                    alert.getAlertCode(),
                    alert.getUpdatesAlertCode(),
                    alert.getDetectedAt(),
                    alert.getProductGroupId(),
                    alert.getChannel(),
                    alert.getMainAspect(),
                    subAspects,
                    alert.getWindowStart(),
                    alert.getWindowEnd(),
                    alert.getVerdict(),
                    alert.getDetectionConfidence(),
                    alert.getRecommendedAction(),
                    alert.getAlertStatus(),
                    alert.isScopeIn(),
                    StatsResponse.of(alert.getStats()),
                    RootCauseResponse.of(alert.getRootCause()),
                    significantChannels,
                    excludedChannels,
                    channelRates,
                    sourceSignals,
                    evidenceInquiryIds,
                    alert.getLinkedChangeId()
            );
        }
    }

    public record SourceSignalsResponse(
            Boolean cs,
            Boolean review,
            String interpretation
    ) {
    }

    public record ChannelRateResponse(
            String channel,
            BigDecimal rate,
            Boolean excluded,
            Integer total
    ) {
    }

    public record SubAspectResponse(
            Aspect aspect,
            BigDecimal delta,
            @JsonAlias("recommended_action") RecommendedAction recommendedAction
    ) {
    }

    public record StatsResponse(
            StatsSource source,
            BigDecimal curRate,
            BigDecimal pastRate,
            BigDecimal delta,
            boolean bhSignificant,
            int curTotal
    ) {
        private static StatsResponse of(AlertStats stats) {
            if (stats == null) {
                return null;
            }
            return new StatsResponse(
                    stats.getSource(),
                    stats.getCurRate(),
                    stats.getPastRate(),
                    stats.getDelta(),
                    stats.isBhSignificant(),
                    stats.getCurTotal()
            );
        }
    }

    public record RootCauseResponse(
            String label,
            Integer count,
            Integer total,
            Boolean consistent
    ) {
        private static RootCauseResponse of(AlertRootCause rootCause) {
            if (rootCause == null) {
                return null;
            }
            return new RootCauseResponse(
                    rootCause.getLabel(),
                    rootCause.getCount(),
                    rootCause.getTotal(),
                    rootCause.getConsistent()
            );
        }
    }
}
