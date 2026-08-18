package com.aivle.sellon.domain.channels.service.connection;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

/** 네이버 OAuth state(CSRF 방지용 임시 토큰)를 Redis에 TTL로 저장 - 로컬 메모리는 다중 인스턴스/재시작/만료 처리에 취약해서 제외. */
@Service
@RequiredArgsConstructor
public class NaverOAuthStateService {

    private static final String KEY_PREFIX = "naver-oauth-state:";
    private static final Duration STATE_EXPIRE = Duration.ofMinutes(10);

    private final RedisTemplate<String, Object> redisTemplate;

    public void save(String state, Long companyId) {
        redisTemplate.opsForValue().set(key(state), String.valueOf(companyId), STATE_EXPIRE);
    }

    /**
     * state를 조회하고 즉시 삭제한다(1회용 - 재사용 방지).
     */
    public Optional<Long> consume(String state) {
        String redisKey = key(state);
        Object value = redisTemplate.opsForValue().get(redisKey);
        if (value == null) {
            return Optional.empty();
        }
        redisTemplate.delete(redisKey);
        return Optional.of(Long.valueOf((String) value));
    }

    private String key(String state) {
        return KEY_PREFIX + state;
    }
}
