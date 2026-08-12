package com.aivle.sellon.domain.alert.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum RecommendedAction {
    GENERATE_RECOMMENDATION("개선안 생성"),
    CHANNEL_OPERATION_CHECK("채널 운영 요소 점검 권장"),
    LOGISTICS_CHECK("물류 점검 권장"),
    OPERATION_CHECK("운영 점검 권장"),
    PRODUCT_CHECK("상품 자체 점검 권장"),
    SCOPE_UNDETERMINED("편중·전역 구분 불가(채널 표본 부족)"),
    OTHER_TYPE_CHECK("기타 유형");

    private final String jsonValue;

    RecommendedAction(String jsonValue) {
        this.jsonValue = jsonValue;
    }

    @JsonCreator
    public static RecommendedAction from(String value) {
        return Arrays.stream(values())
                .filter(action -> action.jsonValue.equals(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown recommended action: " + value));
    }

    @JsonValue
    public String getJsonValue() {
        return jsonValue;
    }
}
