package com.aivle.sellon.domain.inquiries.controller;

import com.aivle.sellon.domain.inquiries.dto.*;
import com.aivle.sellon.domain.inquiries.enums.InquiryStatus;
import com.aivle.sellon.domain.inquiries.service.CsInquiryService;
import com.aivle.sellon.global.security.principal.UserPrincipal;
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
    public ResponseEntity<CsInquiryResponse> create(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody CsInquiryRequest request
    ) {
        CsInquiryResponse response = csInquiryService.createInquiry(principal, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<CsInquiryResponse>> getMyInquiries(@AuthenticationPrincipal UserPrincipal principal) {
        List<CsInquiryResponse> inquiries = csInquiryService.getMyInquiries(principal);
        return ResponseEntity.ok(inquiries);
    }

    @GetMapping("/admin")
    public ResponseEntity<List<CsInquiryResponse>> getAllInquiries(
            @RequestParam(required = false)InquiryStatus status
    ) {
        // TODO: 운영자 권한 체크 필요
        return ResponseEntity.ok(csInquiryService.getAllInquiries(status));
    }

    @GetMapping("/{inquireKey}")
    public ResponseEntity<CsInquiryResponse> getDetail(@PathVariable Long inquireKey) {
        return ResponseEntity.ok(csInquiryService.getInquiryDetail(inquireKey));
    } // TODO: 본인 문의이거나 운영자인 경우만 허용하는 조건 추가(문의 내용 상세)

    @PostMapping("/{inquireKey}/answer")
    public ResponseEntity<CsInquiryResponse> answer(
        @PathVariable Long inquireKey,
        @RequestBody CsAnswerRequest request
    ) {
        CsInquiryResponse response = csInquiryService.answerInquiry(inquireKey, request);
        return ResponseEntity.ok(response);
    } // TODO: 운영자인 경우만 허용하는 조건 추가 (답변 로직)
}
