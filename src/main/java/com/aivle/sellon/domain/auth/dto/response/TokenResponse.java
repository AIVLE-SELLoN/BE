package com.aivle.sellon.domain.auth.dto.response;

public record TokenResponse(
        String accessToken,
        String refreshToken
) {
}
