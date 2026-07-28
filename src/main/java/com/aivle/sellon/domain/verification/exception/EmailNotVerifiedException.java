package com.aivle.sellon.domain.verification.exception;

import com.aivle.sellon.global.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class EmailNotVerifiedException extends ApiException {
    private static final String MESSAGE = "이메일 인증이 필요합니다.";

    public EmailNotVerifiedException() {
        super(HttpStatus.BAD_REQUEST, MESSAGE);
    }
}
