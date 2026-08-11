package com.aivle.sellon.domain.auth.service;

import com.aivle.sellon.domain.auth.dto.request.LoginRequest;
import com.aivle.sellon.domain.auth.dto.request.MemberSignupRequest;
import com.aivle.sellon.domain.auth.dto.request.ReissueRequest;
import com.aivle.sellon.domain.auth.dto.request.RootSignupRequest;
import com.aivle.sellon.domain.auth.dto.response.LoginResponse;
import com.aivle.sellon.domain.auth.dto.response.LoginResult;
import com.aivle.sellon.domain.auth.dto.response.SignupResponse;
import com.aivle.sellon.domain.auth.dto.response.TokenResponse;
import com.aivle.sellon.domain.auth.exception.InvalidCredentialsException;
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
