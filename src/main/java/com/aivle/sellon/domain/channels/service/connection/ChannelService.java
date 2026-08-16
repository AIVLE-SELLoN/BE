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
