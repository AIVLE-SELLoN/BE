package com.aivle.sellon.domain.channels.entity.comparison;

import com.aivle.sellon.domain.channels.entity.connection.UsersChannel;
import com.aivle.sellon.global.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * "채널별 주요 인사이트" 섹션의 행동 권고사항 한 줄 한 줄.
 * Agent2 비교 결과를 바탕으로 생성되어 채널당 여러 건이 순서대로 쌓인다.
 */
@Entity
@Table(name = "channel_insight")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChannelInsight extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "channel_insight_key")
    private Long channelInsightKey;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "users_channel_key", nullable = false)
    private UsersChannel usersChannel;

    @Column(name = "content", nullable = false)
    private String content;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    public static ChannelInsight of(UsersChannel usersChannel, String content, Integer displayOrder) {
        ChannelInsight entity = new ChannelInsight();
        entity.usersChannel = usersChannel;
        entity.content = content;
        entity.displayOrder = displayOrder;
        return entity;
    }
}
