package com.aivle.sellon.domain.channels.entity.synclog;

import com.aivle.sellon.domain.channels.entity.connection.UsersChannel;
import com.aivle.sellon.domain.channels.enums.SyncStatus;
import com.aivle.sellon.global.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "channel_sync_log")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChannelSyncLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sync_log_key")
    private Long syncLogKey;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "users_channel_key", nullable = false)
    private UsersChannel usersChannel;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private SyncStatus status;

    @Column(name = "synced_count")
    private Integer syncedCount;

    @Column(name = "fail_reason")
    private String failReason;

    public static ChannelSyncLog success(UsersChannel usersChannel, Integer syncedCount) {
        ChannelSyncLog entity = new ChannelSyncLog();
        entity.usersChannel = usersChannel;
        entity.status = SyncStatus.SUCCESS;
        entity.syncedCount = syncedCount;
        return entity;
    }

    public static ChannelSyncLog failure(UsersChannel usersChannel, String failReason) {
        ChannelSyncLog entity = new ChannelSyncLog();
        entity.usersChannel = usersChannel;
        entity.status = SyncStatus.FAILED;
        entity.failReason = failReason;
        return entity;
    }
}
