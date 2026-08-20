package com.aivle.sellon.domain.channels.dto.response;

import com.aivle.sellon.domain.channels.entity.productmapping.ProductMappingReviewItem;

public record ProductMappingReviewItemResponse(
        Long id,
        String channelA,
        String productKeyA,
        String channelB,
        String productKeyB,
        Double ruleScore,
        Double embScore,
        String verdict,
        String basis,
        boolean resolved
) {
    public static ProductMappingReviewItemResponse from(ProductMappingReviewItem item) {
        return new ProductMappingReviewItemResponse(
                item.getId(),
                item.getChannelA(),
                item.getProductKeyA(),
                item.getChannelB(),
                item.getProductKeyB(),
                item.getRuleScore(),
                item.getEmbScore(),
                item.getVerdict(),
                item.getBasis(),
                item.isResolved()
        );
    }
}
