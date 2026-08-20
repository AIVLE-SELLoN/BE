package com.aivle.sellon.domain.channels.dto.response;

import com.aivle.sellon.domain.channels.entity.synclog.ChannelSyncLog;
import com.aivle.sellon.domain.channels.enums.SyncStatus;

import java.time.LocalDateTime;

public record ChannelSyncLogResponse(
        Long syncLogKey,
        String channelType,
        LocalDateTime syncedAt,
        SyncStatus status,
        Integer syncedCount,
        String failReason
) {
    public static ChannelSyncLogResponse from(ChannelSyncLog log) {
        return new ChannelSyncLogResponse(
                log.getSyncLogKey(),
                log.getUsersChannel().getChannelType(),
                log.getCreatedDate(),
                log.getStatus(),
                log.getSyncedCount(),
                log.getFailReason()
        );
    }
}
