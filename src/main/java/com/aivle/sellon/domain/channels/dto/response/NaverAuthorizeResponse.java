package com.aivle.sellon.domain.channels.dto.response;

public record NaverAuthorizeResponse(
        String authorizationUrl,
        String state
) {
}
