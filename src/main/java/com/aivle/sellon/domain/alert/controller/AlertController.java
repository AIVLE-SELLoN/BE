package com.aivle.sellon.domain.alert.controller;

import com.aivle.sellon.domain.alert.dto.response.AlertListResponse;
import com.aivle.sellon.domain.alert.dto.response.AlertDetailResponse;
import com.aivle.sellon.domain.alert.dto.response.AlertReadResponse;
import com.aivle.sellon.domain.alert.service.AlertService;
import com.aivle.sellon.global.common.ApiResponse;
import com.aivle.sellon.global.security.principal.UserPrincipal;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/sellon/alerts")
@RequiredArgsConstructor
@Validated
public class AlertController {

    private final AlertService alertService;

    @GetMapping
    public ResponseEntity<ApiResponse<AlertListResponse>> getAlerts(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") @Min(1) int size,
            @RequestParam(defaultValue = "false") boolean unreadOnly
    ) {
        return ApiResponse.ok(alertService.getAlerts(principal, cursor, size, unreadOnly));
    }

    @GetMapping("/{notificationId}")
    public ResponseEntity<ApiResponse<AlertDetailResponse>> getAlert(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long notificationId
    ) {
        return ApiResponse.ok(alertService.getAlert(principal, notificationId));
    }

    @GetMapping("/{notificationId}/read")
    public ResponseEntity<ApiResponse<AlertReadResponse>> markAsRead(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long notificationId
    ) {
        return ApiResponse.ok(alertService.markAsRead(principal, notificationId));
    }
}
