package com.aivle.sellon.domain.channels.entity.connection;

import com.aivle.sellon.domain.channels.enums.ConnectionStatus;
import com.aivle.sellon.domain.company.entity.Company;
import com.aivle.sellon.global.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 회사가 특정 채널(쿠팡/지그재그/네이버 등)에 연동한 기록.
 * 채널 연동은 회사 단위 리소스라 company로 테넌트를 구분한다(같은 회사면 ROOT/MEMBER 누구나 조회 가능).
 * 연동 자체는 ROOT 권한을 가진 계정만 수행할 수 있다 (ChannelService에서 검증).
 * (company_id, channel_type) 조합은 유니크해야 한다 - 동시 연동 요청으로 인한 중복 행 생성을 DB 레벨에서 방지.
 */
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

    public void updateChannelCode(String channelCode) {
        this.channelCode = channelCode;
    }
}
