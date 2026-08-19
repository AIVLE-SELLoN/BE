package com.aivle.sellon.domain.dashboard.dto.response;

import java.util.List;

public record DashboardResponse(
        long unreadNotificationCount,
        List<ChannelSummaryResponse> channelSummary
) {
    public static DashboardResponse of(long unreadNotificationCount,
                                       List<ChannelSummaryResponse> channelSummary) {
        return new DashboardResponse(unreadNotificationCount, channelSummary);
    }
}
