package com.aivle.sellon.domain.dashboard.controller;

import com.aivle.sellon.domain.dashboard.dto.response.DashboardResponse;
import com.aivle.sellon.domain.dashboard.service.DashboardService;
import com.aivle.sellon.global.common.ApiResponse;
import com.aivle.sellon.global.security.principal.UserPrincipal;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/sellon/dashboard")
@RequiredArgsConstructor
@Validated
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    public ResponseEntity<ApiResponse<DashboardResponse>> getDashboard(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "7D")
            @Pattern(regexp = "1D|7D|30D", message = "period는 1D, 7D, 30D 중 하나여야 합니다.") String period
    ) {
        return ApiResponse.ok(dashboardService.getDashboard(principal, period));
    }
}
