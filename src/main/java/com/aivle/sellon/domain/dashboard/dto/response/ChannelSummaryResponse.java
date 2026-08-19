package com.aivle.sellon.domain.dashboard.dto.response;

import com.aivle.sellon.domain.alert.enums.AlertChannel;

public record ChannelSummaryResponse(
        AlertChannel channel,
        String channelName,
        long csCount,
        long orderCount,
        Double avgRating
) {
    public static ChannelSummaryResponse of(AlertChannel channel, String channelName,
                                            long csCount, long orderCount, Double avgRating) {
        return new ChannelSummaryResponse(channel, channelName, csCount, orderCount, avgRating);
    }
}
