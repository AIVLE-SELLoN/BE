package com.aivle.sellon.domain.mypage.exception;

import com.aivle.sellon.global.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class FieldNotEditableException extends ApiException {
    private static final String MESSAGE = "해당 항목은 수정할 수 없습니다.";

    public FieldNotEditableException() {
        super(HttpStatus.FORBIDDEN, MESSAGE);
    }
}
