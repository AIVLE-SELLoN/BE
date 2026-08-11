package com.aivle.sellon.domain.alert.exception;

import com.aivle.sellon.global.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class AlertNotFoundException extends ApiException {
    private static final String MESSAGE = "존재하지 않는 알림입니다.";

    public AlertNotFoundException() {
        super(HttpStatus.NOT_FOUND, MESSAGE);
    }
}
