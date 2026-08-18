package com.aivle.sellon.domain.proposal.enums;

import java.util.Arrays;

public enum Verdict {
    NORMAL("정상"),
    BIASED("편중형"),
    GLOBAL("전역형"),
    TENTATIVE_GLOBAL("잠정 전역형"),
    INDETERMINATE("구분불가");

    private final String koreanValue;

    Verdict(String koreanValue) {
        this.koreanValue = koreanValue;
    }

    public String koreanValue() {
        return koreanValue;
    }

    public static Verdict fromKorean(String koreanValue) {
        return Arrays.stream(values())
                .filter(v -> v.koreanValue.equals(koreanValue))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("알 수 없는 verdict 값: " + koreanValue));
    }
}
