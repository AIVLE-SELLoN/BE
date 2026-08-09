package com.aivle.sellon.domain.alert.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum Aspect {
    COLOR("색상"),
    SIZE("사이즈"),
    MATERIAL("소재"),
    DAMAGE("파손"),
    MISDELIVERY("오배송"),
    ETC("기타");

    private final String jsonValue;

    Aspect(String jsonValue) {
        this.jsonValue = jsonValue;
    }

    @JsonCreator
    public static Aspect from(String value) {
        return Arrays.stream(values())
                .filter(aspect -> aspect.jsonValue.equals(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown aspect: " + value));
    }

    @JsonValue
    public String getJsonValue() {
        return jsonValue;
    }
}
