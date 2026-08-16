package com.aivle.sellon.rawdb.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

// raw db(products) 채널 상품 원본. 상품 매핑용 컨슈머가 적재, BE는 읽기 전용. PK는 variant_row_id.
@Entity
@Table(name = "products")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class RawProduct {

    @Id
    @Column(name = "variant_row_id")
    private String variantRowId;

    @Column(name = "users_channel_key", nullable = false)
    private Long usersChannelKey;

    @Column(name = "channel_id", nullable = false)
    private String channelId;

    // 같은 기본 상품의 옵션들을 묶는 채널 측 상품 식별자.
    @Column(name = "channel_product_id", nullable = false)
    private String channelProductId;

    @Column(name = "channel_product_name", nullable = false)
    private String channelProductName;

    @Column(name = "option_group_names")
    private String optionGroupNames;

    @Column(name = "channel_option_name")
    private String channelOptionName;

    @Column(name = "sale_price")
    private Long salePrice;

    @Column(name = "original_price")
    private Long originalPrice;

    @CreatedDate
    @Column(name = "fetched_at", nullable = false, updatable = false)
    private LocalDateTime fetchedAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static RawProduct of(Long usersChannelKey, String variantRowId, String channelId,
                                String channelProductId, String channelProductName,
                                String optionGroupNames, String channelOptionName,
                                Long salePrice, Long originalPrice) {
        RawProduct entity = new RawProduct();
        entity.usersChannelKey = usersChannelKey;
        entity.variantRowId = variantRowId;
        entity.channelId = channelId;
        entity.channelProductId = channelProductId;
        entity.channelProductName = channelProductName;
        entity.optionGroupNames = optionGroupNames;
        entity.channelOptionName = channelOptionName;
        entity.salePrice = salePrice;
        entity.originalPrice = originalPrice;
        return entity;
    }
}
