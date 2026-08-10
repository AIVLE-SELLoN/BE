package com.aivle.sellon.domain.mypage.exception;

import com.aivle.sellon.global.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class InvalidSendDayException extends ApiException {
    private static final String MESSAGE = "발송 일자는 1일부터 28일 사이로 설정해주세요.";

    public InvalidSendDayException() {
        super(HttpStatus.BAD_REQUEST, MESSAGE);
    }
}
