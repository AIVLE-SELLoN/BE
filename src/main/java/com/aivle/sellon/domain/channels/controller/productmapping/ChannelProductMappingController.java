package com.aivle.sellon.domain.channels.controller.productmapping;

import com.aivle.sellon.domain.channels.dto.request.ConnectMappingRequest;
import com.aivle.sellon.domain.channels.dto.request.NewGroupRequest;
import com.aivle.sellon.domain.channels.dto.response.ChannelProductResponse;
import com.aivle.sellon.domain.channels.dto.response.MappingSummaryResponse;
import com.aivle.sellon.domain.channels.dto.response.MatchCandidateResponse;
import com.aivle.sellon.domain.channels.service.productmapping.ChannelProductService;
import com.aivle.sellon.global.common.ApiResponse;
import com.aivle.sellon.global.security.principal.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// 회사가 연동한 채널(쿠팡/지그재그/네이버) 전체를 합쳐서 보여주는 상품 매핑 API.
// usersChannelKey로 채널 하나씩 나눠 보던 예전 방식(ChannelProductController) 대신, companyId 기준으로 통합 조회한다.
@RestController
@RequestMapping("/channels/product-mappings")
@RequiredArgsConstructor
public class ChannelProductMappingController {

    private final ChannelProductService channelProductService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ChannelProductResponse>>> getMappings(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) Boolean matched,
            @RequestParam(required = false) String keyword
    ) {
        return ApiResponse.ok(channelProductService.getMappings(principal.getCompanyId(), matched, keyword));
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<MappingSummaryResponse>> getSummary(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ApiResponse.ok(channelProductService.getSummary(principal.getCompanyId()));
    }

    /**
     * channelProductKey 자리에 raw db products의 자연키인 variant_row_id(예: VR-0001)를 그대로 쓴다.
     */
    @GetMapping("/{variantRowId}/candidates")
    public ResponseEntity<ApiResponse<List<MatchCandidateResponse>>> getCandidates(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String variantRowId,
            @RequestParam(required = false) String keyword
    ) {
        return ApiResponse.ok(channelProductService.getCandidates(principal.getCompanyId(), variantRowId, keyword));
    }

    @PatchMapping("/{variantRowId}/connect")
    public ResponseEntity<ApiResponse<ChannelProductResponse>> connect(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String variantRowId,
            @RequestBody ConnectMappingRequest request
    ) {
        return ApiResponse.ok(channelProductService.connect(principal.getCompanyId(), variantRowId, request));
    }

    @PatchMapping("/{variantRowId}/skip")
    public ResponseEntity<ApiResponse<ChannelProductResponse>> skip(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String variantRowId
    ) {
        return ApiResponse.ok(channelProductService.skip(principal.getCompanyId(), variantRowId));
    }

    @PostMapping("/{variantRowId}/new-group")
    public ResponseEntity<ApiResponse<ChannelProductResponse>> createNewGroup(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String variantRowId,
            @RequestBody NewGroupRequest request
    ) {
        return ApiResponse.ok(channelProductService.createNewGroup(principal.getCompanyId(), variantRowId, request));
    }
}
