package com.aivle.sellon.domain.report.repository;

import com.aivle.sellon.domain.report.entity.Report;

import java.util.List;

public interface ReportRepositoryCustom {
    List<Report> findAllByCompanyIdWithCursor(Long companyId, Long cursor, int limit);
}
