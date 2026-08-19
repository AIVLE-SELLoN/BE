package com.aivle.sellon.domain.channels.exception.productmapping;

import com.aivle.sellon.global.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class ProductMappingReviewItemNotFoundException extends ApiException {
    private static final String MESSAGE = "존재하지 않는 매핑 검토 항목입니다.";

    public ProductMappingReviewItemNotFoundException() {
        super(HttpStatus.NOT_FOUND, MESSAGE);
    }
}
