package com.aivle.sellon.domain.auth.exception;

import com.aivle.sellon.global.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class InvalidCredentialsException extends ApiException {
    private static final String MESSAGE = "이메일 또는 비밀번호가 올바르지 않습니다.";

    public InvalidCredentialsException() {
        super(HttpStatus.UNAUTHORIZED, MESSAGE);
    }
}
