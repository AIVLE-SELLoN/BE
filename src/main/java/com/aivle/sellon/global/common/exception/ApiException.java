package com.aivle.sellon.global.common.exception;

import org.springframework.http.HttpStatus;

public class ApiException extends RuntimeException{
    public final HttpStatus httpStatus;

    public ApiException(HttpStatus httpStatus , String message) {
        super(message);
        this.httpStatus = httpStatus;
    }
}
