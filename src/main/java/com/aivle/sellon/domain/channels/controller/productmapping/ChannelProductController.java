package com.aivle.sellon.domain.channels.controller.productmapping;

import com.aivle.sellon.domain.channels.dto.request.ConnectMappingRequest;
import com.aivle.sellon.domain.channels.dto.request.NewGroupRequest;
import com.aivle.sellon.domain.channels.dto.response.ChannelProductResponse;
import com.aivle.sellon.domain.channels.dto.response.MappingSummaryResponse;
import com.aivle.sellon.domain.channels.dto.response.MatchCandidateResponse;
import com.aivle.sellon.domain.channels.service.productmapping.ChannelProductBatchService;
import com.aivle.sellon.domain.channels.service.productmapping.ChannelProductService;
import com.aivle.sellon.global.common.ApiResponse;
import com.aivle.sellon.global.security.principal.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/channels/{usersChannelKey}/product-mappings")
@RequiredArgsConstructor
public class ChannelProductController {

    private final ChannelProductService channelProductService;
    private final ChannelProductBatchService channelProductBatchService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ChannelProductResponse>>> getMappings(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long usersChannelKey,
            @RequestParam(required = false) Boolean matched,
            @RequestParam(required = false) String keyword
    ) {
        return ApiResponse.ok(channelProductService.getMappings(principal.getCompanyId(), usersChannelKey, matched, keyword));
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<MappingSummaryResponse>> getSummary(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long usersChannelKey
    ) {
        return ApiResponse.ok(channelProductService.getSummary(principal.getCompanyId(), usersChannelKey));
    }

    /**
     * channelProductKey 자리에 raw db products의 자연키인 variant_row_id(예: VR-0001)를 그대로 쓴다.
     */
    @GetMapping("/{variantRowId}/candidates")
    public ResponseEntity<ApiResponse<List<MatchCandidateResponse>>> getCandidates(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long usersChannelKey,
            @PathVariable String variantRowId,
            @RequestParam(required = false) String keyword
    ) {
        return ApiResponse.ok(channelProductService.getCandidates(principal.getCompanyId(), usersChannelKey, variantRowId, keyword));
    }

    @PatchMapping("/{variantRowId}/connect")
    public ResponseEntity<ApiResponse<ChannelProductResponse>> connect(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long usersChannelKey,
            @PathVariable String variantRowId,
            @RequestBody ConnectMappingRequest request
    ) {
        return ApiResponse.ok(channelProductService.connect(principal.getCompanyId(), usersChannelKey, variantRowId, request));
    }

    @PostMapping("/{variantRowId}/new-group")
    public ResponseEntity<ApiResponse<ChannelProductResponse>> createNewGroup(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long usersChannelKey,
            @PathVariable String variantRowId,
            @RequestBody NewGroupRequest request
    ) {
        return ApiResponse.ok(channelProductService.createNewGroup(principal.getCompanyId(), usersChannelKey, variantRowId, request));
    }

    @PostMapping("/import/mapping-result")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> importMappingResult(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long usersChannelKey,
            @RequestParam("file") MultipartFile file
    ) {
        int matchedCount = channelProductBatchService.importMappingResult(principal.getCompanyId(), usersChannelKey, file);
        return ApiResponse.ok(Map.of("matchedCount", matchedCount));
    }

    @PostMapping("/import/review-queue")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> importReviewQueue(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long usersChannelKey,
            @RequestParam("file") MultipartFile file
    ) {
        int rowCount = channelProductBatchService.importReviewQueue(principal.getCompanyId(), usersChannelKey, file);
        return ApiResponse.ok(Map.of("rowCount", rowCount));
    }
}
