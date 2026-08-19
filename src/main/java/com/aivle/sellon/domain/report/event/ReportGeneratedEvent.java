package com.aivle.sellon.domain.report.event;

/**
 * 월간 보고서 저장이 커밋된 뒤 완료 메일 발송을 예약하기 위한 이벤트.
 * 리스너가 트랜잭션 밖에서 처리하므로 지연 로딩되는 엔티티 대신 식별자만 담는다.
 */
public record ReportGeneratedEvent(
        Long reportId,
        Long companyId,
        String reportMonth
) {
}
