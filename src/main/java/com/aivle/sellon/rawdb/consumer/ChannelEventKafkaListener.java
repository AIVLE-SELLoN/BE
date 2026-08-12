package com.aivle.sellon.rawdb.consumer;

import com.aivle.sellon.rawdb.enums.ChannelEventType;
import com.aivle.sellon.rawdb.service.RawChannelEventIngestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * 채널 비교분석(문의/리뷰/주문/상세변경) 원천 데이터 Kafka 컨슈머.
 * 목 프로듀서 -> Kafka -> 메인서버 구간 전용이며, CSV - Kafka 토픽 매핑 규칙 표를 그대로 반영한다.
 *
 * | CSV                     | 타임스탬프 컬럼  | 토픽                  | 이벤트 타입    |
 * |--------------------------|-----------------|------------------------|---------------|
 * | input_orders.csv         | order_date      | raw.orders             | ORDER         |
 * | input_cs_inquiries.csv   | inquired_at     | raw.inquiries          | INQUIRY       |
 * | input_reviews.csv        | created_at      | raw.reviews            | REVIEW        |
 * | input_detail_changes.csv | changed_at      | raw.detail_changes     | DETAIL_CHANGE |
 *
 * 메시지 키는 공통으로 {channel}:{channel_product_id} 형식.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChannelEventKafkaListener {

    private final RawChannelEventIngestService rawChannelEventIngestService;

    @KafkaListener(topics = "raw.orders", groupId = "${spring.kafka.consumer.group-id}")
    public void onOrder(ConsumerRecord<String, String> record) {
        rawChannelEventIngestService.ingest(ChannelEventType.ORDER, record.topic(), "order_date", record.key(), record.value());
    }

    @KafkaListener(topics = "raw.inquiries", groupId = "${spring.kafka.consumer.group-id}")
    public void onInquiry(ConsumerRecord<String, String> record) {
        rawChannelEventIngestService.ingest(ChannelEventType.INQUIRY, record.topic(), "inquired_at", record.key(), record.value());
    }

    @KafkaListener(topics = "raw.reviews", groupId = "${spring.kafka.consumer.group-id}")
    public void onReview(ConsumerRecord<String, String> record) {
        rawChannelEventIngestService.ingest(ChannelEventType.REVIEW, record.topic(), "created_at", record.key(), record.value());
    }

    @KafkaListener(topics = "raw.detail_changes", groupId = "${spring.kafka.consumer.group-id}")
    public void onDetailChange(ConsumerRecord<String, String> record) {
        rawChannelEventIngestService.ingest(ChannelEventType.DETAIL_CHANGE, record.topic(), "changed_at", record.key(), record.value());
    }
}
