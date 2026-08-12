package com.aivle.sellon.domain.channels.dto.response;

import com.aivle.sellon.domain.channels.entity.connection.UsersChannel;
import com.aivle.sellon.domain.channels.enums.ConnectionStatus;

public record ChannelConnectionResponse(
        Long usersChannelKey,
        String channelType,
        ConnectionStatus connectionStatus,
        String failReason
) {
    public static ChannelConnectionResponse from(UsersChannel usersChannel) {
        return new ChannelConnectionResponse(
                usersChannel.getUsersChannelKey(),
                usersChannel.getChannelType(),
                usersChannel.getConnectionStatus(),
                null
        );
    }

    public static ChannelConnectionResponse failed(String channelType, String failReason) {
        return new ChannelConnectionResponse(null, channelType, ConnectionStatus.DISCONNECTED, failReason);
    }
}
