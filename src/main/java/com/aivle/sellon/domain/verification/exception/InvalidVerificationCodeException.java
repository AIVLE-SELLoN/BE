package com.aivle.sellon.domain.verification.exception;

import com.aivle.sellon.global.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class InvalidVerificationCodeException extends ApiException {
    private static final String MESSAGE = "인증번호가 일치하지 않습니다.";

    public InvalidVerificationCodeException() {
        super(HttpStatus.BAD_REQUEST, MESSAGE);
    }
}
