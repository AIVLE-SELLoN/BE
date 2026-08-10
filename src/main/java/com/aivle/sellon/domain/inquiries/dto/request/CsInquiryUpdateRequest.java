package com.aivle.sellon.domain.inquiries.dto.request;

import com.aivle.sellon.domain.inquiries.enums.InquireType;

public record CsInquiryUpdateRequest(
    String inquireTitle,
    String inquireContent,
    InquireType inquireType,
    String attachmentUrl
) {}
