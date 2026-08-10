package com.aivle.sellon.domain.proposal.enums;

import java.util.Arrays;

// mq_events.md §9 — JSON에는 한글 값("편중형" 등)이 실리고 상수명은 영문이다.
// Jackson 기본 enum 바인딩은 name 매칭이라 한글 값을 못 읽으므로, payload record는
// 원문 문자열로 받은 뒤 fromKorean()으로 직접 변환한다(핸들러에서 호출).
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

    public static Verdict fromKorean(String koreanValue) {
        return Arrays.stream(values())
                .filter(v -> v.koreanValue.equals(koreanValue))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("알 수 없는 verdict 값: " + koreanValue));
    }
}
