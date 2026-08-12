package com.aivle.sellon.rawdb.repository;

import com.aivle.sellon.rawdb.entity.RawChannelEvent;
import com.aivle.sellon.rawdb.enums.ChannelEventType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RawChannelEventRepository extends JpaRepository<RawChannelEvent, Long> {
    List<RawChannelEvent> findByEventTypeAndChannelAndChannelProductId(
            ChannelEventType eventType, String channel, String channelProductId);

    List<RawChannelEvent> findByEventTypeAndChannel(ChannelEventType eventType, String channel);

    /**
     * 채널 비교분석 집계용 - 회사+채널+이벤트타입 단위로 원본 이벤트 전체 조회.
     */
    List<RawChannelEvent> findByCompanyIdAndChannelAndEventType(Long companyId, String channel, ChannelEventType eventType);
}
