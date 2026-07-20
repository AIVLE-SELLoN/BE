package com.aivle.sellon.global.file.exception;

import com.aivle.sellon.global.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class InvalidExtensionException extends ApiException {
    private static final String MESSAGE = "잘못된 형식의 파일입니다.";
    public InvalidExtensionException() {
        super(HttpStatus.BAD_REQUEST , MESSAGE);
    }
}