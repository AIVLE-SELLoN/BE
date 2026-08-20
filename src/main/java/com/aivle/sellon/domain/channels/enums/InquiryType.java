package com.aivle.sellon.domain.channels.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** CS 문의 속성(aspect) 유형 - Agent1 분류 결과 6종에 대응, JSON 직렬화는 한글 라벨 그대로 사용. */
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
