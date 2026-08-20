package com.aivle.sellon.domain.guideline.repository;

import com.aivle.sellon.domain.guideline.entity.GuidelineSummary;
import com.aivle.sellon.domain.guideline.entity.QGuideline;
import com.aivle.sellon.domain.guideline.entity.QGuidelineSummary;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
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

    /**
     * 최신순은 guideline.createdDate 기준이다 — id는 여러 인스턴스가 동시에 큐를 소비할 때
     * INSERT 순서(id 채번)와 실제 생성 시각이 어긋날 수 있어 정렬 기준으로 쓰지 않는다.
     */
    @Override
    public List<GuidelineSummary> findAllByCompanyId(Long companyId, String query, LocalDateTime cursorCreatedAt, int limit) {
        return queryFactory
                .selectFrom(summary)
                .join(summary.guideline, guideline).fetchJoin()
                .where(
                        guideline.company.id.eq(companyId),
                        searchCondition(query),
                        cursorCondition(cursorCreatedAt)
                )
                .orderBy(guideline.createdDate.desc())
                .limit(limit)
                .fetch();
    }

    private BooleanExpression cursorCondition(LocalDateTime cursorCreatedAt) {
        return cursorCreatedAt != null ? guideline.createdDate.lt(cursorCreatedAt) : null;
    }

    private BooleanExpression searchCondition(String query) {
        if (query == null || query.isBlank())
            return null;

        return guideline.guidelineId.containsIgnoreCase(query)
                .or(summary.title.containsIgnoreCase(query))
                .or(summary.productName.containsIgnoreCase(query));
    }
}
