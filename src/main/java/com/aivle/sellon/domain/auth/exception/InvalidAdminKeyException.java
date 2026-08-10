package com.aivle.sellon.domain.auth.exception;

import com.aivle.sellon.global.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class InvalidAdminKeyException extends ApiException {
    private static final String MESSAGE = "관리자 등록 키가 올바르지 않습니다.";

    public InvalidAdminKeyException() {
        super(HttpStatus.FORBIDDEN, MESSAGE);
    }
}
