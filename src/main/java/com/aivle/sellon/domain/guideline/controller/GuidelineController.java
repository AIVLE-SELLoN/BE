package com.aivle.sellon.domain.guideline.controller;

import com.aivle.sellon.domain.guideline.dto.response.GuidelineListItemResponse;
import com.aivle.sellon.domain.guideline.service.GuidelineMailService;
import com.aivle.sellon.domain.guideline.service.GuidelineService;
import com.aivle.sellon.global.common.ApiResponse;
import com.aivle.sellon.global.common.dto.CursorPageResponse;
import com.aivle.sellon.global.security.principal.UserPrincipal;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/sellon/guidelines")
@RequiredArgsConstructor
@Validated
public class GuidelineController {

    private final GuidelineService guidelineService;
    private final GuidelineMailService guidelineMailService;

    @GetMapping
    public ResponseEntity<ApiResponse<CursorPageResponse<GuidelineListItemResponse>>> getGuidelines(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") @Positive int size
    ) {
        return ApiResponse.ok(guidelineService.getGuidelines(principal, cursor, size));
    }

    @PostMapping("/{guidelineId}/mail")
    public ResponseEntity<ApiResponse<Void>> sendMail(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String guidelineId
    ) {
        guidelineMailService.sendMail(principal, guidelineId);
        return ApiResponse.ok();
    }
}
