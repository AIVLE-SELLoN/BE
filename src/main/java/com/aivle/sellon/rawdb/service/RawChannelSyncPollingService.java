package com.aivle.sellon.rawdb.service;

import com.aivle.sellon.domain.channels.entity.connection.UsersChannel;
import com.aivle.sellon.domain.channels.enums.ConnectionStatus;
import com.aivle.sellon.domain.channels.repository.connection.UsersChannelRepository;
import com.aivle.sellon.domain.channels.service.synclog.ChannelSyncLogService;
import com.aivle.sellon.rawdb.repository.RawCsRepository;
import com.aivle.sellon.rawdb.repository.RawOrderRepository;
import com.aivle.sellon.rawdb.repository.RawReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;

// 카프카 리스너 대신 raw db(cs/reviews/orders)를 주기적으로 읽기 전용 조회해 "채널 연동 이력"을 남긴다.
@Slf4j
@Service
@RequiredArgsConstructor
public class RawChannelSyncPollingService {

    private final UsersChannelRepository usersChannelRepository;
    private final RawCsRepository rawCsInquiryRepository;
    private final RawReviewRepository rawReviewRepository;
    private final RawOrderRepository rawOrderRepository;
    private final ChannelSyncLogService channelSyncLogService;

    @Scheduled(cron = "${channel.sync.polling-cron}")
    public void pollAll() {
        List<UsersChannel> connectedChannels = usersChannelRepository.findByConnectionStatus(ConnectionStatus.CONNECTED);
        for (UsersChannel usersChannel : connectedChannels) {
            pollOne(usersChannel);
        }
    }

    // 자정 크론을 안 기다리고 즉시 확인하고 싶을 때(수동 트리거) 쓰는 진입점.
    // channelType이 없으면 회사의 연동된 채널 전부(쿠팡/네이버/지그재그)를 한꺼번에 폴링한다.
    public void pollForCompany(Long companyId, String channelType) {
        List<UsersChannel> connectedChannels =
                usersChannelRepository.findByCompany_IdAndConnectionStatus(companyId, ConnectionStatus.CONNECTED);
        for (UsersChannel usersChannel : connectedChannels) {
            if (channelType != null && !channelType.isBlank() && !channelType.equals(usersChannel.getChannelType())) {
                continue;
            }
            pollOne(usersChannel);
        }
    }

    // raw db 조회는 각 레포지토리가 자체 @Transactional(rawDbTransactionManager)을 가지므로 여기선 안 건다.
    public void pollOne(UsersChannel usersChannel) {
        // 첫 폴링이면 지금까지 쌓인 데이터는 세지 않고 이 시점부터 기준을 잡는다.
        OffsetDateTime since = usersChannel.getLastSyncCheckedAt() != null
                ? usersChannel.getLastSyncCheckedAt()
                : OffsetDateTime.now();
        OffsetDateTime checkedAt = OffsetDateTime.now();
        String channelId = usersChannel.getChannelType();

        try {
            long csCount = rawCsInquiryRepository.countByChannelIdAndInquiredAtAfter(channelId, since);
            long reviewCount = rawReviewRepository.countByChannelIdAndCreatedAtAfter(channelId, since);
            long orderCount = rawOrderRepository.countByChannelIdAndCreatedAtAfter(channelId, since);
            long total = csCount + reviewCount + orderCount;

            recordSuccessAndAdvance(usersChannel, checkedAt, total);
            if (total > 0) {
                log.info("[채널 연동 이력] channelId={}, usersChannelKey={} -> cs {}건, reviews {}건, orders {}건 신규 확인",
                        channelId, usersChannel.getUsersChannelKey(), csCount, reviewCount, orderCount);
            }
        } catch (Exception e) {
            log.error("[채널 연동 이력] usersChannelKey={} 폴링 실패", usersChannel.getUsersChannelKey(), e);
            recordFailureAndAdvance(usersChannel, checkedAt, e.getMessage());
        }
    }

    // self-invocation이라 @Transactional이 안 먹어서, save()를 명시적으로 호출해 확실히 저장한다.
    public void recordSuccessAndAdvance(UsersChannel usersChannel, OffsetDateTime checkedAt, long total) {
        if (total > 0) {
            channelSyncLogService.recordSuccess(usersChannel, (int) total);
        }
        usersChannel.updateLastSyncCheckedAt(checkedAt);
        usersChannelRepository.save(usersChannel);
    }

    public void recordFailureAndAdvance(UsersChannel usersChannel, OffsetDateTime checkedAt, String failReason) {
        channelSyncLogService.recordFailure(usersChannel, failReason);
        usersChannel.updateLastSyncCheckedAt(checkedAt);
        usersChannelRepository.save(usersChannel);
    }
}
