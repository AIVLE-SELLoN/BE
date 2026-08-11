package com.aivle.sellon.domain.guideline.exception;

import com.aivle.sellon.global.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class GuidelineNoCsRecipientException extends ApiException {
    private static final String MESSAGE = "등록된 CS 담당자가 없습니다.";

    public GuidelineNoCsRecipientException() {
        super(HttpStatus.BAD_REQUEST, MESSAGE);
    }
}
