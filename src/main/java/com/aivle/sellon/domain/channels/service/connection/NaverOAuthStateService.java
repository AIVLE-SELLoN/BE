package com.aivle.sellon.domain.channels.service.connection;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

/**
 * 네이버 OAuth state(CSRF 방지용 임시 토큰)를 Redis에 저장.
 * 인스턴스 로컬 메모리(ConcurrentHashMap)에 두면 여러 인스턴스 환경에서 authorize와 callback이
 * 서로 다른 인스턴스로 라우팅될 때 실패하고, 서버가 재시작되면 진행 중이던 인증이 전부 끊긴다.
 * 또한 콜백이 끝내 오지 않은 state가 영원히 안 지워지는 문제(메모리 누수 + 만료되지 않는 토큰)도
 * 있어서, 이미 다른 곳(RefreshTokenRedisService 등)에서 쓰고 있는 Redis에 TTL을 걸어 저장한다.
 */
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
