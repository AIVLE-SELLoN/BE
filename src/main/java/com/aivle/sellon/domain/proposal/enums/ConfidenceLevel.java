package com.aivle.sellon.domain.proposal.enums;

import java.util.Arrays;

// mq_events.md §9 — recommendation.recommendation_confidence(높음/중간/낮음)에 대응.
// null일 수 있다(recommendation 자체가 null이거나 확신도 산출 실패 시).
public enum ConfidenceLevel {
    HIGH("높음"),
    MEDIUM("중간"),
    LOW("낮음");

    private final String koreanValue;

    ConfidenceLevel(String koreanValue) {
        this.koreanValue = koreanValue;
    }

    public static ConfidenceLevel fromKorean(String koreanValue) {
        if (koreanValue == null) return null;
        return Arrays.stream(values())
                .filter(v -> v.koreanValue.equals(koreanValue))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("알 수 없는 confidence 값: " + koreanValue));
    }
}
