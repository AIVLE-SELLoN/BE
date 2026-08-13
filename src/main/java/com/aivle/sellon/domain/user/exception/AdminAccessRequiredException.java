package com.aivle.sellon.domain.user.exception;

import com.aivle.sellon.global.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class AdminAccessRequiredException extends ApiException {
    private static final String MESSAGE = "운영자만 접근할 수 있습니다.";

    public AdminAccessRequiredException() {
        super(HttpStatus.FORBIDDEN, MESSAGE);
    }
}
