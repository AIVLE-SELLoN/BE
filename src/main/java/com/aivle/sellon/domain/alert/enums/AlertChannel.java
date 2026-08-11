package com.aivle.sellon.domain.alert.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum AlertChannel {
    // 탐지 알림은 전체 채널 집계(ALL)를 포함하므로 채널 연동 도메인의 enum과 공유하지 않는다.
    COUPANG("쿠팡"),
    NAVER("네이버"),
    ZIGZAG("지그재그"),
    ALL("전체");

    private final String jsonValue;

    AlertChannel(String jsonValue) {
        this.jsonValue = jsonValue;
    }

    @JsonCreator
    public static AlertChannel from(String value) {
        return Arrays.stream(values())
                .filter(channel -> channel.jsonValue.equals(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown alert channel: " + value));
    }

    @JsonValue
    public String getJsonValue() {
        return jsonValue;
    }
}
