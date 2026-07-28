package com.aivle.sellon.global.security.jwt;

import com.aivle.sellon.domain.user.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;

@Component
public class JwtProvider {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_COMPANY_ID = "companyId";

    @Value("${spring.jwt.secret}")
    private String secret;

    @Value("${spring.jwt.tokens.expire.access_token}")
    private long accessTokenExpireMs;

    @Value("${spring.jwt.tokens.expire.refresh_token}")
    private long refreshTokenExpireMs;

    private SecretKey key;

    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(Base64.getDecoder().decode(secret));
    }

    public String generateAccessToken(User user) {
        Date now = new Date();
        return Jwts.builder()
                .subject(user.getId().toString())
                .claim(CLAIM_EMAIL, user.getEmail())
                .claim(CLAIM_ROLE, user.getRole().name())
                .claim(CLAIM_COMPANY_ID, user.getCompany().getId())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + accessTokenExpireMs))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    public String generateRefreshToken(User user) {
        Date now = new Date();
        return Jwts.builder()
                .subject(user.getId().toString())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + refreshTokenExpireMs))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public long getRemainingValidity(String token) {
        try {
            Date expiration = parseClaims(token).getExpiration();
            return expiration.getTime() - System.currentTimeMillis();
        } catch (JwtException | IllegalArgumentException e) {
            return 0L;
        }
    }

    public String resolveToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || header.isBlank())
            return null;

        if (header.startsWith(BEARER_PREFIX))
            return header.substring(BEARER_PREFIX.length());

        return header;
    }
}
