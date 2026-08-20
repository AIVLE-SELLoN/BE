package com.aivle.sellon.domain.company.exception;

import com.aivle.sellon.global.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class CompanyKeyGenerationException extends ApiException {
    private static final String MESSAGE = "회사 키 생성에 실패했습니다. 다시 시도해주세요.";

    public CompanyKeyGenerationException() {
        super(HttpStatus.INTERNAL_SERVER_ERROR, MESSAGE);
    }
}
