package com.aivle.sellon.rawdb.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 「Raw DB 스키마 확정 (8/7)」§2-5 reviews. main server(우리) 소유·쓰기 대상. cs와 구조는
 * 같고 rating이 추가된다. cs와 달리 발생·적재 시각을 나누지 않고 created_at 하나다(§2-5).
 * id는 CSV의 review_id를 그대로 쓴다(§5-1 A안).
 */
@Entity
@Table(name = "reviews")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RawReview {

    @Id
    @Column(name = "id")
    private String id;

    @Column(name = "channel_product_id")
    private String channelProductId;

    @Column(name = "product_group_id")
    private String productGroupId;

    @Column(name = "channel_id")
    private String channelId;

    @Column(name = "content", nullable = false)
    private String content;

    @Column(name = "rating")
    private Integer rating;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public static RawReview of(String id, String channelProductId, String productGroupId,
                                String channelId, String content, Integer rating,
                                LocalDateTime createdAt) {
        RawReview entity = new RawReview();
        entity.id = id;
        entity.channelProductId = channelProductId;
        entity.productGroupId = productGroupId;
        entity.channelId = channelId;
        entity.content = content;
        entity.rating = rating;
        entity.createdAt = createdAt;
        return entity;
    }
}
