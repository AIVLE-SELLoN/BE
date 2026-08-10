package com.aivle.sellon.domain.guideline.repository;

import com.aivle.sellon.domain.guideline.entity.GuidelineSummary;
import com.aivle.sellon.domain.guideline.entity.QGuidelineSummary;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

@RequiredArgsConstructor
public class GuidelineSummaryRepositoryImpl implements GuidelineSummaryRepositoryCustom {

    private static final QGuidelineSummary summary = QGuidelineSummary.guidelineSummary;

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
}
