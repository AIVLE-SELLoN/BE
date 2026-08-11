package com.aivle.sellon.domain.guideline.exception;

import com.aivle.sellon.global.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class GuidelineMailSendFailedException extends ApiException {
    private static final String MESSAGE = "메일 발송에 실패했습니다. 잠시 후 다시 시도해주세요.";

    public GuidelineMailSendFailedException() {
        super(HttpStatus.INTERNAL_SERVER_ERROR, MESSAGE);
    }
}
