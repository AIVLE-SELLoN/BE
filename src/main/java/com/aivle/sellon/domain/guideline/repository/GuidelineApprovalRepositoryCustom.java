package com.aivle.sellon.domain.guideline.repository;

import com.aivle.sellon.domain.guideline.entity.GuidelineApproval;

import java.util.Optional;

public interface GuidelineApprovalRepositoryCustom {

    Optional<GuidelineApproval> findByGuidelineId(Long guidelineId);
}
