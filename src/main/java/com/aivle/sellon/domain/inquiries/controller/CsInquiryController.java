package com.aivle.sellon.domain.inquiries.controller;

import com.aivle.sellon.domain.inquiries.dto.request.CsAnswerRequest;
import com.aivle.sellon.domain.inquiries.dto.request.CsInquiryRequest;
import com.aivle.sellon.domain.inquiries.dto.request.CsInquiryUpdateRequest;
import com.aivle.sellon.domain.inquiries.dto.response.CsInquiryResponse;
import com.aivle.sellon.domain.inquiries.enums.InquiryStatus;
import com.aivle.sellon.domain.inquiries.service.CsInquiryService;
import com.aivle.sellon.domain.user.enums.Role;
import com.aivle.sellon.domain.user.exception.AdminAccessRequiredException;
import com.aivle.sellon.global.common.ApiResponse;
import com.aivle.sellon.global.security.principal.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/inquiries")
@RequiredArgsConstructor
public class CsInquiryController {

    private final CsInquiryService csInquiryService;

    @PostMapping
    public ResponseEntity<ApiResponse<CsInquiryResponse>> create(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CsInquiryRequest request
    ) {
        CsInquiryResponse response = csInquiryService.createInquiry(principal, request);
        return ApiResponse.ok(response);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CsInquiryResponse>>> getMyInquiries(@AuthenticationPrincipal UserPrincipal principal) {
        List<CsInquiryResponse> inquiries = csInquiryService.getMyInquiries(principal);
        return ApiResponse.ok(inquiries);
    }

    @GetMapping("/admin")
    public ResponseEntity<ApiResponse<List<CsInquiryResponse>>> getAllInquiries(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false)InquiryStatus status
    ) {
        requireAdmin(principal);
        return ApiResponse.ok(csInquiryService.getAllInquiries(status));
    }

    @GetMapping("/{inquireKey}")
    public ResponseEntity<ApiResponse<CsInquiryResponse>> getDetail(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long inquireKey
    ) {
        return ApiResponse.ok(csInquiryService.getInquiryDetail(principal, inquireKey));
    }

    private void requireAdmin(UserPrincipal principal) {
        if (principal.getRole() != Role.ADMIN)
            throw new AdminAccessRequiredException();
    }

    @PatchMapping("/{inquireKey}")
    public ResponseEntity<ApiResponse<CsInquiryResponse>> update(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long inquireKey,
            @Valid @RequestBody CsInquiryUpdateRequest request
    ) {
        CsInquiryResponse response = csInquiryService.updateInquiry(principal, inquireKey, request);
        return ApiResponse.ok(response);
    }

    @DeleteMapping("/{inquireKey}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long inquireKey
    ) {
        csInquiryService.deleteInquiry(principal, inquireKey);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{inquireKey}/answer")
    public ResponseEntity<ApiResponse<CsInquiryResponse>> answer(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable Long inquireKey,
        @Valid @RequestBody CsAnswerRequest request
    ) {
        requireAdmin(principal);
        CsInquiryResponse response = csInquiryService.answerInquiry(inquireKey, request);
        return ApiResponse.ok(response);
    }
}
