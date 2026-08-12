package com.aivle.sellon.rawdb.entity;

import com.aivle.sellon.rawdb.enums.ChannelEventType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 채널 비교분석용 Kafka 유입 원본 랜딩 테이블.
 * raw.orders / raw.inquiries / raw.reviews / raw.detail_changes 4개 토픽을 공통 구조로 받는다.
 * 각 CSV(input_orders/input_cs_inquiries/input_reviews/input_detail_changes)의 정확한 컬럼 스펙이
 * 아직 확정되지 않아, 우선 원본 payload를 그대로 보존(rawPayload)하고 메시지 키/타임스탬프 컬럼처럼
 * 매핑 규칙 표에서 확인된 필드만 별도 컬럼으로 뽑아둔다. 세부 필드 파싱은 스펙 확정 후 추가.
 */
@Entity
@Table(name = "raw_channel_event")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class RawChannelEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "raw_channel_event_id")
    private Long id;

    /**
     * 현재는 단일 회사 운영을 상정해 메시지 payload에 company_id가 하드코딩되어 온다.
     * 멀티 테넌트로 확장되면 이 값 기준으로 UsersChannel/company를 구분하게 된다.
     */
    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private ChannelEventType eventType;

    @Column(name = "kafka_topic", nullable = false)
    private String kafkaTopic;

    /**
     * 카프카 메시지 키({channel}:{channel_product_id})에서 분리한 값.
     */
    @Column(name = "channel", nullable = false)
    private String channel;

    @Column(name = "channel_product_id", nullable = false)
    private String channelProductId;

    /**
     * 원본 CSV의 타임스탬프 컬럼(order_date/inquired_at/created_at/changed_at) 파싱 결과.
     * payload에 없거나 파싱 실패 시 null - 이 경우 ingestedAt으로만 순서를 판단.
     */
    @Column(name = "occurred_at")
    private LocalDateTime occurredAt;

    @Lob
    @Column(name = "raw_payload", nullable = false)
    private String rawPayload;

    @CreatedDate
    @Column(name = "ingested_at", nullable = false, updatable = false)
    private LocalDateTime ingestedAt;

    public static RawChannelEvent of(Long companyId, ChannelEventType eventType, String kafkaTopic, String channel,
                                      String channelProductId, LocalDateTime occurredAt, String rawPayload) {
        RawChannelEvent entity = new RawChannelEvent();
        entity.companyId = companyId;
        entity.eventType = eventType;
        entity.kafkaTopic = kafkaTopic;
        entity.channel = channel;
        entity.channelProductId = channelProductId;
        entity.occurredAt = occurredAt;
        entity.rawPayload = rawPayload;
        return entity;
    }
}
