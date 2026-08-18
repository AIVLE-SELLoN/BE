package com.aivle.sellon.domain.inquiries.exception;

import com.aivle.sellon.global.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class CsInquiryNotFoundException extends ApiException {
    private static final String MESSAGE = "존재하지 않는 문의입니다.";

    public CsInquiryNotFoundException() {
        super(HttpStatus.NOT_FOUND, MESSAGE);
    }
}
