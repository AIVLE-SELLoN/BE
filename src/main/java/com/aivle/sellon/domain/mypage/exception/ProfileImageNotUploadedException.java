package com.aivle.sellon.domain.mypage.exception;

import com.aivle.sellon.global.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class ProfileImageNotUploadedException extends ApiException {
    private static final String MESSAGE = "업로드된 이미지를 찾을 수 없습니다.";

    public ProfileImageNotUploadedException() {
        super(HttpStatus.NOT_FOUND, MESSAGE);
    }
}
