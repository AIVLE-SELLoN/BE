package com.aivle.sellon.domain.channels.dto.response;

import com.aivle.sellon.domain.channels.entity.productmapping.ChannelProduct;
import com.aivle.sellon.domain.channels.enums.MappingMethod;
import com.aivle.sellon.domain.channels.enums.MappingStatus;

public record ChannelProductResponse(
        Long channelProductKey,
        String channelType,
        String sourceSku,
        String productName,
        String optionName,
        Long price,
        String masterSku,
        Double similarityScore,
        MappingMethod mappingMethod,
        MappingStatus mappingStatus
) {
    public static ChannelProductResponse from(ChannelProduct p) {
        return new ChannelProductResponse(
                p.getChannelProductKey(),
                p.getUsersChannel().getChannelType(),
                p.getSourceSku(),
                p.getProductName(),
                p.getOptionName(),
                p.getPrice(),
                p.getMasterProduct() != null ? p.getMasterProduct().getMasterSku() : null,
                p.getSimilarityScore(),
                p.getMappingMethod(),
                p.getMappingStatus()
        );
    }
}
