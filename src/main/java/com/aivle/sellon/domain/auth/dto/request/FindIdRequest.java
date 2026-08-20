package com.aivle.sellon.domain.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

public record FindIdRequest(
        @NotBlank(message = "회사명을 입력해주세요.")
        String companyName,

        @NotBlank(message = "사용자 이름을 입력해주세요.")
        String userName
) {
}
