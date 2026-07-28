package com.aivle.sellon.domain.verification.controller;

import com.aivle.sellon.domain.verification.dto.response.VerificationTokenResponse;
import com.aivle.sellon.domain.verification.service.EmailVerificationService;
import com.aivle.sellon.global.common.ApiResponse;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/sellon/verification")
@RequiredArgsConstructor
@Validated
public class VerificationController {

    private final EmailVerificationService emailVerificationService;

    @GetMapping("/email-verification")
    public ResponseEntity<ApiResponse<Void>> sendEmailVerification(
            @RequestParam(name = "email") @NotBlank @Email String email
    ) {
        emailVerificationService.sendVerificationCode(email);
        return ApiResponse.ok();
    }

    @GetMapping("/email-verification/confirm")
    public ResponseEntity<ApiResponse<VerificationTokenResponse>> confirmEmailVerification(
            @RequestParam(name = "email") @NotBlank @Email String email,
            @RequestParam(name = "code") @NotBlank String code
    ) {
        String token = emailVerificationService.verifyCode(email, code);
        return ApiResponse.ok(new VerificationTokenResponse(token));
    }
}
