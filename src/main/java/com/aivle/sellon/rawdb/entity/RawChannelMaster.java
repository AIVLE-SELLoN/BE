package com.aivle.sellon.rawdb.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 「Raw DB 스키마 확정 (8/7)」§2-1 channel 마스터. main server(우리) 소유·쓰기 대상.
 * channel_id는 문자열 자체가 PK이고 Channel enum(COUPANG/NAVER/ZIGZAG) 값과 동일하다.
 */
@Entity
@Table(name = "channel")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RawChannelMaster {

    @Id
    @Column(name = "channel_id")
    private String channelId;

    @Column(name = "display_name")
    private String displayName;

    @Column(name = "connected_at")
    private LocalDateTime connectedAt;

    @Column(name = "status")
    private String status;

    public static RawChannelMaster of(String channelId) {
        RawChannelMaster entity = new RawChannelMaster();
        entity.channelId = channelId;
        entity.displayName = channelId;
        entity.connectedAt = LocalDateTime.now();
        entity.status = "active";
        return entity;
    }
}
