package com.aivle.sellon.rawdb.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

import java.time.OffsetDateTime;

// raw db(products) 채널 상품 원본. 상품 매핑용 컨슈머가 적재, BE는 읽기 전용. PK는 variant_row_id.
@Entity
@Immutable
@Table(name = "products")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RawProduct {

    // 옵션 조합 단위 자연키 (input_channel_products.csv의 variant_row_id와 대응, 별도 PK 없음)
    @Id
    @Column(name = "variant_row_id")
    private String variantRowId;

    @Column(name = "channel_id", nullable = false)
    private String channelId;

    // 같은 기본 상품의 옵션들을 묶는 채널 측 상품 식별자 (PK 아님)
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

    @Column(name = "fetched_at", nullable = false)
    private OffsetDateTime fetchedAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public static RawProduct of(String variantRowId, String channelId,
                                String channelProductId, String channelProductName,
                                String optionGroupNames, String channelOptionName,
                                Long salePrice, Long originalPrice) {
        RawProduct entity = new RawProduct();
        entity.variantRowId = variantRowId;
        entity.channelId = channelId;
        entity.channelProductId = channelProductId;
        entity.channelProductName = channelProductName;
        entity.optionGroupNames = optionGroupNames;
        entity.channelOptionName = channelOptionName;
        entity.salePrice = salePrice;
        entity.originalPrice = originalPrice;
        // DB 컬럼이 NOT NULL(디폴트 없음)인데 매핑이 빠져있어 신규 insert가 제약 위반으로 실패하던 버그 수정
        OffsetDateTime now = OffsetDateTime.now();
        entity.fetchedAt = now;
        entity.updatedAt = now;
        return entity;
    }
}
