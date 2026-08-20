package com.aivle.sellon.domain.auth.service;

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
import com.aivle.sellon.domain.auth.exception.AccountNotFoundException;
import com.aivle.sellon.domain.auth.exception.InvalidCredentialsException;
import com.aivle.sellon.domain.auth.util.EmailMasker;
import com.aivle.sellon.domain.company.entity.Company;
import com.aivle.sellon.domain.company.exception.InvalidCompanyKeyException;
import com.aivle.sellon.domain.company.repository.CompanyRepository;
import com.aivle.sellon.domain.user.entity.User;
import com.aivle.sellon.domain.user.exception.DuplicateEmailException;
import com.aivle.sellon.domain.user.exception.UserNotFoundException;
import com.aivle.sellon.domain.user.repository.UserRepository;
import com.aivle.sellon.domain.verification.service.EmailVerificationService;
import com.aivle.sellon.global.redis.service.RefreshTokenRedisService;
import com.aivle.sellon.global.redis.service.TokenBlacklistRedisService;
import com.aivle.sellon.global.security.jwt.JwtProvider;
import com.aivle.sellon.global.security.jwt.exception.ExpiredTokenException;
import com.aivle.sellon.global.security.jwt.exception.InvalidTokenException;
import com.aivle.sellon.global.security.principal.UserPrincipal;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtProvider jwtProvider;
    private final RefreshTokenRedisService refreshTokenRedisService;
    private final TokenBlacklistRedisService tokenBlacklistRedisService;
    private final EmailVerificationService emailVerificationService;
    private final EmailMasker emailMasker;
    private final AuthMailService authMailService;

    private static final int TEMP_PASSWORD_LENGTH = 10;
    private static final String TEMP_PASSWORD_CHARS =
            "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789!@#$%";
    private static final SecureRandom RANDOM = new SecureRandom();

    @Transactional
    public SignupResponse signupRoot(RootSignupRequest request) {
        validateEmailNotDuplicated(request.email());
        emailVerificationService.validateVerificationToken(request.email(), request.verificationToken());

        Company company = companyRepository.save(Company.create(request.companyName()));

        User user = userRepository.save(User.createRoot(
                request.email(),
                passwordEncoder.encode(request.password()),
                request.name(),
                company
        ));

        return SignupResponse.of(user, company.getJoinKey());
    }

    @Transactional
    public SignupResponse signupMember(MemberSignupRequest request) {
        validateEmailNotDuplicated(request.email());
        emailVerificationService.validateVerificationToken(request.email(), request.verificationToken());

        Company company = companyRepository.findByJoinKey(request.companyKey())
                .orElseThrow(InvalidCompanyKeyException::new);

        User user = userRepository.save(User.createMember(
                request.email(),
                passwordEncoder.encode(request.password()),
                request.name(),
                company
        ));

        return SignupResponse.of(user, company.getJoinKey());
    }

    public LoginResult login(LoginRequest request) {
        UserPrincipal principal;
        try {
            var authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password())
            );
            principal = (UserPrincipal) authentication.getPrincipal();
        } catch (AuthenticationException e) {
            throw new InvalidCredentialsException();
        }

        User user = userRepository.findByIdAndDeletedAtIsNull(principal.getId())
                .orElseThrow(UserNotFoundException::new);

        TokenResponse tokens = issueTokens(user);

        return new LoginResult(LoginResponse.of(user), tokens);
    }

    public void logout(UserPrincipal principal, String accessToken) {
        tokenBlacklistRedisService.blacklist(accessToken);
        refreshTokenRedisService.deleteByUserId(principal.getId());
    }

    public TokenResponse reissue(ReissueRequest request) {
        Claims claims;
        try {
            claims = jwtProvider.parseClaims(request.refreshToken());
        } catch (ExpiredJwtException e) {
            throw new ExpiredTokenException();
        } catch (JwtException | IllegalArgumentException e) {
            throw new InvalidTokenException();
        }

        Long userId = Long.valueOf(claims.getSubject());

        String storedRefreshToken = refreshTokenRedisService.findByUserId(userId)
                .orElseThrow(InvalidTokenException::new);

        if (!storedRefreshToken.equals(request.refreshToken()))
            throw new InvalidTokenException();

        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(UserNotFoundException::new);

        return issueTokens(user);
    }

    // 아이디 찾기 - 회사명 + 사용자 이름이 모두 일치하는 계정의 이메일을 마스킹해서 내려준다.
    @Transactional(readOnly = true)
    public FindIdResponse findId(FindIdRequest request) {
        Company company = companyRepository.findByName(request.companyName())
                .orElseThrow(AccountNotFoundException::new);

        User user = userRepository.findFirstByNameAndCompanyAndDeletedAtIsNullOrderByIdAsc(request.userName(), company)
                .orElseThrow(AccountNotFoundException::new);

        return FindIdResponse.of(emailMasker.mask(user.getEmail()));
    }

    // 비밀번호 찾기 - 임시 비밀번호를 발급해 즉시 저장하고, 가입된 이메일로 발송한다.
    @Transactional
    public FindPasswordResponse findPassword(FindPasswordRequest request) {
        User user = userRepository.findByEmailAndDeletedAtIsNull(request.email())
                .orElseThrow(AccountNotFoundException::new);

        String tempPassword = generateTempPassword();
        user.changePassword(passwordEncoder.encode(tempPassword));

        authMailService.sendTempPassword(user.getEmail(), tempPassword);

        return FindPasswordResponse.of(emailMasker.mask(user.getEmail()));
    }

    private String generateTempPassword() {
        StringBuilder sb = new StringBuilder(TEMP_PASSWORD_LENGTH);
        for (int i = 0; i < TEMP_PASSWORD_LENGTH; i++) {
            sb.append(TEMP_PASSWORD_CHARS.charAt(RANDOM.nextInt(TEMP_PASSWORD_CHARS.length())));
        }
        return sb.toString();
    }

    private TokenResponse issueTokens(User user) {
        String accessToken = jwtProvider.generateAccessToken(user);
        String refreshToken = jwtProvider.generateRefreshToken(user);
        refreshTokenRedisService.save(user.getId(), refreshToken);
        return new TokenResponse(accessToken, refreshToken);
    }


    private void validateEmailNotDuplicated(String email) {
        if (userRepository.existsByEmailAndDeletedAtIsNull(email))
            throw new DuplicateEmailException();
    }
}
