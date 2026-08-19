package com.aivle.sellon.domain.channels.exception.productmapping;

import com.aivle.sellon.global.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class ChannelProductNotFoundException extends ApiException {
    private static final String MESSAGE = "존재하지 않는 상품 매핑입니다.";

    public ChannelProductNotFoundException() {
        super(HttpStatus.NOT_FOUND, MESSAGE);
    }
}
