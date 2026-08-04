package com.aivle.sellon.domain.mypage.controller;

import com.aivle.sellon.domain.mypage.dto.request.MyPageUpdateRequest;
import com.aivle.sellon.domain.mypage.dto.response.CompanyKeyResponse;
import com.aivle.sellon.domain.mypage.dto.response.MyPageResponse;
import com.aivle.sellon.domain.mypage.service.MyPageService;
import com.aivle.sellon.global.common.ApiResponse;
import com.aivle.sellon.global.security.principal.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/sellon/my-page")
@RequiredArgsConstructor
public class MyPageController {

    private final MyPageService myPageService;

    @GetMapping
    public ResponseEntity<ApiResponse<MyPageResponse>> getMyPage(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ApiResponse.ok(myPageService.getMyPage(principal));
    }

    @PatchMapping
    public ResponseEntity<ApiResponse<MyPageResponse>> updateMyPage(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody MyPageUpdateRequest request
    ) {
        return ApiResponse.ok(myPageService.updateMyPage(principal, request));
    }

    @PostMapping("/company-key")
    public ResponseEntity<ApiResponse<CompanyKeyResponse>> issueCompanyKey(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        String companyKey = myPageService.issueCompanyKey(principal);
        return ApiResponse.ok(new CompanyKeyResponse(companyKey));
    }

    @GetMapping("/company-key")
    public ResponseEntity<ApiResponse<CompanyKeyResponse>> getCompanyKey(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        CompanyKeyResponse response = new CompanyKeyResponse(myPageService.getCompanyKey(principal));
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, max-age=0, must-revalidate")
                .header(HttpHeaders.PRAGMA, "no-cache")
                .header(HttpHeaders.EXPIRES, "0")
                .body(ApiResponse.<CompanyKeyResponse>builder().data(response).build());
    }
}
