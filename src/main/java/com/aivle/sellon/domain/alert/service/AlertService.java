package com.aivle.sellon.domain.alert.service;

import com.aivle.sellon.domain.alert.dto.response.AlertListResponse;
import com.aivle.sellon.domain.alert.dto.response.AlertSummaryResponse;
import com.aivle.sellon.domain.notification.entity.Notification;
import com.aivle.sellon.domain.notification.repository.NotificationRepository;
import com.aivle.sellon.global.common.dto.CursorPageResponse;
import com.aivle.sellon.global.common.utils.CursorUtils;
import com.aivle.sellon.global.security.principal.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AlertService {

    private final NotificationRepository notificationRepository;
    private final CursorUtils cursorUtils;

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
}
