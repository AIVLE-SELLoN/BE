package com.aivle.sellon.domain.mypage.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RecipientRequest(
        Long recipientId,
        @NotBlank @Size(max = 50) String department,
        @NotBlank @Email String email
) {
}
