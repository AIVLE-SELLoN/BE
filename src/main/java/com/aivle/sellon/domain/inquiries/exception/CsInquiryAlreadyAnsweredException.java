package com.aivle.sellon.domain.inquiries.exception;

import com.aivle.sellon.global.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class CsInquiryAlreadyAnsweredException extends ApiException {
    private static final String MESSAGE = "이미 답변이 등록된 문의는 수정/삭제할 수 없습니다.";

    public CsInquiryAlreadyAnsweredException() {
        super(HttpStatus.BAD_REQUEST, MESSAGE);
    }
}
