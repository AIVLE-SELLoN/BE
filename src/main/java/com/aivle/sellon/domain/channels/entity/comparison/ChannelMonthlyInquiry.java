package com.aivle.sellon.domain.channels.entity.comparison;

import com.aivle.sellon.domain.channels.entity.connection.UsersChannel;
import com.aivle.sellon.global.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.YearMonth;

@Entity
@Table(name = "channel_monthly_inquiry")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChannelMonthlyInquiry extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "monthly_inquiry_key")
    private Long monthlyInquiryKey;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "users_channel_key", nullable = false)
    private UsersChannel usersChannel;

    @Column(name = "year_month", nullable = false)
    private String yearMonth;

    @Column(name = "inquiry_count", nullable = false)
    private Integer inquiryCount;

    public static ChannelMonthlyInquiry of(UsersChannel usersChannel, YearMonth yearMonth, Integer inquiryCount) {
        ChannelMonthlyInquiry entity = new ChannelMonthlyInquiry();
        entity.usersChannel = usersChannel;
        entity.yearMonth = yearMonth.toString();
        entity.inquiryCount = inquiryCount;
        return entity;
    }
}
