package com.aivle.sellon.domain.inquiries.exception;

import com.aivle.sellon.global.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class CsInquiryAccessDeniedException extends ApiException {
    private static final String MESSAGE = "본인이 작성한 문의만 접근할 수 있습니다.";

    public CsInquiryAccessDeniedException() {
        super(HttpStatus.FORBIDDEN, MESSAGE);
    }
}
