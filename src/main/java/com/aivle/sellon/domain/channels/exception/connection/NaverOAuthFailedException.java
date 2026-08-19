package com.aivle.sellon.domain.channels.exception.connection;

import com.aivle.sellon.global.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class NaverOAuthFailedException extends ApiException {
    private static final String MESSAGE = "네이버 인증에 실패했습니다.";

    public NaverOAuthFailedException() {
        super(HttpStatus.BAD_REQUEST, MESSAGE);
    }
}
