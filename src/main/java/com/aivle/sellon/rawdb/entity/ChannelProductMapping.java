package com.aivle.sellon.rawdb.entity;

import com.aivle.sellon.domain.channels.enums.MappingMethod;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * raw_channel_product 한 행(variant_row_id)에 대한 상품 매핑 결과(mapped_data).
 * 메인서버가 raw db 적재 시점에 SLM 임베딩 우선 -> 룰 기반 보정으로 계산해 함께 기록한다.
 * product_group_id가 비어있으면(=null) 아직 매핑 미확정(보류) 상태.
 */
@Entity
@Table(name = "mapped_data")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class ChannelProductMapping {

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

    /**
     * 매칭 툴의 mapped_product_code에 대응 - 같은 상품으로 묶인 그룹의 식별자.
     */
    @Column(name = "product_group_id")
    private String productGroupId;

    @Enumerated(EnumType.STRING)
    @Column(name = "mapping_method")
    private MappingMethod mappingMethod;

    @Column(name = "mapping_confidence")
    private Double mappingConfidence;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "mapped_at")
    private LocalDateTime mappedAt;

    public static ChannelProductMapping pending(String variantRowId, String channel, String channelProductId) {
        ChannelProductMapping entity = new ChannelProductMapping();
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
