package com.aivle.sellon.domain.auth.controller;

import com.aivle.sellon.domain.auth.dto.request.LoginRequest;
import com.aivle.sellon.domain.auth.dto.request.MemberSignupRequest;
import com.aivle.sellon.domain.auth.dto.request.ReissueRequest;
import com.aivle.sellon.domain.auth.dto.request.RootSignupRequest;
import com.aivle.sellon.domain.auth.dto.response.LoginResponse;
import com.aivle.sellon.domain.auth.dto.response.LoginResult;
import com.aivle.sellon.domain.auth.dto.response.SignupResponse;
import com.aivle.sellon.domain.auth.dto.response.TokenResponse;
import com.aivle.sellon.domain.auth.service.AuthService;
import com.aivle.sellon.global.common.ApiResponse;
import com.aivle.sellon.global.security.jwt.JwtProvider;
import com.aivle.sellon.global.security.principal.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/sellon/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String BEARER_PREFIX = "Bearer ";

    private final AuthService authService;
    private final JwtProvider jwtProvider;

    @PostMapping("/register/root")
    public ResponseEntity<ApiResponse<SignupResponse>> signupRoot(@Valid @RequestBody RootSignupRequest request) {
        return ApiResponse.create(authService.signupRoot(request));
    }

    @PostMapping("/register/member")
    public ResponseEntity<ApiResponse<SignupResponse>> signupMember(@Valid @RequestBody MemberSignupRequest request) {
        return ApiResponse.create(authService.signupMember(request));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        LoginResult result = authService.login(request);
        TokenResponse tokens = result.tokenResponse();
        return ApiResponse.ofTokens(result.loginResponse(), BEARER_PREFIX + tokens.accessToken(), tokens.refreshToken());
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request
    ) {
        String accessToken = jwtProvider.resolveToken(request);
        authService.logout(principal, accessToken);
        return ApiResponse.ok();
    }

    @PostMapping("/reissue")
    public ResponseEntity<ApiResponse<Void>> reissue(@Valid @RequestBody ReissueRequest request) {
        TokenResponse tokens = authService.reissue(request);
        return ApiResponse.ofTokens(null, BEARER_PREFIX + tokens.accessToken(), tokens.refreshToken());
    }
}
