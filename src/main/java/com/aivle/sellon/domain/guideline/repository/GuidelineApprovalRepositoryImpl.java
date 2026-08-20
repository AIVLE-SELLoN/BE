package com.aivle.sellon.domain.guideline.repository;

import com.aivle.sellon.domain.guideline.entity.GuidelineApproval;
import com.aivle.sellon.domain.guideline.entity.QGuidelineApproval;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

@RequiredArgsConstructor
public class GuidelineApprovalRepositoryImpl implements GuidelineApprovalRepositoryCustom {

    private static final QGuidelineApproval approval = QGuidelineApproval.guidelineApproval;

    private final JPAQueryFactory queryFactory;

    @Override
    public Optional<GuidelineApproval> findByGuidelineId(Long guidelineId) {
        return Optional.ofNullable(
                queryFactory
                        .selectFrom(approval)
                        .where(approval.guideline.id.eq(guidelineId))
                        .fetchOne()
        );
    }
}
