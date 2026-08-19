package com.aivle.sellon.domain.channels.dto.response;

// 네이버 OAuth 1단계 응답 - 302 리다이렉트 대신 JSON으로 authorizationUrl/state를 내려준다.
// (fetch는 redirect:"manual" 응답의 Location 헤더를 읽을 수 없어 JWT bearer 인증 구조와 302 방식이 호환되지 않음)
public record NaverAuthorizeResponse(String authorizationUrl, String state) {
}
