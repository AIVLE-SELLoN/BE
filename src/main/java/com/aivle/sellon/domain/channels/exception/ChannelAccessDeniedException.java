package com.aivle.sellon.domain.channels.exception;

import com.aivle.sellon.global.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class ChannelAccessDeniedException extends ApiException {
    private static final String MESSAGE = "접근 권한이 없습니다.";

    public ChannelAccessDeniedException() {
        super(HttpStatus.FORBIDDEN, MESSAGE);
    }
}
