package com.aivle.sellon.rawdb.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

import java.time.OffsetDateTime;

@Entity
@Immutable
@Table(name = "cs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RawCs {

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

    @Column(name = "inquired_at", nullable = false)
    private OffsetDateTime inquiredAt;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    public static RawCs of(String id, String channelProductId, String productGroupId,
                           String channelId, String content, OffsetDateTime inquiredAt,
                           OffsetDateTime createdAt) {
        RawCs entity = new RawCs();
        entity.id = id;
        entity.channelProductId = channelProductId;
        entity.productGroupId = productGroupId;
        entity.channelId = channelId;
        entity.content = content;
        entity.inquiredAt = inquiredAt;
        entity.createdAt = createdAt;
        return entity;
    }
}
