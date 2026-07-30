package com.aivle.sellon.domain.auth.dto.response;

import com.aivle.sellon.domain.user.entity.User;
import com.aivle.sellon.domain.user.enums.Role;

public record LoginResponse(
        Long userId,
        String email,
        String name,
        Role role,
        String companyKey
) {
    public static LoginResponse of(User user) {
        return new LoginResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getRole(),
                user.getCompany().getJoinKey()
        );
    }
}
