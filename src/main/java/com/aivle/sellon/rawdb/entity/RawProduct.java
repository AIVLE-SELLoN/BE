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
@Table(name = "products")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RawProduct {

    @Id
    @Column(name = "variant_row_id")
    private String variantRowId;

    @Column(name = "channel_product_id")
    private String channelProductId;

    @Column(name = "channel_id")
    private String channelId;

    @Column(name = "channel_product_name")
    private String channelProductName;

    @Column(name = "option_group_names")
    private String optionGroupNames;

    @Column(name = "channel_option_name")
    private String channelOptionName;

    @Column(name = "sale_price")
    private Integer salePrice;

    @Column(name = "original_price")
    private Integer originalPrice;

    @Column(name = "fetched_at")
    private OffsetDateTime fetchedAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
