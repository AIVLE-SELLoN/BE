package com.aivle.sellon.domain.alert.dto.response;

import com.aivle.sellon.global.common.dto.CursorPageResponse;

import java.util.List;

public record AlertListResponse(
        List<AlertSummaryResponse> items,
        long totalCount,
        long unreadCount,
        boolean hasNext,
        String nextCursor
) {
    public static AlertListResponse of(CursorPageResponse<AlertSummaryResponse> page,
                                       long totalCount, long unreadCount) {
        return new AlertListResponse(
                page.content(),
                totalCount,
                unreadCount,
                page.hasNext(),
                page.nextCursor()
        );
    }
}
