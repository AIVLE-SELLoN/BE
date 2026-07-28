package com.aivle.sellon.domain.report.controller;

import com.aivle.sellon.domain.report.dto.response.ReportResponse;
import com.aivle.sellon.domain.report.service.ReportService;
import com.aivle.sellon.global.common.ApiResponse;
import com.aivle.sellon.global.common.dto.CursorPageResponse;
import com.aivle.sellon.global.security.principal.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/sellon/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping
    public ResponseEntity<ApiResponse<CursorPageResponse<ReportResponse>>> getReports(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String cursor
    ) {
        return ApiResponse.ok(reportService.getReports(principal, cursor));
    }
}
