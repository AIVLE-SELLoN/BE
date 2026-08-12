package com.aivle.sellon.domain.channels.service.connection;

import com.aivle.sellon.domain.channels.dto.request.ChannelConnectRequest;
import com.aivle.sellon.domain.channels.dto.response.ChannelConnectionResponse;
import com.aivle.sellon.domain.channels.entity.connection.UsersChannel;
import com.aivle.sellon.domain.channels.enums.ConnectionStatus;
import com.aivle.sellon.domain.channels.exception.connection.ChannelConnectNotAllowedException;
import com.aivle.sellon.domain.channels.repository.connection.UsersChannelRepository;
import com.aivle.sellon.domain.company.entity.Company;
import com.aivle.sellon.domain.company.repository.CompanyRepository;
import com.aivle.sellon.domain.user.enums.Role;
import com.aivle.sellon.global.security.principal.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

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
    private final CompanyRepository companyRepository;
    private final ChannelKeyValidator channelKeyValidator;
    private final MockNaverOAuthClient mockNaverOAuthClient;

    // TODO: Redis 등 외부 저장소로 교체 (인스턴스 재시작 시 유실됨)
    private final Map<String, Long> naverOAuthStateStore = new ConcurrentHashMap<>();

    @Transactional
    public ChannelConnectionResponse connect(UserPrincipal principal, ChannelConnectRequest request) {
        requireRoot(principal);

        if (!channelKeyValidator.validateFormat(request.channelType(), request.channelCode())) {
            return ChannelConnectionResponse.failed(request.channelType(), "채널 키 형식이 올바르지 않습니다.");
        }

        Company company = companyRepository.getReferenceById(principal.getCompanyId());
        UsersChannel usersChannel = usersChannelRepository
                .findByCompany_IdAndChannelType(principal.getCompanyId(), request.channelType())
                .orElseGet(() -> UsersChannel.of(company, request.channelType(), request.channelCode()));
        usersChannel.updateChannelCode(request.channelCode());
        usersChannel.updateStatus(ConnectionStatus.CONNECTED);
        usersChannelRepository.save(usersChannel);

        return ChannelConnectionResponse.from(usersChannel);
    }

    public String naverAuthorize(UserPrincipal principal) {
        requireRoot(principal);

        String state = UUID.randomUUID().toString();
        naverOAuthStateStore.put(state, principal.getCompanyId());
        return mockNaverOAuthClient.buildAuthorizationUrl(state);
    }

    @Transactional
    public ChannelConnectionResponse naverCallback(String code, String state) {
        Long companyId = naverOAuthStateStore.remove(state);
        if (companyId == null) {
            return ChannelConnectionResponse.failed("NAVER", "유효하지 않은 인증 상태입니다.");
        }

        MockNaverOAuthClient.TokenResult tokenResult = mockNaverOAuthClient.exchangeToken(code);
        if (tokenResult == null) {
            return ChannelConnectionResponse.failed("NAVER", "네이버 인증에 실패했습니다.");
        }

        Company company = companyRepository.getReferenceById(companyId);
        UsersChannel usersChannel = usersChannelRepository
                .findByCompany_IdAndChannelType(companyId, "NAVER")
                .orElseGet(() -> UsersChannel.of(company, "NAVER", tokenResult.accountId()));
        usersChannel.updateChannelCode(tokenResult.accountId());
        usersChannel.updateStatus(ConnectionStatus.CONNECTED);
        usersChannelRepository.save(usersChannel);

        return ChannelConnectionResponse.from(usersChannel);
    }

    private void requireRoot(UserPrincipal principal) {
        if (principal.getRole() != Role.ROOT) {
            throw new ChannelConnectNotAllowedException();
        }
    }
}
