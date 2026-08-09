package com.aivle.sellon.domain.alert.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum DetectionConfidence {
    HIGH("높음"),
    MEDIUM("중간"),
    LOW("낮음"),
    NOT_APPLICABLE("해당없음");

    private final String jsonValue;

    DetectionConfidence(String jsonValue) {
        this.jsonValue = jsonValue;
    }

    @JsonCreator
    public static DetectionConfidence from(String value) {
        return Arrays.stream(values())
                .filter(confidence -> confidence.jsonValue.equals(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown detection confidence: " + value));
    }

    @JsonValue
    public String getJsonValue() {
        return jsonValue;
    }
}
