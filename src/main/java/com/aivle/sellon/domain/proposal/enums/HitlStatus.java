package com.aivle.sellon.domain.proposal.enums;

// mq_events.md §9 — 대기/승인/반려/수정후승인. koreanValue()는 feedback.recommendation.reviewed
// 발행 시 hitl_status 필드에 실릴 값(대기는 발행 대상 아님, §8)을 만들 때 쓴다.
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
