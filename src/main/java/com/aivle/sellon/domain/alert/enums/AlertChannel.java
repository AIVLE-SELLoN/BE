package com.aivle.sellon.domain.alert.enums;

public enum AlertChannel {
    // 탐지 알림은 전체 채널 집계(ALL)를 포함하므로 채널 연동 도메인의 enum과 공유하지 않는다.
    COUPANG,
    NAVER,
    ZIGZAG,
    ALL
}
