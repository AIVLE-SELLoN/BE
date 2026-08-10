package com.aivle.sellon.domain.proposal.enums;

// mq_events.md §9 — 매핑 필요 없는 enum(값 자체가 영문). Jackson 기본 name 매칭으로 바인딩된다.
public enum Channel {
    COUPANG,
    NAVER,
    ZIGZAG,
    ALL
}
