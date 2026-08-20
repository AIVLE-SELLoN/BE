package com.aivle.sellon.domain.inquiries.dto.response;

import com.aivle.sellon.domain.inquiries.enums.InquireType;
import com.aivle.sellon.domain.inquiries.enums.InquiryStatus;

import java.time.LocalDateTime;

public record CsInquiryResponse(
    Long inquireKey,
    String inquireTitle,
    String inquireContent,
    InquireType inquireType,
    String attachmentUrl,
    String inquireAnswer,
    InquiryStatus inquiryStatus,
    String authorName,
    LocalDateTime createdAt
) {}
