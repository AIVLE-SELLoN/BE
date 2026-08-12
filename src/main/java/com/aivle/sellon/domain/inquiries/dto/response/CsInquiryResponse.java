package com.aivle.sellon.domain.inquiries.dto.response;

import com.aivle.sellon.domain.inquiries.enums.InquireType;
import com.aivle.sellon.domain.inquiries.enums.InquiryStatus;

public record CsInquiryResponse(
    Long inquireKey,
    String inquireTitle,
    String inquireContent,
    InquireType inquireType,
    String attachmentUrl,
    String inquireAnswer,
    InquiryStatus inquiryStatus
) {}
