package com.aivle.sellon.domain.guideline.repository;

import com.aivle.sellon.domain.guideline.entity.Guideline;
import com.aivle.sellon.domain.guideline.entity.QGuideline;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public class GuidelineRepositoryImpl implements GuidelineRepositoryCustom {

    private static final QGuideline guideline = QGuideline.guideline;

    private final JPAQueryFactory queryFactory;

    @Override
    public Optional<Guideline> findByCompanyIdAndGuidelineId(Long companyId, String guidelineId) {
        return Optional.ofNullable(
                queryFactory
                        .selectFrom(guideline)
                        .where(
                                guideline.company.id.eq(companyId),
                                guideline.guidelineId.eq(guidelineId)
                        )
                        .fetchOne()
        );
    }

    @Override
    public List<Guideline> findAllWithFileByCompanyId(Long companyId, String query, Long cursorId, int limit) {
        return queryFactory
                .selectFrom(guideline)
                .where(
                        guideline.company.id.eq(companyId),
                        guideline.pdfS3Meta.s3FullKey.isNotNull(),
                        searchCondition(query),
                        cursorCondition(cursorId)
                )
                .orderBy(guideline.id.desc())
                .limit(limit)
                .fetch();
    }

    @Override
    public Optional<Guideline> findByIdForUpdate(Long id) {
        return Optional.ofNullable(
                queryFactory
                        .selectFrom(guideline)
                        .where(guideline.id.eq(id))
                        .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                        .fetchOne()
        );
    }

    private BooleanExpression cursorCondition(Long cursorId) {
        return cursorId != null ? guideline.id.lt(cursorId) : null;
    }

    private BooleanExpression searchCondition(String query) {
        if (query == null || query.isBlank())
            return null;

        return guideline.guidelineId.containsIgnoreCase(query)
                .or(guideline.pdfS3Meta.originalFileName.containsIgnoreCase(query));
    }
}
