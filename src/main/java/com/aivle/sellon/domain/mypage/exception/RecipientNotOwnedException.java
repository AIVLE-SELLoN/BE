package com.aivle.sellon.domain.mypage.exception;

import com.aivle.sellon.global.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class RecipientNotOwnedException extends ApiException {
    private static final String MESSAGE = "접근 권한이 없습니다.";

    public RecipientNotOwnedException() {
        super(HttpStatus.FORBIDDEN, MESSAGE);
    }
}
