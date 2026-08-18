package com.aivle.sellon.domain.channels.service.connection;

import com.aivle.sellon.domain.channels.dto.request.ChannelConnectRequest;
import com.aivle.sellon.domain.channels.dto.response.ChannelConnectionResponse;
import com.aivle.sellon.domain.channels.entity.connection.UsersChannel;
import com.aivle.sellon.domain.channels.enums.ConnectionStatus;
import com.aivle.sellon.domain.channels.exception.connection.ChannelConnectNotAllowedException;
import com.aivle.sellon.domain.channels.exception.connection.ChannelKeyFormatInvalidException;
import com.aivle.sellon.domain.channels.exception.connection.NaverOAuthFailedException;
import com.aivle.sellon.domain.channels.exception.connection.NaverOAuthStateInvalidException;
import com.aivle.sellon.domain.channels.repository.connection.UsersChannelRepository;
import com.aivle.sellon.domain.company.entity.Company;
import com.aivle.sellon.domain.user.enums.Role;
import com.aivle.sellon.global.security.principal.UserPrincipal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChannelServiceTest {

    @Mock
    private UsersChannelRepository usersChannelRepository;

    @Mock
    private ChannelKeyValidator channelKeyValidator;

    @Mock
    private MockNaverOAuthClient mockNaverOAuthClient;

    @Mock
    private NaverOAuthStateService naverOAuthStateService;

    @InjectMocks
    private ChannelService channelService;

    private UsersChannel existingUsersChannel(Long companyId, String channelType, String channelCode) {
        Company company = Company.create("마르디 메크르디");
        ReflectionTestUtils.setField(company, "id", companyId);
        UsersChannel usersChannel = UsersChannel.of(company, channelType, channelCode);
        ReflectionTestUtils.setField(usersChannel, "usersChannelKey", 1L);
        usersChannel.updateStatus(ConnectionStatus.CONNECTED);
        return usersChannel;
    }

    @Test
    @DisplayName("MEMBER 가 채널 연동을 요청하면 ChannelConnectNotAllowedException 이 발생한다")
    void connect_member_throwsChannelConnectNotAllowedException() {
        UserPrincipal principal = UserPrincipal.ofClaims(1L, "member@example.com", Role.MEMBER, 10L);
        ChannelConnectRequest request = new ChannelConnectRequest("COUPANG", "cp_live_abcdefghijkl");

        assertThrows(ChannelConnectNotAllowedException.class, () -> channelService.connect(principal, request));
    }

    @Test
    @DisplayName("ROOT 가 잘못된 형식의 채널 키로 연동을 요청하면 ChannelKeyFormatInvalidException 이 발생하고 저장을 시도하지 않는다")
    void connect_invalidKeyFormat_throwsChannelKeyFormatInvalidException() {
        UserPrincipal principal = UserPrincipal.ofClaims(1L, "root@example.com", Role.ROOT, 10L);
        ChannelConnectRequest request = new ChannelConnectRequest("COUPANG", "invalid-key");
        when(channelKeyValidator.validateFormat("COUPANG", "invalid-key")).thenReturn(false);

        assertThrows(ChannelKeyFormatInvalidException.class, () -> channelService.connect(principal, request));
        verify(usersChannelRepository, never()).upsertConnected(anyLong(), anyString(), anyString());
    }

    @Test
    @DisplayName("ROOT 가 유효한 채널 키로 연동하면 upsertConnected 를 호출하고 저장된 값을 응답한다")
    void connect_root_validKey_upsertsAndReturnsResponse() {
        UserPrincipal principal = UserPrincipal.ofClaims(1L, "root@example.com", Role.ROOT, 10L);
        ChannelConnectRequest request = new ChannelConnectRequest("COUPANG", "cp_live_abcdefghijkl");
        when(channelKeyValidator.validateFormat("COUPANG", "cp_live_abcdefghijkl")).thenReturn(true);
        when(usersChannelRepository.findByCompany_IdAndChannelType(10L, "COUPANG"))
                .thenReturn(Optional.of(existingUsersChannel(10L, "COUPANG", "cp_live_abcdefghijkl")));

        ChannelConnectionResponse response = channelService.connect(principal, request);

        verify(usersChannelRepository).upsertConnected(10L, "COUPANG", "cp_live_abcdefghijkl");
        assertEquals("COUPANG", response.channelType());
        assertEquals(ConnectionStatus.CONNECTED, response.connectionStatus());
    }

    @Test
    @DisplayName("connect 는 조회 후 조건부 save 대신 원자적 upsert 쿼리만 사용한다 (동시 요청 시 중복 행/트랜잭션 abort 방지)")
    void connect_usesAtomicUpsert_notReadThenSave() {
        UserPrincipal principal = UserPrincipal.ofClaims(1L, "root@example.com", Role.ROOT, 10L);
        ChannelConnectRequest request = new ChannelConnectRequest("COUPANG", "cp_live_abcdefghijkl");
        when(channelKeyValidator.validateFormat(anyString(), anyString())).thenReturn(true);
        when(usersChannelRepository.findByCompany_IdAndChannelType(10L, "COUPANG"))
                .thenReturn(Optional.of(existingUsersChannel(10L, "COUPANG", "cp_live_abcdefghijkl")));

        channelService.connect(principal, request);

        verify(usersChannelRepository, times(1)).upsertConnected(anyLong(), anyString(), anyString());
        verify(usersChannelRepository, never()).save(any());
    }

    @Test
    @DisplayName("같은 회사·채널 조합으로 두 요청이 동시에 들어와도 둘 다 예외 없이 응답한다 (upsert 멱등성 - 실제 DB 원자성 자체는 unit test 범위 밖)")
    void connect_concurrentRequests_bothSucceedWithoutException() throws InterruptedException {
        Long companyId = 10L;
        String channelType = "COUPANG";
        String channelCode = "cp_live_abcdefghijkl";
        UserPrincipal principal = UserPrincipal.ofClaims(1L, "root@example.com", Role.ROOT, companyId);
        ChannelConnectRequest request = new ChannelConnectRequest(channelType, channelCode);
        when(channelKeyValidator.validateFormat(channelType, channelCode)).thenReturn(true);
        // 실제 DB에서는 INSERT ... ON CONFLICT가 동시에 들어와도 항상 하나의 행으로 수렴한다.
        // 여기서는 그 결과(항상 같은 행이 조회됨)를 mock으로 재현해, 서비스 로직이 예외 없이
        // 정상 응답을 만들어내는지만 검증한다.
        when(usersChannelRepository.findByCompany_IdAndChannelType(companyId, channelType))
                .thenReturn(Optional.of(existingUsersChannel(companyId, channelType, channelCode)));

        CountDownLatch latch = new CountDownLatch(2);
        AtomicInteger successCount = new AtomicInteger();

        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            for (int i = 0; i < 2; i++) {
                executor.submit(() -> {
                    try {
                        channelService.connect(principal, request);
                        successCount.incrementAndGet();
                    } finally {
                        latch.countDown();
                    }
                });
            }
            latch.await();
        }

        assertEquals(2, successCount.get());
        verify(usersChannelRepository, times(2)).upsertConnected(companyId, channelType, channelCode);
    }

    @Test
    @DisplayName("MEMBER 가 네이버 인가 URL을 요청하면 ChannelConnectNotAllowedException 이 발생한다")
    void naverAuthorize_member_throwsChannelConnectNotAllowedException() {
        UserPrincipal principal = UserPrincipal.ofClaims(1L, "member@example.com", Role.MEMBER, 10L);

        assertThrows(ChannelConnectNotAllowedException.class, () -> channelService.naverAuthorize(principal));
    }

    @Test
    @DisplayName("ROOT 가 네이버 인가 URL을 요청하면 state 를 저장하고 인가 URL을 반환한다")
    void naverAuthorize_root_savesStateAndReturnsUrl() {
        UserPrincipal principal = UserPrincipal.ofClaims(1L, "root@example.com", Role.ROOT, 10L);
        when(mockNaverOAuthClient.buildAuthorizationUrl(anyString()))
                .thenReturn("https://mock-naver-oauth.local/authorize?state=abc");

        var response = channelService.naverAuthorize(principal);

        assertEquals("https://mock-naver-oauth.local/authorize?state=abc", response.authorizationUrl());
        verify(naverOAuthStateService).save(anyString(), eq(10L));
    }

    @Test
    @DisplayName("유효하지 않은 state 로 콜백이 오면 NaverOAuthStateInvalidException 이 발생한다")
    void naverCallback_invalidState_throwsNaverOAuthStateInvalidException() {
        when(naverOAuthStateService.consume("bad-state")).thenReturn(Optional.empty());

        assertThrows(NaverOAuthStateInvalidException.class,
                () -> channelService.naverCallback("code", "bad-state"));
    }

    @Test
    @DisplayName("state 는 유효하지만 토큰 교환에 실패하면 NaverOAuthFailedException 이 발생한다")
    void naverCallback_exchangeTokenFails_throwsNaverOAuthFailedException() {
        when(naverOAuthStateService.consume("state")).thenReturn(Optional.of(10L));
        when(mockNaverOAuthClient.exchangeToken("bad-code")).thenReturn(null);

        assertThrows(NaverOAuthFailedException.class,
                () -> channelService.naverCallback("bad-code", "state"));
    }

    @Test
    @DisplayName("정상적인 네이버 콜백이면 계정 식별자로 upsert 하고 연동 결과를 응답한다")
    void naverCallback_success_upsertsAndReturnsResponse() {
        when(naverOAuthStateService.consume("state")).thenReturn(Optional.of(10L));
        when(mockNaverOAuthClient.exchangeToken("code"))
                .thenReturn(new MockNaverOAuthClient.TokenResult("mock-access-token", "naver-account-code"));
        when(usersChannelRepository.findByCompany_IdAndChannelType(10L, "NAVER"))
                .thenReturn(Optional.of(existingUsersChannel(10L, "NAVER", "naver-account-code")));

        ChannelConnectionResponse response = channelService.naverCallback("code", "state");

        verify(usersChannelRepository).upsertConnected(10L, "NAVER", "naver-account-code");
        assertEquals("NAVER", response.channelType());
    }
}
