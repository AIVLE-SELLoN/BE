package com.aivle.sellon.domain.channels.controller;

import com.aivle.sellon.domain.channels.dto.response.ProductDescriptionResponse;
import com.aivle.sellon.domain.channels.service.ProductDescriptionService;
import com.aivle.sellon.global.common.ApiResponse;
import com.aivle.sellon.global.security.principal.UserPrincipal;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// 채널별 상품 설명(현재 값) 조회. 개선안(proposal) 리뷰 화면에서 현재 상품 설명을 미리 보여주기 위해 추가.
@RestController
@RequestMapping("/channels")
@RequiredArgsConstructor
@Validated
public class ProductDescriptionController {

    private final ProductDescriptionService productDescriptionService;

    @GetMapping("/product-description")
    public ResponseEntity<ApiResponse<ProductDescriptionResponse>> getProductDescription(
            @RequestParam @NotBlank String productGroupId,
            @RequestParam @NotBlank String channel,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        String description = productDescriptionService
                .findDescription(principal.getCompanyId(), productGroupId, channel)
                .orElse(null);

        return ApiResponse.ok(new ProductDescriptionResponse(productGroupId, channel, description));
    }
}
