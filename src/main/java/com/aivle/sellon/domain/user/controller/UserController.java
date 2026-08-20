package com.aivle.sellon.domain.user.controller;

import com.aivle.sellon.domain.user.dto.response.UserResponse;
import com.aivle.sellon.domain.user.service.UserService;
import com.aivle.sellon.global.common.ApiResponse;
import com.aivle.sellon.global.security.jwt.JwtProvider;
import com.aivle.sellon.global.security.principal.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final JwtProvider jwtProvider;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getMe(@AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(userService.getMe(principal));
    }

    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<Void>> withdraw(
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request
    ) {
        userService.withdraw(principal, jwtProvider.resolveToken(request));
        return ApiResponse.ok();
    }
}
