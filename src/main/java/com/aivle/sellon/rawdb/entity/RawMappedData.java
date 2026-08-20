package com.aivle.sellon.rawdb.entity;

import com.aivle.sellon.domain.channels.enums.MappingMethod;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Entity
@Table(name = "mapped_data")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RawMappedData {

    @Id
    @Column(name = "variant_row_id")
    private String variantRowId;

    // 매칭 툴의 mapped_product_code에 대응하는 상품 그룹 식별자
    @Column(name = "product_group_id")
    private String productGroupId;

    // raw DB 문서 §4.3 기준 String으로 저장 (enum은 서비스 레이어까지만 사용, 엔티티 필드는 문서 표기에 맞춤)
    @Column(name = "mapping_method")
    private String mappingMethod;

    @Column(name = "mapping_confidence")
    private Double mappingConfidence;

    // raw DB 문서 §4.1 기준 TIMESTAMPTZ -> OffsetDateTime
    @Column(name = "mapped_at")
    private OffsetDateTime mappedAt;

    public static RawMappedData pending(String variantRowId) {
        RawMappedData entity = new RawMappedData();
        entity.variantRowId = variantRowId;
        return entity;
    }

    public void confirm(String productGroupId, MappingMethod mappingMethod, Double mappingConfidence, OffsetDateTime mappedAt) {
        this.productGroupId = productGroupId;
        this.mappingMethod = mappingMethod != null ? mappingMethod.name() : null;
        this.mappingConfidence = mappingConfidence;
        this.mappedAt = mappedAt;
    }
}
