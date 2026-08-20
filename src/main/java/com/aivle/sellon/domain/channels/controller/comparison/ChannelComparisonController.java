package com.aivle.sellon.domain.channels.controller.comparison;

import com.aivle.sellon.domain.channels.dto.response.*;
import com.aivle.sellon.domain.channels.service.comparison.ChannelComparisonService;
import com.aivle.sellon.global.common.ApiResponse;
import com.aivle.sellon.global.security.principal.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/channels/comparison")
@RequiredArgsConstructor
public class ChannelComparisonController {

    private final ChannelComparisonService channelComparisonService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ChannelComparisonResponse>>> getComparisons(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ApiResponse.ok(channelComparisonService.getComparisons(principal.getCompanyId()));
    }

    @GetMapping("/{usersChannelKey}/aspects")
    public ResponseEntity<ApiResponse<List<ChannelInquiryTypeResponse>>> getAspects(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long usersChannelKey,
            @RequestParam(required = false, defaultValue = "3") Integer limit
    ) {
        return ApiResponse.ok(channelComparisonService.getAspectDistribution(principal.getCompanyId(), usersChannelKey, limit));
    }

    @GetMapping("/inquiry-type-radar")
    public ResponseEntity<ApiResponse<List<ChannelInquiryTypeRadarResponse>>> getInquiryTypeRadar(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ApiResponse.ok(channelComparisonService.getInquiryTypeRadar(principal.getCompanyId()));
    }

    @GetMapping("/{usersChannelKey}/monthly")
    public ResponseEntity<ApiResponse<List<ChannelMonthlyInquiryResponse>>> getMonthly(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long usersChannelKey
    ) {
        return ApiResponse.ok(channelComparisonService.getMonthlyInquiries(principal.getCompanyId(), usersChannelKey));
    }

    @GetMapping("/{usersChannelKey}/insights")
    public ResponseEntity<ApiResponse<List<ChannelInsightResponse>>> getInsights(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long usersChannelKey
    ) {
        return ApiResponse.ok(channelComparisonService.getInsights(principal.getCompanyId(), usersChannelKey));
    }

    /**
     * Agent2를 호출해 이 채널의 비교분석 데이터를 새로 받아와 저장한다.
     * TODO: 지금은 수동 트리거용 엔드포인트 — 실제로는 배치/스케줄러가 호출하도록 교체 필요.
     */
    @PostMapping("/{usersChannelKey}/refresh")
    public ResponseEntity<Void> refresh(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long usersChannelKey
    ) {
        channelComparisonService.refreshComparisonData(principal.getCompanyId(), usersChannelKey);
        return ResponseEntity.noContent().build();
    }

    /**
     * 회사가 연동한 채널 전부를 한 번에 refresh. usersChannelKey 목록을 몰라도(GET /channels 미포팅)
     * 채널 비교 분석 페이지 진입 시 이것부터 호출하면 전체 채널 데이터가 채워진다.
     */
    @PostMapping("/refresh")
    public ResponseEntity<Void> refreshAll(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        channelComparisonService.refreshAllForCompany(principal.getCompanyId());
        return ResponseEntity.noContent().build();
    }
}
