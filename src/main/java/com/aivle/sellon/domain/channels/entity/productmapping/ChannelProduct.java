package com.aivle.sellon.domain.channels.entity.productmapping;

import com.aivle.sellon.domain.channels.entity.connection.UsersChannel;
import com.aivle.sellon.domain.channels.enums.MappingMethod;
import com.aivle.sellon.domain.channels.enums.MappingStatus;
import com.aivle.sellon.global.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "channel_product")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChannelProduct extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "channel_product_key")
    private Long channelProductKey;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "users_channel_key", nullable = false)
    private UsersChannel usersChannel;

    @Column(name = "source_sku", nullable = false)
    private String sourceSku;

    @Column(name = "channel_item_id", nullable = false)
    private String channelItemId;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(name = "option_name")
    private String optionName;

    @Column(name = "price")
    private Long price;

    @Column(name = "original_price")
    private Long originalPrice;

    /**
     * 매칭 툴 input_channel_products.csv의 option_group_names 컬럼 — 표시용 메타데이터로만 보관, 매칭 로직에는 사용하지 않음.
     */
    @Column(name = "option_group_names")
    private String optionGroupNames;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "master_product_key")
    private MasterProduct masterProduct;

    @Column(name = "similarity_score")
    private Double similarityScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "mapping_method")
    private MappingMethod mappingMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "mapping_status", nullable = false)
    private MappingStatus mappingStatus;


    public static ChannelProduct ofRaw(UsersChannel usersChannel, String channelItemId, String sourceSku,
                                        String productName, String optionName, Long price) {
        return ofRaw(usersChannel, channelItemId, sourceSku, productName, optionName, price, null, null);
    }

    public static ChannelProduct ofRaw(UsersChannel usersChannel, String channelItemId, String sourceSku,
                                        String productName, String optionName, Long price,
                                        Long originalPrice, String optionGroupNames) {
        ChannelProduct entity = new ChannelProduct();
        entity.usersChannel = usersChannel;
        entity.channelItemId = channelItemId;
        entity.sourceSku = sourceSku;
        entity.productName = productName;
        entity.optionName = optionName;
        entity.price = price;
        entity.originalPrice = originalPrice;
        entity.optionGroupNames = optionGroupNames;
        entity.mappingStatus = MappingStatus.UNMATCHED;
        return entity;
    }

    public void autoMatch(MasterProduct masterProduct, Double similarityScore, MappingMethod mappingMethod, MappingStatus mappingStatus) {
        this.masterProduct = masterProduct;
        this.similarityScore = similarityScore;
        this.mappingMethod = mappingMethod;
        this.mappingStatus = mappingStatus;
    }

    public void manualConfirm(MasterProduct masterProduct) {
        this.masterProduct = masterProduct;
        this.mappingMethod = null;
        this.mappingStatus = MappingStatus.MANUAL_CONFIRMED;
    }
}
