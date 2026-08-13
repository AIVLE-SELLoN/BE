package com.aivle.sellon.domain.inquiries.dto.response;

public record FaqResponse(
    Long faqKey,
    String faqTitle,
    String faqQuestion,
    String faqAnswer
) {}
