package com.aivle.sellon.domain.guideline.repository;

import com.aivle.sellon.domain.guideline.entity.Guideline;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GuidelineRepository extends JpaRepository<Guideline, Long>, GuidelineRepositoryCustom {
}
