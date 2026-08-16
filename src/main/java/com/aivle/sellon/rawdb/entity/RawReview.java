package com.aivle.sellon.rawdb.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

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
    private Short rating;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    public static RawReview of(String id, String channelProductId, String productGroupId,
                                String channelId, String content, Short rating,
                                OffsetDateTime createdAt) {
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
