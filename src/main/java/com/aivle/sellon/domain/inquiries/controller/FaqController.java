package com.aivle.sellon.domain.inquiries.controller;

import com.aivle.sellon.domain.inquiries.dto.response.FaqResponse;
import com.aivle.sellon.domain.inquiries.service.FaqService;
import com.aivle.sellon.global.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/inquiries/faq")
@RequiredArgsConstructor
public class FaqController {

    private final FaqService faqService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<FaqResponse>>> getAllFaqs() {
        return ApiResponse.ok(faqService.getAllFaqs());
    }
}
