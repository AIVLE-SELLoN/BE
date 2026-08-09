package com.aivle.sellon.domain.report.repository;

import com.aivle.sellon.domain.report.entity.QReport;
import com.aivle.sellon.domain.report.entity.Report;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public class ReportRepositoryImpl implements ReportRepositoryCustom {

    private static final QReport report = QReport.report;

    private final JPAQueryFactory queryFactory;

    @Override
    public List<Report> findAllByCompanyId(Long companyId) {
        return queryFactory
                .selectFrom(report)
                .where(report.company.id.eq(companyId))
                .orderBy(report.id.desc())
                .fetch();
    }

    @Override
    public Optional<Report> findByCompanyIdAndReportId(Long companyId, String reportId) {
        return Optional.ofNullable(
                queryFactory
                        .selectFrom(report)
                        .where(
                                report.company.id.eq(companyId),
                                report.reportId.eq(reportId)
                        )
                        .fetchOne()
        );
    }

    @Override
    public Optional<Report> findByIdForUpdate(Long id) {
        return Optional.ofNullable(
                queryFactory
                        .selectFrom(report)
                        .where(report.id.eq(id))
                        .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                        .fetchOne()
        );
    }
}
