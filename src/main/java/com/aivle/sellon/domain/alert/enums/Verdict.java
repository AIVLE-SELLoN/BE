package com.aivle.sellon.domain.alert.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum Verdict {
    NORMAL("정상"),
    BIASED("편중형"),
    GLOBAL("전역형"),
    TENTATIVE_GLOBAL("잠정 전역형"),
    INDETERMINATE("구분불가");

    private final String jsonValue;

    Verdict(String jsonValue) {
        this.jsonValue = jsonValue;
    }

    @JsonCreator
    public static Verdict from(String value) {
        return Arrays.stream(values())
                .filter(verdict -> verdict.jsonValue.equals(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown verdict: " + value));
    }

    @JsonValue
    public String getJsonValue() {
        return jsonValue;
    }
}
