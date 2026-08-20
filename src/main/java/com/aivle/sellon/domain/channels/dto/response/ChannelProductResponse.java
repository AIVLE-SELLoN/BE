package com.aivle.sellon.domain.channels.dto.response;

import com.aivle.sellon.domain.channels.enums.MappingMethod;
import com.aivle.sellon.domain.channels.enums.MappingStatus;
import com.aivle.sellon.rawdb.entity.RawMappedData;
import com.aivle.sellon.rawdb.entity.RawProduct;

public record ChannelProductResponse(
        String variantRowId,
        String channelType,
        String channelProductId,
        String productName,
        String optionName,
        Integer price,
        String productGroupId,
        MappingMethod mappingMethod,
        MappingStatus mappingStatus
) {
    public static ChannelProductResponse of(RawProduct product, RawMappedData mapping) {
        return of(product, mapping, false);
    }

    // skipped: 메인 db(SkippedMapping)에 별도로 저장된 "건너뜀" 여부 - raw db 스키마는 건드리지 않는다.
    public static ChannelProductResponse of(RawProduct product, RawMappedData mapping, boolean skipped) {
        boolean matched = mapping != null && mapping.getProductGroupId() != null;
        return new ChannelProductResponse(
                product.getVariantRowId(),
                product.getChannelId(),
                product.getChannelProductId(),
                product.getChannelProductName(),
                product.getChannelOptionName(),
                product.getSalePrice(),
                matched ? mapping.getProductGroupId() : null,
                matched ? parseMappingMethod(mapping.getMappingMethod()) : null,
                resolveStatus(matched, skipped, mapping)
        );
    }

    // mapped_data.mapping_method는 raw db 외부 프로듀서(초기 시딩/배치 매칭 등)도 값을 써서
    // 우리 enum에 없는 값("AUTO", "INITIAL_SEED" 등)이 섞여 있다. valueOf가 그대로 터지면
    // 그 행 하나 때문에 목록 조회 전체가 500으로 죽으므로, 모르는 값은 null로 내려보낸다.
    private static MappingMethod parseMappingMethod(String mappingMethod) {
        if (mappingMethod == null) {
            return null;
        }
        try {
            return MappingMethod.valueOf(mappingMethod);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static MappingStatus resolveStatus(boolean matched, boolean skipped, RawMappedData mapping) {
        if (!matched) {
            return skipped ? MappingStatus.SKIPPED : MappingStatus.UNMATCHED;
        }
        return MappingMethod.MANUAL.name().equals(mapping.getMappingMethod())
                ? MappingStatus.MANUAL_CONFIRMED
                : MappingStatus.AUTO_MATCHED;
    }
}
