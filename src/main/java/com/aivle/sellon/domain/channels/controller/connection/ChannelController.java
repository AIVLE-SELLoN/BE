package com.aivle.sellon.domain.channels.controller.connection;

import com.aivle.sellon.domain.channels.dto.request.ChannelConnectRequest;
import com.aivle.sellon.domain.channels.dto.response.ChannelConnectionResponse;
import com.aivle.sellon.domain.channels.service.connection.ChannelService;
import com.aivle.sellon.global.security.principal.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/channels")
@RequiredArgsConstructor
public class ChannelController {

    private final ChannelService channelService;

    @PostMapping("/connect")
    public ResponseEntity<ChannelConnectionResponse> connect(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody ChannelConnectRequest request
    ) {
        return ResponseEntity.ok(channelService.connect(principal, request));
    }

    @GetMapping("/naver/authorize")
    public ResponseEntity<Void> naverAuthorize(@AuthenticationPrincipal UserPrincipal principal) {
        String authorizationUrl = channelService.naverAuthorize(principal);
        return ResponseEntity.status(302)
                .location(URI.create(authorizationUrl))
                .header(HttpHeaders.LOCATION, authorizationUrl)
                .build();
    }

    @GetMapping("/naver/callback")
    public ResponseEntity<ChannelConnectionResponse> naverCallback(
            @RequestParam String code,
            @RequestParam String state
    ) {
        return ResponseEntity.ok(channelService.naverCallback(code, state));
    }
}
