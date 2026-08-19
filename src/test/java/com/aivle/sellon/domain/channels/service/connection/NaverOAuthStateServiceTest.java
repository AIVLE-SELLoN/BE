package com.aivle.sellon.domain.channels.service.connection;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NaverOAuthStateServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private NaverOAuthStateService naverOAuthStateService;

    @Test
    @DisplayName("state 저장 시 접두사가 붙은 키와 TTL 을 함께 Redis 에 기록한다")
    void save_setsValueWithPrefixedKeyAndTtl() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        naverOAuthStateService.save("state-abc", 10L);

        verify(valueOperations).set(eq("naver-oauth-state:state-abc"), eq("10"), any(Duration.class));
    }

    @Test
    @DisplayName("존재하지 않는(또는 만료된) state 를 consume 하면 빈 Optional 을 반환한다")
    void consume_missingState_returnsEmpty() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("naver-oauth-state:missing")).thenReturn(null);

        Optional<Long> result = naverOAuthStateService.consume("missing");

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("존재하는 state 를 consume 하면 companyId 를 반환하고 즉시 삭제해 재사용을 막는다")
    void consume_existingState_returnsCompanyIdAndDeletesForOneTimeUse() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("naver-oauth-state:state-abc")).thenReturn("10");

        Optional<Long> result = naverOAuthStateService.consume("state-abc");

        assertEquals(10L, result.orElseThrow());
        verify(redisTemplate).delete("naver-oauth-state:state-abc");
    }
}
