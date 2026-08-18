package com.aivle.sellon.domain.report.repository;

import com.aivle.sellon.domain.report.entity.Report;

import java.util.List;
import java.util.Optional;

public interface ReportRepositoryCustom {
    List<Report> findAllByCompanyId(Long companyId);

    Optional<Report> findByCompanyIdAndReportId(Long companyId, String reportId);

    Optional<Report> findLatestByCompanyId(Long companyId);

    /**
     * 리포트 행을 락과 함께 가져온다. 같은 report_id에 대한 메일 예약 생성이
     * 여러 인스턴스에서 동시에 들어와도 한 번에 하나씩만 처리되게 하기 위함이다.
     * <p>
     * SKIP LOCKED를 쓰지 않는다 — 뒤에 온 트랜잭션은 포기하지 않고 대기했다가,
     * 앞선 트랜잭션이 커밋한 최신 예약 상태를 다시 읽어야 신규 수신자를 놓치지 않는다.
     */
    Optional<Report> findByIdForUpdate(Long id);
}
