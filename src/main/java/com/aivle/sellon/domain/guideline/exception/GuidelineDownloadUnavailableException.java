package com.aivle.sellon.domain.guideline.exception;

import com.aivle.sellon.global.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class GuidelineDownloadUnavailableException extends ApiException {
    private static final String MESSAGE = "다운로드 가능한 파일이 없습니다. 생성에 실패했거나 파일 보관 기한(7일)이 지났습니다.";

    public GuidelineDownloadUnavailableException() {
        super(HttpStatus.BAD_REQUEST, MESSAGE);
    }
}
