package com.aivle.sellon.domain.mypage.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ProfileImageCompleteRequest(
        @NotBlank String objectKey
) {
}
