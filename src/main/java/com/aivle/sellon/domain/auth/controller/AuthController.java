package com.aivle.sellon.domain.auth.controller;

import com.aivle.sellon.domain.auth.dto.request.FindIdRequest;
import com.aivle.sellon.domain.auth.dto.request.FindPasswordRequest;
import com.aivle.sellon.domain.auth.dto.request.LoginRequest;
import com.aivle.sellon.domain.auth.dto.request.MemberSignupRequest;
import com.aivle.sellon.domain.auth.dto.request.ReissueRequest;
import com.aivle.sellon.domain.auth.dto.request.RootSignupRequest;
import com.aivle.sellon.domain.auth.dto.response.FindIdResponse;
import com.aivle.sellon.domain.auth.dto.response.FindPasswordResponse;
import com.aivle.sellon.domain.auth.dto.response.LoginResponse;
import com.aivle.sellon.domain.auth.dto.response.LoginResult;
import com.aivle.sellon.domain.auth.dto.response.SignupResponse;
import com.aivle.sellon.domain.auth.dto.response.TokenResponse;
import com.aivle.sellon.domain.auth.exception.InvalidCredentialsException;
import com.aivle.sellon.domain.auth.service.AuthService;
import com.aivle.sellon.global.common.ApiResponse;
import com.aivle.sellon.global.security.jwt.JwtProvider;
import com.aivle.sellon.global.security.principal.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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

    // 시연용 원클릭 로그인 계정. 실제 비밀번호는 소스에 담지 않고 env var로만 받는다.
    @Value("${sellon.demo.login.email:}")
    private String demoLoginEmail;

    @Value("${sellon.demo.login.password:}")
    private String demoLoginPassword;

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
        return loginResponse(authService.login(request));
    }

    // 시연용 버튼 전용 - 요청 바디 없이 서버에 설정된 데모 계정으로 바로 로그인 처리한다.
    @PostMapping("/demo-login")
    public ResponseEntity<ApiResponse<LoginResponse>> demoLogin() {
        if (demoLoginEmail.isBlank() || demoLoginPassword.isBlank()) {
            throw new InvalidCredentialsException();
        }
        return loginResponse(authService.login(new LoginRequest(demoLoginEmail, demoLoginPassword)));
    }

    private ResponseEntity<ApiResponse<LoginResponse>> loginResponse(LoginResult result) {
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

    // 아이디 찾기 - 회사명 + 사용자 이름으로 마스킹된 가입 이메일을 조회
    @PostMapping("/find-id")
    public ResponseEntity<ApiResponse<FindIdResponse>> findId(@Valid @RequestBody FindIdRequest request) {
        return ApiResponse.ok(authService.findId(request));
    }

    // 비밀번호 찾기 - 가입 이메일로 임시 비밀번호를 즉시 발급/발송
    @PostMapping("/find-password")
    public ResponseEntity<ApiResponse<FindPasswordResponse>> findPassword(@Valid @RequestBody FindPasswordRequest request) {
        return ApiResponse.ok(authService.findPassword(request));
    }
}
