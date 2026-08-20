package com.aivle.sellon.domain.channels.dto.response;

import java.util.List;

public record ChannelInquiryTypeRadarResponse(
        Long usersChannelKey,
        String channelType,
        List<ChannelInquiryTypeResponse> distribution
) {
}
