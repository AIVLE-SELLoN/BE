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

    /**
     * 재시도(dead-letter)용 원본 Kafka 메시지 스냅샷 - 실패 시에만 채워진다.
     * Mock Producer/Kafka를 다시 거치지 않고 이 값 그대로 RawChannelEventIngestService.ingest()를
     * 재호출해 "그 실패 건 하나만" 재처리하기 위함.
     */
    @Column(name = "event_type")
    private String eventType;

    @Column(name = "topic")
    private String topic;

    @Column(name = "timestamp_field")
    private String timestampField;

    @Column(name = "message_key")
    private String messageKey;

    @Lob
    @Column(name = "payload")
    private String payload;

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

    public static ChannelSyncLog failure(UsersChannel usersChannel, String failReason,
                                          String eventType, String topic, String timestampField,
                                          String messageKey, String payload) {
        ChannelSyncLog entity = failure(usersChannel, failReason);
        entity.eventType = eventType;
        entity.topic = topic;
        entity.timestampField = timestampField;
        entity.messageKey = messageKey;
        entity.payload = payload;
        return entity;
    }
}
