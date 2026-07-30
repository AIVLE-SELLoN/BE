package com.aivle.sellon.domain.mypage.exception;

import com.aivle.sellon.global.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class NotCompanyOwnerException extends ApiException {
    private static final String MESSAGE = "루트 계정만 이용할 수 있습니다.";

    public NotCompanyOwnerException() {
        super(HttpStatus.FORBIDDEN, MESSAGE);
    }
}
