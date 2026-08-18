package com.aivle.sellon.domain.channels.service.connection;

import org.springframework.stereotype.Component;

/**
 * 네이버 커머스 API OAuth(client_credentials) 흐름을 흉내내는 Mock 클라이언트.
 * TODO: 실제 네이버 로그인/토큰 발급(https://api.commerce.naver.com/external/v1/oauth2/token) 연동으로 교체.
 */
@Component
public class MockNaverOAuthClient {

    public String buildAuthorizationUrl(String state) {
        // TODO: 실제 네이버 로그인 페이지 URL로 교체
        return "https://mock-naver-oauth.local/authorize?state=" + state;
    }

    public TokenResult exchangeToken(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        // Mock: 항상 성공 처리, accountId는 임시로 code 기반 생성
        return new TokenResult("mock-access-token", "naver-account-" + code);
    }

    public record TokenResult(String accessToken, String accountId) {
    }
}
