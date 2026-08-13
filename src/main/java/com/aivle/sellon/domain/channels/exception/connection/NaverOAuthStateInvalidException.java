package com.aivle.sellon.domain.channels.exception.connection;

import com.aivle.sellon.global.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class NaverOAuthStateInvalidException extends ApiException {
    private static final String MESSAGE = "유효하지 않은 인증 상태입니다.";

    public NaverOAuthStateInvalidException() {
        super(HttpStatus.BAD_REQUEST, MESSAGE);
    }
}
