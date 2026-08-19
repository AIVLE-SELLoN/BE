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
        Long price,
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
                matched && mapping.getMappingMethod() != null ? MappingMethod.valueOf(mapping.getMappingMethod()) : null,
                resolveStatus(matched, skipped, mapping)
        );
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
