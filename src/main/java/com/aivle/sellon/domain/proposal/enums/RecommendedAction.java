package com.aivle.sellon.domain.proposal.enums;

import java.util.Arrays;

public enum RecommendedAction {
    GENERATE_RECOMMENDATION("개선안 생성"),
    CHANNEL_OPERATION_CHECK("채널 운영 요소 점검 권장"),
    LOGISTICS_CHECK("물류 점검 권장"),
    OPERATION_CHECK("운영 점검 권장"),
    PRODUCT_CHECK("상품 자체 점검 권장"),
    SCOPE_UNDETERMINED("편중·전역 구분 불가(채널 표본 부족)"),
    OTHER_TYPE_CHECK("기타 유형 점검");

    private final String koreanValue;

    RecommendedAction(String koreanValue) {
        this.koreanValue = koreanValue;
    }

    public String koreanValue() {
        return koreanValue;
    }

    public static RecommendedAction fromKorean(String koreanValue) {
        return Arrays.stream(values())
                .filter(v -> v.koreanValue.equals(koreanValue))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("알 수 없는 recommended_action 값: " + koreanValue));
    }
}
