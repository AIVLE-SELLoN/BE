package com.aivle.sellon.domain.channels.controller.synclog;

import com.aivle.sellon.domain.channels.dto.response.ChannelSyncLogResponse;
import com.aivle.sellon.domain.channels.service.synclog.ChannelSyncLogService;
import com.aivle.sellon.global.security.principal.UserPrincipal;
import com.aivle.sellon.rawdb.service.RawChannelEventRetryService;
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
    private final RawChannelEventRetryService rawChannelEventRetryService;

    @GetMapping
    public ResponseEntity<Page<ChannelSyncLogResponse>> getSyncLogs(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String channelType,
            Pageable pageable
    ) {
        return ResponseEntity.ok(channelSyncLogService.getSyncLogs(principal.getCompanyId(), channelType, pageable));
    }

    @PostMapping("/{syncLogKey}/retry")
    public ResponseEntity<Void> retry(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long syncLogKey
    ) {
        rawChannelEventRetryService.retry(principal.getCompanyId(), syncLogKey);
        return ResponseEntity.noContent().build();
    }
}
