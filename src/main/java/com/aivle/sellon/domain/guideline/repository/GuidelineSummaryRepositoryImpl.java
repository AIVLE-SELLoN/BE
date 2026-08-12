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
    public List<GuidelineSummary> findAllByCompanyId(Long companyId, String query, Long cursorId, int limit) {
        return queryFactory
                .selectFrom(summary)
                .join(summary.guideline, guideline).fetchJoin()
                .where(
                        guideline.company.id.eq(companyId),
                        searchCondition(query),
                        cursorCondition(cursorId)
                )
                .orderBy(summary.id.desc())
                .limit(limit)
                .fetch();
    }

    private BooleanExpression cursorCondition(Long cursorId) {
        return cursorId != null ? summary.id.lt(cursorId) : null;
    }

    private BooleanExpression searchCondition(String query) {
        if (query == null || query.isBlank())
            return null;

        return guideline.guidelineId.containsIgnoreCase(query)
                .or(summary.title.containsIgnoreCase(query))
                .or(summary.productName.containsIgnoreCase(query));
    }
}
