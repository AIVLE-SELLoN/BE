package com.aivle.sellon.domain.channels.dto.response;

public record ChannelMonthlyInquiryResponse(
        String yearMonth,
        Integer inquiryCount
) {
}
