package com.aivle.sellon.domain.proposal.enums;

import java.util.Arrays;

public enum ConfidenceLevel {
    HIGH("높음"),
    MEDIUM("중간"),
    LOW("낮음");

    private final String koreanValue;

    ConfidenceLevel(String koreanValue) {
        this.koreanValue = koreanValue;
    }

    public String koreanValue() {
        return koreanValue;
    }

    public static ConfidenceLevel fromKorean(String koreanValue) {
        if (koreanValue == null) return null;
        return Arrays.stream(values())
                .filter(v -> v.koreanValue.equals(koreanValue))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("알 수 없는 confidence 값: " + koreanValue));
    }
}
