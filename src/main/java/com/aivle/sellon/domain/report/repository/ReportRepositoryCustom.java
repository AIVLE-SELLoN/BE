package com.aivle.sellon.domain.report.repository;

import com.aivle.sellon.domain.report.entity.Report;

import java.util.List;
import java.util.Optional;

public interface ReportRepositoryCustom {
    List<Report> findAllByCompanyId(Long companyId);

    Optional<Report> findByCompanyIdAndReportId(Long companyId, String reportId);
}
