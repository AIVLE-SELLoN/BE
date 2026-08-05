package com.aivle.sellon.domain.inquiries.dto;

import com.aivle.sellon.domain.inquiries.enums.InquireType;

public record CsInquiryRequest(
    String inquireTitle,
    String inquireContent,
    InquireType inquireType,
    String attachmentUrl
) {}
