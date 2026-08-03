package com.aivle.sellon.domain.report.repository;

import com.aivle.sellon.domain.report.entity.QReport;
import com.aivle.sellon.domain.report.entity.Report;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class ReportRepositoryImpl implements ReportRepositoryCustom {

    private static final QReport report = QReport.report;

    private final JPAQueryFactory queryFactory;

    @Override
    public List<Report> findAllByCompanyId(Long companyId) {
        return queryFactory
                .selectFrom(report)
                .where(report.company.id.eq(companyId))
                .orderBy(report.generatedAt.desc())
                .fetch();
    }
}
