package com.aivle.sellon.domain.proposal.exception;

import com.aivle.sellon.global.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class ProposalAcceptHistoryNotFoundException extends ApiException {
    private static final String MESSAGE = "존재하지 않는 개선안 이력입니다.";

    public ProposalAcceptHistoryNotFoundException() {
        super(HttpStatus.NOT_FOUND, MESSAGE);
    }
}
