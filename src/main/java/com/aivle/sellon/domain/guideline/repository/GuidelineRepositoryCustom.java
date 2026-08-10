package com.aivle.sellon.domain.guideline.repository;

import com.aivle.sellon.domain.guideline.entity.Guideline;

import java.util.Optional;

public interface GuidelineRepositoryCustom {

    /**
     * 큐 재전달 시 이미 저장된 가이드라인인지 판단하는 upsert 조회용.
     * guidelineId는 alert_id와 1:1이지만 회사 구분자가 없어 단독으로는 유일하지 않다.
     */
    Optional<Guideline> findByCompanyIdAndGuidelineId(Long companyId, String guidelineId);
}
