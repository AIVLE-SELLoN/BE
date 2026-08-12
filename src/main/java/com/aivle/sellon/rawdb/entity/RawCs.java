package com.aivle.sellon.rawdb.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

import java.time.OffsetDateTime;

@Entity
@Immutable
@Getter
@Table(name = "cs")
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

    @Column(name = "content")
    private String content;

    @Column(name = "inquired_at")
    private OffsetDateTime inquiredAt;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;
}
