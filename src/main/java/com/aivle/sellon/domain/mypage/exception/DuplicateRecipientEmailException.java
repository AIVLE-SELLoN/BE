package com.aivle.sellon.domain.mypage.exception;

import com.aivle.sellon.global.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class DuplicateRecipientEmailException extends ApiException {
    private static final String MESSAGE = "중복된 수신자 이메일이 있습니다.";

    public DuplicateRecipientEmailException() {
        super(HttpStatus.BAD_REQUEST, MESSAGE);
    }
}
