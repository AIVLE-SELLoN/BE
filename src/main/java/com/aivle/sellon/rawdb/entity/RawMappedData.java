package com.aivle.sellon.rawdb.entity;

import com.aivle.sellon.domain.channels.enums.MappingMethod;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// products 한 행(variant_row_id)에 대한 상품 매핑 결과 - product_group_id가 null이면 미확정(보류)
@Entity
@Table(name = "mapped_data")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RawMappedData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "channel_product_mapping_id")
    private Long id;

    @Column(name = "variant_row_id", nullable = false, unique = true)
    private String variantRowId;

    @Column(name = "channel", nullable = false)
    private String channel;

    @Column(name = "channel_product_id", nullable = false)
    private String channelProductId;

    // 매칭 툴의 mapped_product_code에 대응하는 상품 그룹 식별자
    @Column(name = "product_group_id")
    private String productGroupId;

    @Enumerated(EnumType.STRING)
    @Column(name = "mapping_method")
    private MappingMethod mappingMethod;

    @Column(name = "mapping_confidence")
    private Double mappingConfidence;

    @Column(name = "mapped_at")
    private LocalDateTime mappedAt;

    public static RawMappedData pending(String variantRowId, String channel, String channelProductId) {
        RawMappedData entity = new RawMappedData();
        entity.variantRowId = variantRowId;
        entity.channel = channel;
        entity.channelProductId = channelProductId;
        return entity;
    }

    public void confirm(String productGroupId, MappingMethod mappingMethod, Double mappingConfidence, LocalDateTime mappedAt) {
        this.productGroupId = productGroupId;
        this.mappingMethod = mappingMethod;
        this.mappingConfidence = mappingConfidence;
        this.mappedAt = mappedAt;
    }
}
