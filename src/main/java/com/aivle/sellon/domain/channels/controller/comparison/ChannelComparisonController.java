package com.aivle.sellon.domain.channels.controller.comparison;

import com.aivle.sellon.domain.channels.dto.response.*;
import com.aivle.sellon.domain.channels.service.comparison.ChannelComparisonService;
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
    public ResponseEntity<List<ChannelComparisonResponse>> getComparisons(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(channelComparisonService.getComparisons(principal.getCompanyId()));
    }

    @GetMapping("/{usersChannelKey}/aspects")
    public ResponseEntity<List<ChannelInquiryTypeResponse>> getAspects(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long usersChannelKey,
            @RequestParam(required = false, defaultValue = "3") Integer limit
    ) {
        return ResponseEntity.ok(channelComparisonService.getAspectDistribution(principal.getCompanyId(), usersChannelKey, limit));
    }

    @GetMapping("/inquiry-type-radar")
    public ResponseEntity<List<ChannelInquiryTypeRadarResponse>> getInquiryTypeRadar(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(channelComparisonService.getInquiryTypeRadar(principal.getCompanyId()));
    }

    @GetMapping("/{usersChannelKey}/monthly")
    public ResponseEntity<List<ChannelMonthlyInquiryResponse>> getMonthly(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long usersChannelKey
    ) {
        return ResponseEntity.ok(channelComparisonService.getMonthlyInquiries(principal.getCompanyId(), usersChannelKey));
    }

    @GetMapping("/{usersChannelKey}/insights")
    public ResponseEntity<List<ChannelInsightResponse>> getInsights(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long usersChannelKey
    ) {
        return ResponseEntity.ok(channelComparisonService.getInsights(principal.getCompanyId(), usersChannelKey));
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
}
