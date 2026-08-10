package com.aivle.sellon.domain.guideline.repository;

import com.aivle.sellon.domain.guideline.entity.GuidelineSummary;

import java.util.Optional;

public interface GuidelineSummaryRepositoryCustom {

    /** guideline(FK) 기준 upsert 조회용. */
    Optional<GuidelineSummary> findByGuidelineId(Long guidelineId);
}
