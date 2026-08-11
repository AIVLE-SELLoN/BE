package com.aivle.sellon.domain.guideline.repository;

import com.aivle.sellon.domain.guideline.entity.Guideline;
import com.aivle.sellon.domain.guideline.entity.QGuideline;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

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
}
