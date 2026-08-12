package com.aivle.sellon.domain.inquiries.dto.request;

import com.aivle.sellon.domain.inquiries.enums.InquireType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CsInquiryUpdateRequest(
    @NotBlank(message = "문의 제목을 입력해주세요.")
    String inquireTitle,

    @NotBlank(message = "문의 내용을 입력해주세요.")
    String inquireContent,

    @NotNull(message = "문의 유형을 선택해주세요.")
    InquireType inquireType,

    String attachmentUrl
) {}
