package com.aivle.sellon.global.security.jwt.exception;

import com.aivle.sellon.global.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class InvalidTokenException extends ApiException {
    private static final String MESSAGE = "유효하지 않은 토큰입니다.";

    public InvalidTokenException() {
        super(HttpStatus.UNAUTHORIZED, MESSAGE);
    }
}
