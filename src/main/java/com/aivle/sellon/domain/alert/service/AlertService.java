package com.aivle.sellon.domain.alert.service;

import com.aivle.sellon.domain.alert.dto.response.AlertDetailResponse;
import com.aivle.sellon.domain.alert.dto.response.AlertListResponse;
import com.aivle.sellon.domain.alert.dto.response.AlertSummaryResponse;
import com.aivle.sellon.domain.alert.entity.DetectionAlert;
import com.aivle.sellon.domain.alert.exception.AlertNotFoundException;
import com.aivle.sellon.domain.alert.repository.DetectionAlertRepository;
import com.aivle.sellon.domain.notification.entity.Notification;
import com.aivle.sellon.domain.notification.enums.NotificationType;
import com.aivle.sellon.domain.notification.repository.NotificationRepository;
import com.aivle.sellon.global.common.dto.CursorPageResponse;
import com.aivle.sellon.global.common.utils.CursorUtils;
import com.aivle.sellon.global.security.principal.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AlertService {

    private final NotificationRepository notificationRepository;
    private final DetectionAlertRepository detectionAlertRepository;
    private final CursorUtils cursorUtils;
    private final JsonMapper jsonMapper;

    public AlertListResponse getAlerts(UserPrincipal principal, String cursor, int size, boolean unreadOnly) {
        Long cursorId = cursor != null ? cursorUtils.toId(cursor) : null;
        List<Notification> notifications = notificationRepository.findAllByCompanyId(
                principal.getCompanyId(), cursorId, unreadOnly, size + 1);

        boolean hasNext = notifications.size() > size;
        List<AlertSummaryResponse> items = notifications.stream()
                .limit(size)
                .map(AlertSummaryResponse::of)
                .toList();

        String nextCursor = hasNext ? cursorUtils.toCursor(items.getLast().notificationId()) : null;
        CursorPageResponse<AlertSummaryResponse> page = new CursorPageResponse<>(items, nextCursor, hasNext);

        return AlertListResponse.of(
                page,
                notificationRepository.countByCompanyId(principal.getCompanyId()),
                notificationRepository.countUnreadByCompanyId(principal.getCompanyId())
        );
    }

    public AlertDetailResponse getAlert(UserPrincipal principal, Long notificationId) {
        Notification notification = notificationRepository.findByIdAndCompanyId(notificationId, principal.getCompanyId())
                .orElseThrow(AlertNotFoundException::new);

        AlertDetailResponse.AlertResponse alert = notification.getType() == NotificationType.ANOMALY_DETECTED
                ? detectionAlertRepository.findById(notification.getNotificationTargetId())
                .map(this::toAlertResponse)
                .orElse(null)
                : null;

        return AlertDetailResponse.of(notification, alert);
    }

    private AlertDetailResponse.AlertResponse toAlertResponse(DetectionAlert alert) {
        return AlertDetailResponse.AlertResponse.of(
                alert,
                parseJsonArray(alert.getSubAspects()),
                parseJsonArray(alert.getSignificantChannels()),
                parseJsonArray(alert.getExcludedChannels()),
                parseSourceSignals(alert.getSourceSignals()),
                parseJsonArray(alert.getEvidenceInquiryIds())
        );
    }

    private AlertDetailResponse.SourceSignalsResponse parseSourceSignals(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return jsonMapper.readValue(value, AlertDetailResponse.SourceSignalsResponse.class);
        } catch (Exception e) {
            return null;
        }
    }

    private List<Object> parseJsonArray(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }

        try {
            JsonNode node = jsonMapper.readTree(value);
            if (!node.isArray()) {
                return List.of();
            }
            return jsonMapper.convertValue(node, new tools.jackson.core.type.TypeReference<List<Object>>() {
            });
        } catch (Exception e) {
            return List.of();
        }
    }
}
