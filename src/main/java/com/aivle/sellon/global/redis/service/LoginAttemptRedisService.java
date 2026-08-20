package com.aivle.sellon.global.redis.service;

import com.aivle.sellon.domain.auth.exception.AccountLockedException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class LoginAttemptRedisService {

    private static final String ATTEMPT_KEY_PREFIX = "login-attempt:";
    private static final String LOCK_KEY_PREFIX = "login-lock:";
    private static final String LOCKED_VALUE = "true";

    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${sellon.auth.login-lock.max-attempts}")
    private int maxAttempts;

    @Value("${sellon.auth.login-lock.attempt-window}")
    private long attemptWindowMs;

    @Value("${sellon.auth.login-lock.lock-duration}")
    private long lockDurationMs;

    public void assertNotLocked(String email) {
        Long ttlSeconds = redisTemplate.getExpire(lockKey(email), TimeUnit.SECONDS);
        if (ttlSeconds != null && ttlSeconds > 0)
            throw new AccountLockedException(ttlSeconds);
    }

    public void recordFailure(String email) {
        String key = attemptKey(email);
        Long attempts = redisTemplate.opsForValue().increment(key);
        if (attempts == null) return;

        if (attempts == 1L)
            redisTemplate.expire(key, Duration.ofMillis(attemptWindowMs));

        if (attempts >= maxAttempts) {
            redisTemplate.opsForValue().set(lockKey(email), LOCKED_VALUE, Duration.ofMillis(lockDurationMs));
            redisTemplate.delete(key);
        }
    }

    public void recordSuccess(String email) {
        redisTemplate.delete(attemptKey(email));
    }

    private String attemptKey(String email) {
        return ATTEMPT_KEY_PREFIX + normalize(email);
    }

    private String lockKey(String email) {
        return LOCK_KEY_PREFIX + normalize(email);
    }

    private String normalize(String email) {
        return email.trim().toLowerCase();
    }
}
