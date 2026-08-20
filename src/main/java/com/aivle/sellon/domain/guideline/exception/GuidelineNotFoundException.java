package com.aivle.sellon.domain.guideline.exception;

import com.aivle.sellon.global.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class GuidelineNotFoundException extends ApiException {
    private static final String MESSAGE = "존재하지 않는 가이드라인입니다.";

    public GuidelineNotFoundException() {
        super(HttpStatus.NOT_FOUND, MESSAGE);
    }
}
