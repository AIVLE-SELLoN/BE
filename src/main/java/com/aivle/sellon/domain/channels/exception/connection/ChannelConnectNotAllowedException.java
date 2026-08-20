package com.aivle.sellon.domain.channels.exception.connection;

import com.aivle.sellon.global.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class ChannelConnectNotAllowedException extends ApiException {
    private static final String MESSAGE = "채널 연동은 회사 루트 계정만 가능합니다.";

    public ChannelConnectNotAllowedException() {
        super(HttpStatus.FORBIDDEN, MESSAGE);
    }
}
