package com.aivle.sellon.domain.alert.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum StatsSource {
    CS("cs"),
    REVIEW("review");

    private final String jsonValue;

    StatsSource(String jsonValue) {
        this.jsonValue = jsonValue;
    }

    @JsonCreator
    public static StatsSource from(String value) {
        return Arrays.stream(values())
                .filter(source -> source.jsonValue.equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown stats source: " + value));
    }

    @JsonValue
    public String getJsonValue() {
        return jsonValue;
    }
}
