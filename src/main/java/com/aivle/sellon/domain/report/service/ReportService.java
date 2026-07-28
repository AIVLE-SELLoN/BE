package com.aivle.sellon.domain.report.service;

import com.aivle.sellon.domain.report.dto.response.ReportResponse;
import com.aivle.sellon.domain.report.entity.Report;
import com.aivle.sellon.domain.report.repository.ReportRepository;
import com.aivle.sellon.global.common.dto.CursorPageResponse;
import com.aivle.sellon.global.common.utils.CursorUtils;
import com.aivle.sellon.global.security.principal.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportService {

    private static final int PAGE_SIZE = 8;

    private final ReportRepository reportRepository;
    private final CursorUtils cursorUtils;

    public CursorPageResponse<ReportResponse> getReports(UserPrincipal principal, String cursor) {
        Long cursorId = cursorUtils.toId(cursor);

        List<Report> reports = reportRepository.findAllByCompanyIdWithCursor(
                principal.getCompanyId(), cursorId, PAGE_SIZE + 1
        );

        boolean hasNext = reports.size() > PAGE_SIZE;
        List<Report> content = hasNext ? reports.subList(0, PAGE_SIZE) : reports;
        String nextCursor = hasNext ? cursorUtils.toCursor(content.get(content.size() - 1).getId()) : null;

        List<ReportResponse> responses = content.stream()
                .map(ReportResponse::of)
                .toList();

        return new CursorPageResponse<>(responses, nextCursor, hasNext);
    }
}
