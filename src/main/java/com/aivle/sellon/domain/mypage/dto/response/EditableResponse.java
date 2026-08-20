package com.aivle.sellon.domain.mypage.dto.response;

import com.aivle.sellon.domain.user.enums.Role;

public record EditableResponse(
        boolean email,
        boolean brandName
) {
    public static EditableResponse of(Role role) {
        boolean isRoot = role == Role.ROOT;
        return new EditableResponse(!isRoot, isRoot);
    }
}
