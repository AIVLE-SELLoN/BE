package com.aivle.sellon.domain.mypage.dto.request;

import com.aivle.sellon.domain.mypage.enums.RecipientDepartment;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RecipientRequest(
        Long recipientId,
        @NotNull RecipientDepartment department,
        @NotBlank @Email String email
) {
}
