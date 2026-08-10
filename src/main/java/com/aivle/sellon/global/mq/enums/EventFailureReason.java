package com.aivle.sellon.global.mq.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 재시도해도 결과가 달라지지 않는 실패 사유. DLQ 재처리 시 원인 분류에 쓴다.
 * 여기 없는 실패(DB 커넥션 등 일시적 장애)는 그대로 던져서 x-delivery-limit(5회)만큼 재시도한다.
 */
@Getter
@RequiredArgsConstructor
public enum EventFailureReason {

    MALFORMED_PAYLOAD("payload를 읽을 수 없거나 필수 필드가 비어 있음 - 발행부 수정 필요"),
    UNKNOWN_COMPANY("company_id가 없거나 존재하지 않는 회사 - 회사 등록 또는 발행부 수정 필요");

    private final String description;
}
