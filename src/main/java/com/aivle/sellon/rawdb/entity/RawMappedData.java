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
@Table(name = "mapped_data")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RawMappedData {

    @Id
    @Column(name = "variant_row_id")
    private String variantRowId;

    @Column(name = "product_group_id")
    private String productGroupId;

    @Column(name = "mapping_method")
    private String mappingMethod;

    @Column(name = "mapping_confidence")
    private Double mappingConfidence;

    @Column(name = "mapped_at")
    private OffsetDateTime mappedAt;
}
