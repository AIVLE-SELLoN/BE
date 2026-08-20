package com.aivle.sellon.domain.report.exception;

import com.aivle.sellon.global.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class ReportNotFoundException extends ApiException {
    private static final String MESSAGE = "존재하지 않는 리포트입니다.";

    public ReportNotFoundException() {
        super(HttpStatus.NOT_FOUND, MESSAGE);
    }
}
