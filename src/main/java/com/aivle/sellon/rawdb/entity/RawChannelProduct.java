package com.aivle.sellon.rawdb.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Kafka로 유입된 채널 상품 원본(raw) 데이터.
 * raw db(별도 PostgreSQL, RawDataSourceConfig)에 적재되며, 메인 서비스 DB의 UsersChannel과는
 * 물리적으로 다른 데이터소스라 JPA 연관관계 대신 usersChannelKey를 값으로만 들고 있는다.
 */
@Entity
@Table(name = "raw_channel_product")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class RawChannelProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "raw_channel_product_id")
    private Long id;

    @Column(name = "users_channel_key", nullable = false)
    private Long usersChannelKey;

    /**
     * 채널 유입 이벤트 원본의 행 식별자 (매칭 툴 input_channel_products.csv의 variant_row_id와 대응).
     */
    @Column(name = "variant_row_id", nullable = false)
    private String variantRowId;

    @Column(name = "channel", nullable = false)
    private String channel;

    /**
     * 같은 기본 상품의 옵션들을 묶는 채널 측 상품 식별자 (channel_product_id).
     */
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
    @Column(name = "ingested_at", nullable = false, updatable = false)
    private LocalDateTime ingestedAt;

    public static RawChannelProduct of(Long usersChannelKey, String variantRowId, String channel,
                                        String channelProductId, String channelProductName,
                                        String optionGroupNames, String channelOptionName,
                                        Long salePrice, Long originalPrice) {
        RawChannelProduct entity = new RawChannelProduct();
        entity.usersChannelKey = usersChannelKey;
        entity.variantRowId = variantRowId;
        entity.channel = channel;
        entity.channelProductId = channelProductId;
        entity.channelProductName = channelProductName;
        entity.optionGroupNames = optionGroupNames;
        entity.channelOptionName = channelOptionName;
        entity.salePrice = salePrice;
        entity.originalPrice = originalPrice;
        return entity;
    }
}
