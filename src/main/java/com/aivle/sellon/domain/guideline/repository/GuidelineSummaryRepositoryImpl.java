package com.aivle.sellon.domain.guideline.repository;

import com.aivle.sellon.domain.guideline.entity.GuidelineSummary;
import com.aivle.sellon.domain.guideline.entity.QGuideline;
import com.aivle.sellon.domain.guideline.entity.QGuidelineSummary;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public class GuidelineSummaryRepositoryImpl implements GuidelineSummaryRepositoryCustom {

    private static final QGuidelineSummary summary = QGuidelineSummary.guidelineSummary;
    private static final QGuideline guideline = QGuideline.guideline;

    private final JPAQueryFactory queryFactory;

    @Override
    public Optional<GuidelineSummary> findByGuidelineId(Long guidelineId) {
        return Optional.ofNullable(
                queryFactory
                        .selectFrom(summary)
                        .where(summary.guideline.id.eq(guidelineId))
                        .fetchOne()
        );
    }

    @Override
    public List<GuidelineSummary> findAllByCompanyId(Long companyId, Long cursorId, int limit) {
        return queryFactory
                .selectFrom(summary)
                .join(summary.guideline, guideline).fetchJoin()
                .where(
                        guideline.company.id.eq(companyId),
                        cursorCondition(cursorId)
                )
                .orderBy(summary.id.desc())
                .limit(limit)
                .fetch();
    }

    private BooleanExpression cursorCondition(Long cursorId) {
        return cursorId != null ? summary.id.lt(cursorId) : null;
    }
}
