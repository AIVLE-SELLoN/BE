package com.aivle.sellon.domain.channels.dto.response;

import com.aivle.sellon.domain.channels.enums.MappingMethod;
import com.aivle.sellon.domain.channels.enums.MappingStatus;
import com.aivle.sellon.rawdb.entity.ChannelProductMapping;
import com.aivle.sellon.rawdb.entity.RawChannelProduct;

public record ChannelProductResponse(
        String variantRowId,
        String channelType,
        String channelProductId,
        String productName,
        String optionName,
        Long price,
        String masterSku,
        MappingMethod mappingMethod,
        MappingStatus mappingStatus
) {
    public static ChannelProductResponse of(RawChannelProduct product, ChannelProductMapping mapping) {
        boolean matched = mapping != null && mapping.getProductGroupId() != null;
        return new ChannelProductResponse(
                product.getVariantRowId(),
                product.getChannel(),
                product.getChannelProductId(),
                product.getChannelProductName(),
                product.getChannelOptionName(),
                product.getSalePrice(),
                matched ? mapping.getProductGroupId() : null,
                matched ? mapping.getMappingMethod() : null,
                resolveStatus(matched, mapping)
        );
    }

    private static MappingStatus resolveStatus(boolean matched, ChannelProductMapping mapping) {
        if (!matched) {
            return MappingStatus.UNMATCHED;
        }
        return mapping.getMappingMethod() == MappingMethod.MANUAL
                ? MappingStatus.MANUAL_CONFIRMED
                : MappingStatus.AUTO_MATCHED;
    }
}
