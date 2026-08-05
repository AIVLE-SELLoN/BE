package com.aivle.sellon.domain.mypage.exception;

import com.aivle.sellon.global.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class FileSizeExceededException extends ApiException {
    private static final String MESSAGE = "파일 크기는 5MB 이하만 업로드할 수 있습니다.";

    public FileSizeExceededException() {
        super(HttpStatus.BAD_REQUEST, MESSAGE);
    }
}
