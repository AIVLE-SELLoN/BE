package com.aivle.sellon.domain.guideline.repository;

import com.aivle.sellon.domain.guideline.entity.Guideline;
import com.aivle.sellon.domain.guideline.entity.QGuideline;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
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

    /**
     * 최신순은 pdfS3Meta.createdAt(파일 생성 시각) 기준이다. 재생성되면 이 값이 재생성 시점으로
     * 갱신되는데, id는 그대로라 id로 정렬하면 방금 재생성된 파일이 옛날 자리에 남는 문제가 있었다.
     */
    @Override
    public List<Guideline> findAllWithFileByCompanyId(Long companyId, String query, LocalDateTime cursorCreatedAt, int limit) {
        return queryFactory
                .selectFrom(guideline)
                .where(
                        guideline.company.id.eq(companyId),
                        guideline.pdfS3Meta.s3FullKey.isNotNull(),
                        searchCondition(query),
                        cursorCondition(cursorCreatedAt)
                )
                .orderBy(guideline.pdfS3Meta.createdAt.desc())
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

    private BooleanExpression cursorCondition(LocalDateTime cursorCreatedAt) {
        return cursorCreatedAt != null ? guideline.pdfS3Meta.createdAt.lt(cursorCreatedAt) : null;
    }

    private BooleanExpression searchCondition(String query) {
        if (query == null || query.isBlank())
            return null;

        return guideline.guidelineId.containsIgnoreCase(query)
                .or(guideline.pdfS3Meta.originalFileName.containsIgnoreCase(query));
    }
}
