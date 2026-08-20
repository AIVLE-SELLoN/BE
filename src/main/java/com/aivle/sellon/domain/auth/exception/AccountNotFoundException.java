package com.aivle.sellon.domain.auth.exception;

import com.aivle.sellon.global.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class AccountNotFoundException extends ApiException {
    private static final String MESSAGE = "일치하는 계정을 찾을 수 없어요. 입력하신 정보를 다시 확인해주세요.";

    public AccountNotFoundException() {
        super(HttpStatus.NOT_FOUND, MESSAGE);
    }
}
