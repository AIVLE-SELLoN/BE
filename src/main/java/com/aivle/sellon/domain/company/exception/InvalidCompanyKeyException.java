package com.aivle.sellon.domain.company.exception;

import com.aivle.sellon.global.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class InvalidCompanyKeyException extends ApiException {
    private static final String MESSAGE = "유효하지 않은 회사 키입니다.";

    public InvalidCompanyKeyException() {
        super(HttpStatus.BAD_REQUEST, MESSAGE);
    }
}
