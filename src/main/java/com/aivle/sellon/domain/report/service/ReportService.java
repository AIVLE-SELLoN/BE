package com.aivle.sellon.domain.report.service;

import com.aivle.sellon.domain.company.entity.Company;
import com.aivle.sellon.domain.company.exception.CompanyNotFoundException;
import com.aivle.sellon.domain.company.repository.CompanyRepository;
import com.aivle.sellon.domain.report.dto.message.MonthlyReportPayload;
import com.aivle.sellon.domain.report.dto.response.ReportResponse;
import com.aivle.sellon.domain.report.entity.Report;
import com.aivle.sellon.domain.report.event.ReportGeneratedEvent;
import com.aivle.sellon.domain.report.exception.ReportNotFoundException;
import com.aivle.sellon.domain.report.repository.ReportRepository;
import com.aivle.sellon.global.security.principal.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
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
    public void saveGeneratedReport(String companyKey, MonthlyReportPayload payload) {
        Company company = findCompany(companyKey);

        // reportId는 RPT-{YYYYMM} 형식이라 회사를 함께 봐야 다른 회사 리포트를 덮어쓰지 않는다
        Report report = reportRepository.findByCompanyIdAndReportId(company.getId(), payload.reportId())
                .map(existing -> {
                    existing.update(payload);
                    return existing;
                })
                .orElseGet(() -> reportRepository.save(Report.create(payload, company)));

        // 큐 컨벤션 §4.2 - 월간 이벤트의 status는 이 3종뿐이고 케이스별 처리가 다르다
        switch (report.getStatus()) {
            case SUCCESS -> eventPublisher.publishEvent(
                    new ReportGeneratedEvent(report.getId(), company.getId()));

            // 합본 PDF가 10MB를 넘어 S3 업로드 이전에 차단됐다. 보낼 링크 자체가 없다
            case FAILED_SIZE_EXCEEDED -> log.warn(
                    "월간 보고서 메일 발송 중단 - PDF 크기 초과. companyId={}, reportId={}, notice={}",
                    company.getId(), payload.reportId(), payload.noticeMessage());

            // 수록할 상품이 하나도 없거나 S3 미구성 등
            case FAILED_ERROR -> log.error(
                    "월간 보고서 생성 실패. companyId={}, reportId={}, notice={}",
                    company.getId(), payload.reportId(), payload.noticeMessage());

            // 상품 단위 상태라 월간 이벤트로는 오지 않아야 한다. 오면 발행부 확인이 필요하다
            default -> log.warn(
                    "월간 이벤트에 오지 않아야 할 status 수신 - status={}, companyId={}, reportId={}",
                    report.getStatus(), company.getId(), payload.reportId());
        }
    }

    // company_id는 DB PK가 아니라 회원가입 시 발급되는 회사 식별키(join_key)다.
    private Company findCompany(String companyKey) {
        if (companyKey == null || companyKey.isBlank())
            throw new CompanyNotFoundException();

        return companyRepository.findByJoinKey(companyKey)
                .orElseThrow(CompanyNotFoundException::new);
    }
}
