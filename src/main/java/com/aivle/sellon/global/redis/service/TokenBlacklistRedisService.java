package com.aivle.sellon.global.redis.service;

import com.aivle.sellon.global.security.jwt.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class TokenBlacklistRedisService {

    private static final String KEY_PREFIX = "blacklist:access-token:";
    private static final String BLACKLISTED_VALUE = "true";

    private final RedisTemplate<String, Object> redisTemplate;
    private final JwtProvider jwtProvider;

    public void blacklist(String accessToken) {
        long remainingMs = jwtProvider.getRemainingValidity(accessToken);
        if (remainingMs > 0)
            redisTemplate.opsForValue().set(key(accessToken), BLACKLISTED_VALUE, Duration.ofMillis(remainingMs));
    }

    public boolean isBlacklisted(String accessToken) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key(accessToken)));
    }

    private String key(String accessToken) {
        return KEY_PREFIX + accessToken;
    }
}
