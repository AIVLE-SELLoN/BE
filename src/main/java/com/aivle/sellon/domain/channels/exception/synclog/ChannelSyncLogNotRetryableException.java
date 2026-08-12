package com.aivle.sellon.domain.channels.exception.synclog;

import com.aivle.sellon.global.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class ChannelSyncLogNotRetryableException extends ApiException {
    private static final String MESSAGE = "재시도할 수 없는 동기화 이력입니다 (실패 건이 아니거나 원본 메시지가 저장되지 않았어요).";

    public ChannelSyncLogNotRetryableException() {
        super(HttpStatus.BAD_REQUEST, MESSAGE);
    }
}
