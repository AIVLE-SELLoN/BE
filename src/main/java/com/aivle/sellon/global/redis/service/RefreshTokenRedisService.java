package com.aivle.sellon.global.redis.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RefreshTokenRedisService {

    private static final String KEY_PREFIX = "refresh-token:";

    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${spring.jwt.tokens.expire.refresh_token}")
    private long refreshTokenExpireMs;

    public void save(Long userId, String refreshToken) {
        redisTemplate.opsForValue().set(key(userId), refreshToken, Duration.ofMillis(refreshTokenExpireMs));
    }

    public Optional<String> findByUserId(Long userId) {
        Object value = redisTemplate.opsForValue().get(key(userId));
        return Optional.ofNullable((String) value);
    }

    public void deleteByUserId(Long userId) {
        redisTemplate.delete(key(userId));
    }

    private String key(Long userId) {
        return KEY_PREFIX + userId;
    }
}
