package com.aivle.sellon.domain.channels.service.connection;

import com.aivle.sellon.domain.channels.dto.request.ChannelConnectRequest;
import com.aivle.sellon.domain.channels.dto.response.ChannelConnectionResponse;
import com.aivle.sellon.domain.channels.entity.connection.UsersChannel;
import com.aivle.sellon.domain.channels.exception.connection.ChannelConnectNotAllowedException;
import com.aivle.sellon.domain.channels.exception.connection.ChannelKeyFormatInvalidException;
import com.aivle.sellon.domain.channels.exception.connection.NaverOAuthFailedException;
import com.aivle.sellon.domain.channels.exception.connection.NaverOAuthStateInvalidException;
import com.aivle.sellon.domain.channels.repository.connection.UsersChannelRepository;
import com.aivle.sellon.domain.user.enums.Role;
import com.aivle.sellon.global.security.principal.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 채널 연동(쿠팡/지그재그 = API 키, 네이버 = OAuth) 처리.
 * 실제 외부 API 호출이 아니라 Mock 데이터 기준으로 동작한다.
 * 채널 연동은 회사 리소스이지만, 연동 수행 자체는 ROOT 계정만 가능하다.
 * NOTE: 여기서 채널연동 완료 시 별도로 데이터 주입 이벤트를 발행하지 않는다 —
 * Mock Producer가 CSV를 독립적으로 재생하는 구조라 우리 쪽은 그 결과를 받는 소비자(consumer) 역할이기 때문.
 */
@Service
@RequiredArgsConstructor
public class ChannelService {

    private final UsersChannelRepository usersChannelRepository;
    private final ChannelKeyValidator channelKeyValidator;
    private final MockNaverOAuthClient mockNaverOAuthClient;
    private final NaverOAuthStateService naverOAuthStateService;

    @Transactional
    public ChannelConnectionResponse connect(UserPrincipal principal, ChannelConnectRequest request) {
        requireRoot(principal);

        if (!channelKeyValidator.validateFormat(request.channelType(), request.channelCode())) {
            throw new ChannelKeyFormatInvalidException();
        }

        UsersChannel usersChannel = findOrCreate(principal.getCompanyId(), request.channelType(), request.channelCode());
        return ChannelConnectionResponse.from(usersChannel);
    }

    public String naverAuthorize(UserPrincipal principal) {
        requireRoot(principal);

        String state = UUID.randomUUID().toString();
        naverOAuthStateService.save(state, principal.getCompanyId());
        return mockNaverOAuthClient.buildAuthorizationUrl(state);
    }

    @Transactional
    public ChannelConnectionResponse naverCallback(String code, String state) {
        Long companyId = naverOAuthStateService.consume(state)
                .orElseThrow(NaverOAuthStateInvalidException::new);

        MockNaverOAuthClient.TokenResult tokenResult = mockNaverOAuthClient.exchangeToken(code);
        if (tokenResult == null) {
            throw new NaverOAuthFailedException();
        }

        UsersChannel usersChannel = findOrCreate(companyId, "NAVER", tokenResult.accountId());
        return ChannelConnectionResponse.from(usersChannel);
    }

    /**
     * (company, channelType) 조합을 INSERT ... ON CONFLICT로 원자적으로 upsert한 뒤 조회한다.
     * PostgreSQL 레벨에서 한 문장으로 처리되기 때문에, 동시에 같은 조합으로 연동 요청이 들어와도
     * 중복 행이 생기거나 트랜잭션이 깨지는 일 없이 항상 하나의 행으로 수렴한다.
     */
    private UsersChannel findOrCreate(Long companyId, String channelType, String channelCode) {
        usersChannelRepository.upsertConnected(companyId, channelType, channelCode);
        return usersChannelRepository.findByCompany_IdAndChannelType(companyId, channelType)
                .orElseThrow();
    }

    private void requireRoot(UserPrincipal principal) {
        if (principal.getRole() != Role.ROOT) {
            throw new ChannelConnectNotAllowedException();
        }
    }
}
