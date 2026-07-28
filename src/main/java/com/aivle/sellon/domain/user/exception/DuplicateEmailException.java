package com.aivle.sellon.domain.user.exception;

import com.aivle.sellon.global.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class DuplicateEmailException extends ApiException {
    private static final String MESSAGE = "이미 사용 중인 이메일입니다.";

    public DuplicateEmailException() {
        super(HttpStatus.CONFLICT, MESSAGE);
    }
}
