package com.aivle.sellon.domain.report.service;

import com.aivle.sellon.domain.company.entity.Company;
import com.aivle.sellon.domain.company.exception.CompanyNotFoundException;
import com.aivle.sellon.domain.company.repository.CompanyRepository;
import com.aivle.sellon.domain.report.dto.message.MonthlyReportPayload;
import com.aivle.sellon.domain.report.dto.response.ReportResponse;
import com.aivle.sellon.domain.report.entity.Report;
import com.aivle.sellon.domain.report.enums.ReportStatus;
import com.aivle.sellon.domain.report.event.ReportGeneratedEvent;
import com.aivle.sellon.domain.report.exception.ReportNotFoundException;
import com.aivle.sellon.domain.report.repository.ReportRepository;
import com.aivle.sellon.global.security.principal.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final ReportRepository reportRepository;
    private final CompanyRepository companyRepository;
    private final ReportDownloadUrlService reportDownloadUrlService;
    private final ApplicationEventPublisher eventPublisher;

    public List<ReportResponse> getReports(UserPrincipal principal) {
        return reportRepository.findAllByCompanyId(principal.getCompanyId()).stream()
                .map(report -> ReportResponse.of(report, reportDownloadUrlService.generate(report.getPdfS3Meta())))
                .toList();
    }

    // 프론트는 status로 분기한다: SUCCESS면 월간 리포트 화면을, FAILED_*면 noticeMessage로 오류 화면을 띄운다
    public ReportResponse getReport(UserPrincipal principal, String reportId) {
        Report report = reportRepository.findByCompanyIdAndReportId(principal.getCompanyId(), reportId)
                .orElseThrow(ReportNotFoundException::new);

        return ReportResponse.of(report, reportDownloadUrlService.generate(report.getPdfS3Meta()));
    }

    @Transactional
    public void saveGeneratedReport(MonthlyReportPayload payload) {
        Company company = findCompany(payload.companyId());

        // reportId는 RPT-{YYYYMM} 형식이라 회사를 함께 봐야 다른 회사 리포트를 덮어쓰지 않는다
        Report report = reportRepository.findByCompanyIdAndReportId(company.getId(), payload.reportId())
                .map(existing -> {
                    existing.update(payload);
                    return existing;
                })
                .orElseGet(() -> reportRepository.save(Report.create(payload, company)));

        if (report.getStatus() == ReportStatus.SUCCESS)
            eventPublisher.publishEvent(
                    new ReportGeneratedEvent(company.getId(), report.getReportMonth(), report.getPdfS3Meta()));
    }

    private Company findCompany(Long companyId) {
        if (companyId == null)
            throw new CompanyNotFoundException();

        return companyRepository.findById(companyId)
                .orElseThrow(CompanyNotFoundException::new);
    }
}
