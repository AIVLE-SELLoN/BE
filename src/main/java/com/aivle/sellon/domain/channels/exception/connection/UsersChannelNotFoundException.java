package com.aivle.sellon.domain.channels.exception.connection;

import com.aivle.sellon.global.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class UsersChannelNotFoundException extends ApiException {
    private static final String MESSAGE = "존재하지 않는 채널 연동입니다.";

    public UsersChannelNotFoundException() {
        super(HttpStatus.NOT_FOUND, MESSAGE);
    }
}
