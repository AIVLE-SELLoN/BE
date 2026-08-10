package com.aivle.sellon.domain.proposal.enums;

public enum HitlStatus {
    PENDING("대기"),
    APPROVED("승인"),
    EDITED_APPROVED("수정후승인"),
    REJECTED("반려");

    private final String koreanValue;

    HitlStatus(String koreanValue) {
        this.koreanValue = koreanValue;
    }

    public String koreanValue() {
        return koreanValue;
    }
}
