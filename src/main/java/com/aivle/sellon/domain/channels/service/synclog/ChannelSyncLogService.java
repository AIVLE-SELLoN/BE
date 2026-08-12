package com.aivle.sellon.domain.channels.service.synclog;

import com.aivle.sellon.domain.channels.dto.response.ChannelSyncLogResponse;
import com.aivle.sellon.domain.channels.entity.synclog.ChannelSyncLog;
import com.aivle.sellon.domain.channels.entity.connection.UsersChannel;
import com.aivle.sellon.domain.channels.repository.synclog.ChannelSyncLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChannelSyncLogService {

    private final ChannelSyncLogRepository channelSyncLogRepository;

    @Transactional(readOnly = true)
    public Page<ChannelSyncLogResponse> getSyncLogs(Long companyId, String channelType, Pageable pageable) {
        Page<ChannelSyncLog> logs = (channelType != null && !channelType.isBlank())
                ? channelSyncLogRepository.findByUsersChannel_Company_IdAndUsersChannel_ChannelTypeOrderByCreatedDateDesc(
                        companyId, channelType, pageable)
                : channelSyncLogRepository.findByUsersChannel_Company_IdOrderByCreatedDateDesc(companyId, pageable);
        return logs.map(ChannelSyncLogResponse::from);
    }

    @Transactional
    public void recordSuccess(UsersChannel usersChannel, Integer syncedCount) {
        channelSyncLogRepository.save(ChannelSyncLog.success(usersChannel, syncedCount));
    }

    @Transactional
    public void recordFailure(UsersChannel usersChannel, String failReason) {
        channelSyncLogRepository.save(ChannelSyncLog.failure(usersChannel, failReason));
    }

    /**
     * 재시도(dead-letter)용 원본 Kafka 메시지까지 같이 저장하는 버전.
     * RawChannelEventRetryService가 이 값을 그대로 다시 읽어 ingest()를 재호출한다.
     */
    @Transactional
    public void recordFailure(UsersChannel usersChannel, String failReason,
                               String eventType, String topic, String timestampField,
                               String messageKey, String payload) {
        channelSyncLogRepository.save(ChannelSyncLog.failure(
                usersChannel, failReason, eventType, topic, timestampField, messageKey, payload));
    }
}
