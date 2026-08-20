package com.aivle.sellon.domain.auth.exception;

import com.aivle.sellon.global.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class AccountLockedException extends ApiException {
    private static final String MESSAGE_FORMAT = "로그인 시도 횟수를 초과했습니다. %d초 후 다시 시도해주세요.";

    public AccountLockedException(long remainingSeconds) {
        super(HttpStatus.LOCKED, MESSAGE_FORMAT.formatted(remainingSeconds));
    }
}
