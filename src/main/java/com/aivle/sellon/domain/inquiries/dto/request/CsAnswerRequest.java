package com.aivle.sellon.domain.inquiries.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CsAnswerRequest(
    @NotBlank(message = "답변 내용을 입력해주세요.")
    String inquireAnswer
) {}
