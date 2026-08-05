package com.aivle.sellon.domain.report.service;

import com.aivle.sellon.domain.report.dto.message.MonthlyReportPayload;
import com.aivle.sellon.domain.report.dto.response.ReportResponse;
import com.aivle.sellon.domain.report.entity.Report;
import com.aivle.sellon.domain.report.repository.ReportRepository;
import com.aivle.sellon.global.security.principal.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final ReportRepository reportRepository;

    public List<ReportResponse> getReports(UserPrincipal principal) {
        return reportRepository.findAllByCompanyId(principal.getCompanyId()).stream()
                .map(ReportResponse::of)
                .toList();
    }

    @Transactional
    public void saveGeneratedReport(MonthlyReportPayload payload) {
        reportRepository.findByReportId(payload.reportId())
                .ifPresentOrElse(
                        report -> report.update(payload),
                        () -> reportRepository.save(Report.create(payload))
                );
    }
}
