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

import java.util.List;

@RestController
@RequestMapping("/channels")
@RequiredArgsConstructor
public class ChannelController {

    private final ChannelService channelService;

    // 현재 회사가 연동한 채널 목록 (새로고침 시 상태 복원용)
    @GetMapping
    public ResponseEntity<ApiResponse<List<ChannelConnectionResponse>>> getChannels(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ApiResponse.ok(channelService.getChannels(principal));
    }

    @PostMapping("/connect")
    public ResponseEntity<ApiResponse<ChannelConnectionResponse>> connect(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody ChannelConnectRequest request
    ) {
        return ApiResponse.ok(channelService.connect(principal, request));
    }

    // 연동 해제 - ROOT 전용. channelType: "COUPANG" | "ZIGZAG" | "NAVER"
    @DeleteMapping("/{channelType}")
    public ResponseEntity<ApiResponse<Void>> disconnect(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String channelType
    ) {
        channelService.disconnect(principal, channelType);
        return ApiResponse.ok();
    }

    // 실제 네이버 연동 전까지는 프론트에서 목업 로그인 화면을 띄우기 위해 302 대신 JSON으로 응답한다.
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
