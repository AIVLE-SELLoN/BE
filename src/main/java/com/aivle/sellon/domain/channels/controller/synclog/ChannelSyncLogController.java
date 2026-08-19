package com.aivle.sellon.domain.channels.controller.synclog;

import com.aivle.sellon.domain.channels.dto.response.ChannelSyncLogResponse;
import com.aivle.sellon.domain.channels.service.synclog.ChannelSyncLogService;
import com.aivle.sellon.global.common.ApiResponse;
import com.aivle.sellon.global.security.principal.UserPrincipal;
import com.aivle.sellon.rawdb.service.RawChannelSyncPollingService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/channels/sync-logs")
@RequiredArgsConstructor
public class ChannelSyncLogController {

    private final ChannelSyncLogService channelSyncLogService;
    private final RawChannelSyncPollingService rawChannelSyncPollingService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<ChannelSyncLogResponse>>> getSyncLogs(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String channelType,
            Pageable pageable
    ) {
        return ApiResponse.ok(channelSyncLogService.getSyncLogs(principal.getCompanyId(), channelType, pageable));
    }

    // 자정 폴링을 안 기다리고 즉시 확인하는 수동 트리거.
    // channelType을 주면 그 채널만, 안 주면 연동된 채널 전부(쿠팡/네이버/지그재그) 폴링한다.
    @PostMapping("/poll")
    public ResponseEntity<Void> pollNow(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String channelType
    ) {
        rawChannelSyncPollingService.pollForCompany(principal.getCompanyId(), channelType);
        return ResponseEntity.noContent().build();
    }
}
