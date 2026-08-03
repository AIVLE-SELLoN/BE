package com.aivle.sellon.domain.report.service;

import com.aivle.sellon.domain.company.entity.Company;
import com.aivle.sellon.domain.company.exception.CompanyNotFoundException;
import com.aivle.sellon.domain.company.repository.CompanyRepository;
import com.aivle.sellon.domain.report.dto.message.MonthlyReportGeneratedMessage;
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
    private final CompanyRepository companyRepository;

    public List<ReportResponse> getReports(UserPrincipal principal) {
        return reportRepository.findAllByCompanyId(principal.getCompanyId()).stream()
                .map(ReportResponse::of)
                .toList();
    }

    @Transactional
    public List<Report> saveGeneratedReports(MonthlyReportGeneratedMessage message) {
        Company company = companyRepository.findById(message.companyId())
                .orElseThrow(CompanyNotFoundException::new);

        List<Report> reports = message.files().stream()
                .map(file -> Report.create(
                        company,
                        file.originalFileName(),
                        file.storedFileName(),
                        file.fileSize(),
                        file.generatedAt()
                ))
                .toList();

        return reportRepository.saveAll(reports);
    }
}
