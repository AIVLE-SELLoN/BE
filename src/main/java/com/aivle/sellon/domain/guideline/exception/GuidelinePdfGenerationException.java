package com.aivle.sellon.domain.guideline.exception;

import com.aivle.sellon.global.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class GuidelinePdfGenerationException extends ApiException {
    private static final String MESSAGE = "가이드라인 파일 재생성에 실패했습니다.";

    public GuidelinePdfGenerationException() {
        super(HttpStatus.INTERNAL_SERVER_ERROR, MESSAGE);
    }
}
