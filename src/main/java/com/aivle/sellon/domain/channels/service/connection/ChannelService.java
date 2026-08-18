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

/** 채널 연동(쿠팡/지그재그=API 키, 네이버=OAuth) 처리 - ROOT 전용, 데이터 주입 이벤트는 발행하지 않음(Mock Producer가 CSV 독립 재생). */
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

    /** (company, channelType) 조합을 upsert 후 조회 - 동시 요청에도 중복 행 없이 하나로 수렴. */
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
