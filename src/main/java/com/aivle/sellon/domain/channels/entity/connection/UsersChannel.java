package com.aivle.sellon.domain.channels.entity.connection;

import com.aivle.sellon.domain.channels.enums.ConnectionStatus;
import com.aivle.sellon.domain.company.entity.Company;
import com.aivle.sellon.global.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/** 회사가 채널(쿠팡/지그재그/네이버)에 연동한 기록 - company 단위 테넌트, 연동은 ROOT만 가능(검증은 ChannelService), (company_id, channel_type) 유니크로 DB 레벨 중복 방지. */
@Entity
@Table(
        name = "users_channel",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_users_channel_company_channel_type",
                columnNames = {"company_id", "channel_type"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UsersChannel extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "users_channel_key")
    private Long usersChannelKey;

    @Column(name = "channel_type", nullable = false)
    private String channelType;

    @Column(name = "channel_code")
    private String channelCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "connection_status", nullable = false)
    private ConnectionStatus connectionStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    // raw db 신규 건수를 마지막으로 확인한 시각 (폴링 기준점).
    @Column(name = "last_sync_checked_at")
    private OffsetDateTime lastSyncCheckedAt;

    public static UsersChannel of(Company company, String channelType, String channelCode) {
        UsersChannel entity = new UsersChannel();
        entity.company = company;
        entity.channelType = channelType;
        entity.channelCode = channelCode;
        entity.connectionStatus = ConnectionStatus.PENDING;
        return entity;
    }

    public void updateStatus(ConnectionStatus connectionStatus) {
        this.connectionStatus = connectionStatus;
    }

    public void updateLastSyncCheckedAt(OffsetDateTime checkedAt) {
        this.lastSyncCheckedAt = checkedAt;
    }
}
