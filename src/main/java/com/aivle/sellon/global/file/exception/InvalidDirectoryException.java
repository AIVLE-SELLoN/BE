package com.aivle.sellon.global.file.exception;

import com.aivle.sellon.global.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class InvalidDirectoryException extends ApiException {
    private static final String MESSAGE = "잘못된 파일 디렉토리명 입니다.";
    public InvalidDirectoryException() {
        super(HttpStatus.BAD_REQUEST , MESSAGE);
    }
}