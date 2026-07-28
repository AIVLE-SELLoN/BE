package com.aivle.sellon.global.security.jwt.exception;

import com.aivle.sellon.global.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class ExpiredTokenException extends ApiException {
    private static final String MESSAGE = "만료된 토큰입니다.";

    public ExpiredTokenException() {
        super(HttpStatus.UNAUTHORIZED, MESSAGE);
    }
}
