package com.aivle.sellon.domain.proposal.enums;

import java.util.Arrays;

public enum MainAspect {
    COLOR("색상"),
    SIZE("사이즈"),
    MATERIAL("소재"),
    DAMAGE("파손"),
    MISDELIVERY("오배송"),
    ETC("기타");

    private final String koreanValue;

    MainAspect(String koreanValue) {
        this.koreanValue = koreanValue;
    }

    public String koreanValue() {
        return koreanValue;
    }

    public static MainAspect fromKorean(String koreanValue) {
        return Arrays.stream(values())
                .filter(v -> v.koreanValue.equals(koreanValue))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("알 수 없는 main_aspect 값: " + koreanValue));
    }
}
