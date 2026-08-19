package com.aivle.sellon.rawdb.entity;

import com.aivle.sellon.domain.channels.enums.MappingMethod;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

// products 한 행(variant_row_id)에 대한 상품 매핑 결과 - product_group_id가 null이면 미확정(보류)
// raw DB 문서 §4.3 기준 PK는 variant_row_id. channel_product_mapping_id는 DB에서 identity로 자동 채번되므로 매핑하지 않는다.
@Entity
@Table(name = "mapped_data")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RawMappedData {

    @Id
    @Column(name = "variant_row_id")
    private String variantRowId;

    @Column(name = "channel", nullable = false)
    private String channel;

    @Column(name = "channel_product_id", nullable = false)
    private String channelProductId;

    // 매칭 툴의 mapped_product_code에 대응하는 상품 그룹 식별자
    @Column(name = "product_group_id")
    private String productGroupId;

    // raw DB 문서 §4.3 기준 String으로 저장 (enum은 서비스 레이어까지만 사용, 엔티티 필드는 문서 표기에 맞춤)
    @Column(name = "mapping_method")
    private String mappingMethod;

    @Column(name = "mapping_confidence")
    private Double mappingConfidence;

    // raw DB 문서 §4.3 기준 TIMESTAMPTZ -> OffsetDateTime (실제 DB 컬럼은 without time zone)
    @Column(name = "mapped_at")
    private OffsetDateTime mappedAt;

    // DB 컬럼이 NOT NULL(디폴트 없음)인데 매핑이 빠져있어 신규 insert가 제약 위반으로 실패하던 버그 수정
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public static RawMappedData pending(String variantRowId, String channel, String channelProductId) {
        RawMappedData entity = new RawMappedData();
        entity.variantRowId = variantRowId;
        entity.channel = channel;
        entity.channelProductId = channelProductId;
        entity.createdAt = LocalDateTime.now();
        return entity;
    }

    public void confirm(String productGroupId, MappingMethod mappingMethod, Double mappingConfidence, OffsetDateTime mappedAt) {
        this.productGroupId = productGroupId;
        this.mappingMethod = mappingMethod != null ? mappingMethod.name() : null;
        this.mappingConfidence = mappingConfidence;
        this.mappedAt = mappedAt;
    }
}
