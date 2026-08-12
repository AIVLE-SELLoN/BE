package com.aivle.sellon.rawdb.service;

import com.aivle.sellon.domain.channels.entity.synclog.ChannelSyncLog;
import com.aivle.sellon.domain.channels.enums.SyncStatus;
import com.aivle.sellon.domain.channels.exception.ChannelAccessDeniedException;
import com.aivle.sellon.domain.channels.exception.synclog.ChannelSyncLogNotFoundException;
import com.aivle.sellon.domain.channels.exception.synclog.ChannelSyncLogNotRetryableException;
import com.aivle.sellon.domain.channels.repository.synclog.ChannelSyncLogRepository;
import com.aivle.sellon.rawdb.enums.ChannelEventType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 실패한 채널 이벤트(dead-letter로 저장해둔 원본 Kafka 메시지)를 다시 처리한다.
 * Mock Producer/Kafka를 다시 거치지 않고, 실패 시점에 ChannelSyncLog에 같이 저장해둔
 * topic/key/payload를 그대로 RawChannelEventIngestService.ingest()에 재호출한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RawChannelEventRetryService {

    private final ChannelSyncLogRepository channelSyncLogRepository;
    private final RawChannelEventIngestService rawChannelEventIngestService;

    @Transactional
    public void retry(Long companyId, Long syncLogKey) {
        ChannelSyncLog syncLog = channelSyncLogRepository.findById(syncLogKey)
                .orElseThrow(ChannelSyncLogNotFoundException::new);

        if (!syncLog.getUsersChannel().getCompany().getId().equals(companyId)) {
            throw new ChannelAccessDeniedException();
        }
        if (syncLog.getStatus() != SyncStatus.FAILED || syncLog.getPayload() == null) {
            throw new ChannelSyncLogNotRetryableException();
        }

        log.info("[sync retry] syncLogKey={} usersChannelKey={} eventType={}",
                syncLogKey, syncLog.getUsersChannel().getUsersChannelKey(), syncLog.getEventType());

        rawChannelEventIngestService.ingest(
                ChannelEventType.valueOf(syncLog.getEventType()),
                syncLog.getTopic(),
                syncLog.getTimestampField(),
                syncLog.getMessageKey(),
                syncLog.getPayload()
        );
    }
}
