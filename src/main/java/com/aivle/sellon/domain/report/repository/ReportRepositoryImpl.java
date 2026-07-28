package com.aivle.sellon.domain.report.repository;

import com.aivle.sellon.domain.report.entity.QReport;
import com.aivle.sellon.domain.report.entity.Report;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class ReportRepositoryImpl implements ReportRepositoryCustom {

    private static final QReport report = QReport.report;

    private final JPAQueryFactory queryFactory;

    @Override
    public List<Report> findAllByCompanyIdWithCursor(Long companyId, Long cursor, int limit) {
        return queryFactory
                .selectFrom(report)
                .where(
                        report.company.id.eq(companyId),
                        cursorLessThan(cursor)
                )
                .orderBy(report.id.desc())
                .limit(limit)
                .fetch();
    }

    private BooleanExpression cursorLessThan(Long cursor) {
        return cursor != null ? report.id.lt(cursor) : null;
    }
}
