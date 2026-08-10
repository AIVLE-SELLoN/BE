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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

@Service
@Slf4j
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

        AlertDetailResponse.AlertResponse alert = findAlertResponse(notification);

        return AlertDetailResponse.of(notification, alert);
    }

    private AlertDetailResponse.AlertResponse findAlertResponse(Notification notification) {
        if (notification.getType() != NotificationType.ANOMALY_DETECTED) {
            return null;
        }

        return detectionAlertRepository.findById(notification.getNotificationTargetId())
                .map(this::toAlertResponse)
                .orElseGet(() -> {
                    // notificationTargetId는 폴리모픽 참조라 DB FK를 걸 수 없어 정합성이 깨질 수 있음.
                    // 이때 404를 던지면 알림 상세가 통째로 안 열린다. 알림 메시지만이라도 보여주는 편이
                    // 나으므로 alert를 null로 두고 200을 반환하며, 어긋난 사실은 로그로만 남긴다.
                    log.warn("알림 대상 DetectionAlert를 찾을 수 없습니다. notificationId={}, notificationTargetId={}",
                            notification.getId(), notification.getNotificationTargetId());
                    return null;
                });
    }

    private AlertDetailResponse.AlertResponse toAlertResponse(DetectionAlert alert) {
        return AlertDetailResponse.AlertResponse.of(
                alert,
                parseJsonArray("subAspects", alert.getSubAspects()),
                parseJsonArray("significantChannels", alert.getSignificantChannels()),
                parseJsonArray("excludedChannels", alert.getExcludedChannels()),
                parseSourceSignals("sourceSignals", alert.getSourceSignals()),
                parseJsonArray("evidenceInquiryIds", alert.getEvidenceInquiryIds())
        );
    }

    private AlertDetailResponse.SourceSignalsResponse parseSourceSignals(String fieldName, String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return jsonMapper.readValue(value, AlertDetailResponse.SourceSignalsResponse.class);
        } catch (Exception e) {
            log.warn("JSON 객체 파싱에 실패했습니다. fieldName={}, value={}", fieldName, value, e);
            return null;
        }
    }

    private List<Object> parseJsonArray(String fieldName, String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }

        try {
            JsonNode node = jsonMapper.readTree(value);
            if (!node.isArray()) {
                log.warn("JSON 배열이 필요한 필드에 다른 JSON 타입이 들어왔습니다. fieldName={}, value={}",
                        fieldName, value);
                return List.of();
            }
            return jsonMapper.convertValue(node, new tools.jackson.core.type.TypeReference<List<Object>>() {
            });
        } catch (Exception e) {
            log.warn("JSON 배열 파싱에 실패했습니다. fieldName={}, value={}", fieldName, value, e);
            return List.of();
        }
    }
}
