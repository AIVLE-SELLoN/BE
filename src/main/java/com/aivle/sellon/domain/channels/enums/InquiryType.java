package com.aivle.sellon.domain.channels.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * CS 문의 속성(aspect) 유형. Agent1 분류 결과의 6종 aspect 값(색상/사이즈/소재/파손/오배송/기타)에 대응.
 * JSON 직렬화/역직렬화 시에는 한글 라벨을 그대로 사용해 기존 API 응답 포맷을 유지한다.
 */
public enum InquiryType {
    COLOR("색상"),
    SIZE("사이즈"),
    MATERIAL("소재"),
    DAMAGE("파손"),
    WRONG_DELIVERY("오배송"),
    ETC("기타");

    private final String label;

    InquiryType(String label) {
        this.label = label;
    }

    @JsonValue
    public String getLabel() {
        return label;
    }

    @JsonCreator
    public static InquiryType fromLabel(String label) {
        for (InquiryType type : values()) {
            if (type.label.equals(label)) {
                return type;
            }
        }
        throw new IllegalArgumentException("알 수 없는 문의 유형: " + label);
    }
}
