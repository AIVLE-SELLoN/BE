package com.aivle.sellon.domain.guideline.repository;

import com.aivle.sellon.domain.guideline.entity.GuidelineApproval;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GuidelineApprovalRepository
        extends JpaRepository<GuidelineApproval, Long>, GuidelineApprovalRepositoryCustom {
}
