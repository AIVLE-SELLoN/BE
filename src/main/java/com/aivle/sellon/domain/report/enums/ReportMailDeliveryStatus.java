package com.aivle.sellon.domain.report.enums;

public enum ReportMailDeliveryStatus {

    /** 예약 시각 도래 전이거나, 실패해서 다음 날 재시도를 기다리는 중 */
    PENDING,

    /** 발송 완료 */
    SENT,

    /** 5회를 모두 실패했거나 더 시도해도 소용없어 포기 */
    GIVEN_UP
}
