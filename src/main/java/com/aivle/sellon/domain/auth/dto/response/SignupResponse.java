package com.aivle.sellon.domain.auth.dto.response;

import com.aivle.sellon.domain.user.entity.User;
import com.aivle.sellon.domain.user.enums.Role;

public record SignupResponse(
        Long userId,
        String email,
        Role role,
        String companyKey
) {
    public static SignupResponse of(User user, String companyKey) {
        return new SignupResponse(user.getId(), user.getEmail(), user.getRole(), companyKey);
    }
}
