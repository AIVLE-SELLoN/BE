package com.aivle.sellon.domain.guideline.repository;

import com.aivle.sellon.domain.guideline.entity.GuidelineSummary;

import java.util.List;
import java.util.Optional;

public interface GuidelineSummaryRepositoryCustom {

    /** guideline(FK) 기준 upsert 조회용. */
    Optional<GuidelineSummary> findByGuidelineId(Long guidelineId);

    /**
     * 커서(summary_id) 기준 최신순 조회. limit은 hasNext 판단을 위해 호출부가 요청 size + 1로 넘긴다.
     */
    List<GuidelineSummary> findAllByCompanyId(Long companyId, Long cursorId, int limit);
}
