package com.aivle.sellon.domain.user.dto.response;

import com.aivle.sellon.domain.user.entity.User;
import com.aivle.sellon.domain.user.enums.Role;

public record UserResponse(
        Long userId,
        String email,
        String name,
        Role role,
        String companyKey
) {
    public static UserResponse of(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getRole(),
                user.getCompany().getJoinKey()
        );
    }
}
