package com.aivle.sellon.domain.channels.controller.connection;

import com.aivle.sellon.domain.channels.dto.request.ChannelConnectRequest;
import com.aivle.sellon.domain.channels.dto.response.ChannelConnectionResponse;
import com.aivle.sellon.domain.channels.dto.response.NaverAuthorizeResponse;
import com.aivle.sellon.domain.channels.service.connection.ChannelService;
import com.aivle.sellon.global.common.ApiResponse;
import com.aivle.sellon.global.security.principal.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/channels")
@RequiredArgsConstructor
public class ChannelController {

    private final ChannelService channelService;

    @PostMapping("/connect")
    public ResponseEntity<ApiResponse<ChannelConnectionResponse>> connect(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody ChannelConnectRequest request
    ) {
        return ApiResponse.ok(channelService.connect(principal, request));
    }

    @GetMapping("/naver/authorize")
    public ResponseEntity<ApiResponse<NaverAuthorizeResponse>> naverAuthorize(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ApiResponse.ok(channelService.naverAuthorize(principal));
    }

    @GetMapping("/naver/callback")
    public ResponseEntity<ApiResponse<ChannelConnectionResponse>> naverCallback(
            @RequestParam String code,
            @RequestParam String state
    ) {
        return ApiResponse.ok(channelService.naverCallback(code, state));
    }
}
