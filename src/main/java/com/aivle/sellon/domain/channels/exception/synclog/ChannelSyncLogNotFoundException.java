package com.aivle.sellon.domain.channels.exception.synclog;

import com.aivle.sellon.global.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class ChannelSyncLogNotFoundException extends ApiException {
    private static final String MESSAGE = "존재하지 않는 동기화 이력입니다.";

    public ChannelSyncLogNotFoundException() {
        super(HttpStatus.NOT_FOUND, MESSAGE);
    }
}
