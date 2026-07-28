package com.aivle.sellon.domain.auth.dto.response;

public record LoginResult(
        LoginResponse loginResponse,
        TokenResponse tokenResponse
) {
}
