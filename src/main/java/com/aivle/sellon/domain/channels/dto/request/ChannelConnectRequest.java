package com.aivle.sellon.domain.channels.dto.request;

public record ChannelConnectRequest(
        String channelType,
        String channelCode
) {
}
