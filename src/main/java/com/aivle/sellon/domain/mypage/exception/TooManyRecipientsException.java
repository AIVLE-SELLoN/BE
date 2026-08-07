package com.aivle.sellon.domain.mypage.exception;

import com.aivle.sellon.global.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class TooManyRecipientsException extends ApiException {
    private static final String MESSAGE = "수신자는 최대 20명까지 등록할 수 있습니다.";

    public TooManyRecipientsException() {
        super(HttpStatus.BAD_REQUEST, MESSAGE);
    }
}
