package com.aivle.sellon.domain.channels.controller.productmapping;

import com.aivle.sellon.domain.channels.dto.response.ProductMappingReviewItemResponse;
import com.aivle.sellon.domain.channels.service.productmapping.ProductMappingReviewService;
import com.aivle.sellon.global.common.ApiResponse;
import com.aivle.sellon.global.security.principal.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 매칭 툴(docker_mapping_tool)이 "보류(hold)"로 넘긴 상품 쌍(review_queue.csv) 조회/처리.
 * 두 상품이 서로 다른 채널에 속할 수 있어 특정 usersChannel이 아닌 회사 단위로 조회한다.
 */
@RestController
@RequestMapping("/channels/product-mapping-review")
@RequiredArgsConstructor
public class ProductMappingReviewController {

    private final ProductMappingReviewService productMappingReviewService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ProductMappingReviewItemResponse>>> getReviewQueue(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "false") boolean resolved
    ) {
        return ApiResponse.ok(productMappingReviewService.getReviewQueue(principal.getCompanyId(), resolved));
    }

    @PatchMapping("/{reviewItemId}/resolve")
    public ResponseEntity<ApiResponse<ProductMappingReviewItemResponse>> resolve(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long reviewItemId
    ) {
        return ApiResponse.ok(productMappingReviewService.resolve(principal.getCompanyId(), reviewItemId));
    }
}
