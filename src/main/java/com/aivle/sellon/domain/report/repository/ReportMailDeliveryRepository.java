package com.aivle.sellon.domain.report.repository;

import com.aivle.sellon.domain.report.entity.ReportMailDelivery;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportMailDeliveryRepository
        extends JpaRepository<ReportMailDelivery, Long>, ReportMailDeliveryRepositoryCustom {
}
