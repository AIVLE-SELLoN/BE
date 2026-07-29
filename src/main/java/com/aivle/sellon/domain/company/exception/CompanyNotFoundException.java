package com.aivle.sellon.domain.company.exception;

import com.aivle.sellon.global.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class CompanyNotFoundException extends ApiException {
    private static final String MESSAGE = "존재하지 않는 회사입니다.";

    public CompanyNotFoundException() {
        super(HttpStatus.NOT_FOUND, MESSAGE);
    }
}
