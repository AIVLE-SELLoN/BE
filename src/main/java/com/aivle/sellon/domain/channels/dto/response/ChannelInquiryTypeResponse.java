package com.aivle.sellon.domain.channels.dto.response;

import com.aivle.sellon.domain.channels.enums.InquiryType;

public record ChannelInquiryTypeResponse(
        InquiryType inquireType,
        Integer inquiryCount,
        Double ratio
) {
}
