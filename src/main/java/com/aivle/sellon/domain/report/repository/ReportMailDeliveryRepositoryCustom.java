package com.aivle.sellon.domain.report.repository;

import java.time.LocalDateTime;
import java.util.List;

public interface ReportMailDeliveryRepositoryCustom {

    /**
     * 발송 시각이 도래했고 아직 시도 횟수가 남은 예약의 ID 목록.
     * 건별로 트랜잭션을 나눠 처리하기 위해 엔티티가 아니라 ID만 가져온다.
     */
    List<Long> findDueIds(LocalDateTime now, int limit);

    /** 해당 리포트에 이미 예약된 수신자 이메일. 큐 재전달 시 중복 예약을 막는 데 쓴다. */
    List<String> findScheduledEmails(Long reportId);
}
