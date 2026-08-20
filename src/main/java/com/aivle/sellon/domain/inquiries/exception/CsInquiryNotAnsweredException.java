package com.aivle.sellon.domain.inquiries.exception;

import com.aivle.sellon.global.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class CsInquiryNotAnsweredException extends ApiException {
    private static final String MESSAGE = "아직 답변이 등록되지 않은 문의입니다.";

    public CsInquiryNotAnsweredException() {
        super(HttpStatus.BAD_REQUEST, MESSAGE);
    }
}
