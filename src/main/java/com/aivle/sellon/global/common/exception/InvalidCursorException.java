package com.aivle.sellon.global.common.exception;

import org.springframework.http.HttpStatus;

public class InvalidCursorException extends ApiException {
    private static final String MESSAGE = "유효하지 않은 커서 값입니다.";

    public InvalidCursorException() {
        super(HttpStatus.BAD_REQUEST, MESSAGE);
    }
}
