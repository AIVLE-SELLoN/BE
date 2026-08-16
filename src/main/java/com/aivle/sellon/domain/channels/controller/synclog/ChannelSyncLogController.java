package com.aivle.sellon.domain.channels.controller.synclog;

import com.aivle.sellon.domain.channels.dto.response.ChannelSyncLogResponse;
import com.aivle.sellon.domain.channels.service.synclog.ChannelSyncLogService;
import com.aivle.sellon.global.security.principal.UserPrincipal;
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

    @GetMapping
    public ResponseEntity<Page<ChannelSyncLogResponse>> getSyncLogs(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String channelType,
            Pageable pageable
    ) {
        return ResponseEntity.ok(channelSyncLogService.getSyncLogs(principal.getCompanyId(), channelType, pageable));
    }
}
