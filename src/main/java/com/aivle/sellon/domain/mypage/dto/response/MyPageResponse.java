package com.aivle.sellon.domain.mypage.dto.response;

import com.aivle.sellon.domain.user.entity.User;
import com.aivle.sellon.domain.user.enums.Role;

public record MyPageResponse(
        String email,
        String name,
        String brandName,
        Role role,
        String companyKey,
        boolean companyKeyIssued,
        EditableResponse editable,
        ReportSettingResponse reportSetting
) {
    public static MyPageResponse of(
            User user,
            String companyKey,
            boolean companyKeyIssued,
            ReportSettingResponse reportSetting
    ) {
        return new MyPageResponse(
                user.getEmail(),
                user.getName(),
                user.getCompany().getName(),
                user.getRole(),
                companyKey,
                companyKeyIssued,
                EditableResponse.of(user.getRole()),
                reportSetting
        );
    }
}
