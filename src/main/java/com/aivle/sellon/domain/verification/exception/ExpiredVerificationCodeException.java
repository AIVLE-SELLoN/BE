package com.aivle.sellon.domain.verification.exception;

import com.aivle.sellon.global.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class ExpiredVerificationCodeException extends ApiException {
    private static final String MESSAGE = "인증번호가 만료되었거나 존재하지 않습니다. 다시 요청해주세요.";

    public ExpiredVerificationCodeException() {
        super(HttpStatus.BAD_REQUEST, MESSAGE);
    }
}
