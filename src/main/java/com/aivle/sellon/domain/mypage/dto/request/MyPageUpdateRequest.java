package com.aivle.sellon.domain.mypage.dto.request;

import jakarta.validation.Valid;

public record MyPageUpdateRequest(
        String email,
        String verificationToken,
        String brandName,
        @Valid ReportSettingRequest reportSetting
) {
}
