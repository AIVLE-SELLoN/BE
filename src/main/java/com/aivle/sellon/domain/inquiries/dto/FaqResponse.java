package com.aivle.sellon.domain.inquiries.dto;

import com.aivle.sellon.domain.inquiries.enums.InquireType;

public record FaqResponse(
    Long faqKey,
    String faqTitle,
    String faqQuestion,
    String faqAnswer,
    InquireType faqCategory
) {}
