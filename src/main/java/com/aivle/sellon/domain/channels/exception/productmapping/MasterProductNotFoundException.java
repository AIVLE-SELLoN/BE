package com.aivle.sellon.domain.channels.exception.productmapping;

import com.aivle.sellon.global.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class MasterProductNotFoundException extends ApiException {
    private static final String MESSAGE = "존재하지 않는 마스터 상품입니다.";

    public MasterProductNotFoundException() {
        super(HttpStatus.NOT_FOUND, MESSAGE);
    }
}
