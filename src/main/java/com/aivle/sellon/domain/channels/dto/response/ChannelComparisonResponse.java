package com.aivle.sellon.domain.channels.dto.response;

import com.aivle.sellon.domain.channels.entity.comparison.ChannelComparison;

public record ChannelComparisonResponse(
        Long usersChannelKey,
        String channelType,
        Integer totalInquiryCount,
        Double totalInquiryChangeRate,
        String totalInquiryComment,
        Double inquiryRatePerOrder,
        String inquiryRateComment,
        Double avgRating,
        String avgRatingComment,
        Double reviewRatePerOrder,
        String reviewRateComment,
        Double positiveRatio,
        Double neutralRatio,
        Double negativeRatio,
        String sentimentComment,
        String aspectComment
) {
    public static ChannelComparisonResponse from(ChannelComparison c) {
        return new ChannelComparisonResponse(
                c.getUsersChannel().getUsersChannelKey(),
                c.getUsersChannel().getChannelType(),
                c.getTotalInquiryCount(), c.getTotalInquiryChangeRate(), c.getTotalInquiryComment(),
                c.getInquiryRatePerOrder(), c.getInquiryRateComment(),
                c.getAvgRating(), c.getAvgRatingComment(),
                c.getReviewRatePerOrder(), c.getReviewRateComment(),
                c.getPositiveRatio(), c.getNeutralRatio(), c.getNegativeRatio(), c.getSentimentComment(),
                c.getAspectComment()
        );
    }
}
