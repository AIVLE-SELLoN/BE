package com.aivle.sellon.domain.channels.entity.comparison;

import com.aivle.sellon.domain.channels.entity.connection.UsersChannel;
import com.aivle.sellon.domain.channels.enums.InquiryType;
import com.aivle.sellon.global.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 채널별 CS 문의 유형 분포(Type B divergence 산출용).
 */
@Entity
@Table(name = "channel_inquiry_type_stat")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChannelInquiryTypeStat extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "inquiry_type_stat_key")
    private Long inquiryTypeStatKey;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "users_channel_key", nullable = false)
    private UsersChannel usersChannel;

    @Enumerated(EnumType.STRING)
    @Column(name = "inquire_type", nullable = false)
    private InquiryType inquireType;

    @Column(name = "inquiry_count", nullable = false)
    private Integer inquiryCount;

    public static ChannelInquiryTypeStat of(UsersChannel usersChannel, InquiryType inquireType, Integer inquiryCount) {
        ChannelInquiryTypeStat entity = new ChannelInquiryTypeStat();
        entity.usersChannel = usersChannel;
        entity.inquireType = inquireType;
        entity.inquiryCount = inquiryCount;
        return entity;
    }
}
