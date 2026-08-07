package com.aivle.sellon.domain.mypage.exception;

import com.aivle.sellon.global.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class CompanyKeyNotIssuedException extends ApiException {
    private static final String MESSAGE = "식별자 키가 아직 발급되지 않았습니다.";

    public CompanyKeyNotIssuedException() {
        super(HttpStatus.CONFLICT, MESSAGE);
    }
}
