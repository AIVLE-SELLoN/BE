package com.aivle.sellon.domain.channels.exception.connection;

import com.aivle.sellon.global.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class ChannelKeyFormatInvalidException extends ApiException {
    private static final String MESSAGE = "채널 키 형식이 올바르지 않습니다.";

    public ChannelKeyFormatInvalidException() {
        super(HttpStatus.BAD_REQUEST, MESSAGE);
    }
}
