package com.aivle.sellon.domain.mypage.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record ProfileImagePresignedUrlRequest(
        @NotBlank String fileName,
        @Positive long fileSize
) {
}
