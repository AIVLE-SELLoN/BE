package com.aivle.sellon.domain.guideline.repository;

import com.aivle.sellon.domain.guideline.entity.GuidelineSummary;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GuidelineSummaryRepository
        extends JpaRepository<GuidelineSummary, Long>, GuidelineSummaryRepositoryCustom {
}
