package com.aivle.sellon.global.file.exception;

import com.aivle.sellon.global.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class InvalidFileException extends ApiException {
    private static final String MESSAGE = "파일 업로드에 실패했습니다.";

    public InvalidFileException() {
        super(HttpStatus.BAD_REQUEST, MESSAGE);
    }
}
