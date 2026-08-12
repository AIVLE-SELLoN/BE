package com.aivle.sellon.rawdb.service;

import com.aivle.sellon.domain.channels.entity.connection.UsersChannel;
import com.aivle.sellon.domain.channels.repository.connection.UsersChannelRepository;
import com.aivle.sellon.domain.channels.service.synclog.ChannelSyncLogService;
import com.aivle.sellon.rawdb.entity.ChannelProductMapping;
import com.aivle.sellon.rawdb.entity.RawChannelMaster;
import com.aivle.sellon.rawdb.entity.RawCsInquiry;
import com.aivle.sellon.rawdb.entity.RawDetailChange;
import com.aivle.sellon.rawdb.entity.RawOrder;
import com.aivle.sellon.rawdb.entity.RawReview;
import com.aivle.sellon.rawdb.enums.ChannelEventType;
import com.aivle.sellon.rawdb.repository.ChannelProductMappingRepository;
import com.aivle.sellon.rawdb.repository.RawChannelMasterRepository;
import com.aivle.sellon.rawdb.repository.RawCsInquiryRepository;
import com.aivle.sellon.rawdb.repository.RawDetailChangeRepository;
import com.aivle.sellon.rawdb.repository.RawOrderRepository;
import com.aivle.sellon.rawdb.repository.RawReviewRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * raw.orders/raw.inquiries/raw.reviews/raw.detail_changes 4개 토픽 공통 적재 로직.
 *
 * 「Raw DB 스키마 확정 (8/7)」 §1 소유권 반영: main server(우리)가 channel/cs/reviews/orders에
 * 쓰고, AI 노드(classification_worker)가 그걸 폴링해 classified_item(_aspect)에 결과를 쓴다.
 * ORDER/INQUIRY/REVIEW는 확정 테이블(channel/cs/reviews/orders)에 적재하고, DETAIL_CHANGE는
 * input_detail_changes.csv 스키마 확정(2026-08-11)에 맞춰 자체 구조화한 detail_change
 * 테이블에 적재한다(공식 raw db 확정 스키마엔 아직 없어 우리 쪽에서 우선 구조화해 둔 것).
 *
 * 메시지 키({channel}:{channel_product_id})를 파싱하고, CSV별 자연키(inquiry_id/review_id)를
 * item_id로 그대로 쓴다(§5-1 A안) - classification_worker가 이 값으로 classified_item을
 * 조인하므로 여기서 값을 바꾸면 안 된다.
 * 적재 성공/실패는 해당 UsersChannel의 ChannelSyncLog로 기록해 "채널 연동 이력" 화면을 채운다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RawChannelEventIngestService {

    private final RawChannelMasterRepository rawChannelMasterRepository;
    private final RawCsInquiryRepository rawCsInquiryRepository;
    private final RawReviewRepository rawReviewRepository;
    private final RawOrderRepository rawOrderRepository;
    private final RawDetailChangeRepository rawDetailChangeRepository;
    private final ChannelProductMappingRepository channelProductMappingRepository;
    private final UsersChannelRepository usersChannelRepository;
    private final ChannelSyncLogService channelSyncLogService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional("rawDbTransactionManager")
    public void ingest(ChannelEventType eventType, String topic, String timestampField, String key, String payload) {
        if (key == null || !key.contains(":")) {
            log.warn("[{}] 메시지 키 형식이 올바르지 않아 스킵: key={}", topic, key);
            return;
        }
        String[] parts = key.split(":", 2);
        String channel = parts[0];
        String channelProductId = parts[1];

        Long companyId = extractCompanyId(payload);
        if (companyId == null) {
            log.warn("[{}] payload에 company_id가 없어 스킵: key={}", topic, key);
            return;
        }

        // 이 메시지가 어느 UsersChannel(연동) 소속인지 찾는다 - 못 찾으면 동기화 이력을 남길 대상 자체가 없다.
        Optional<UsersChannel> usersChannel = usersChannelRepository.findByCompany_IdAndChannelType(companyId, channel);
        if (usersChannel.isEmpty()) {
            log.warn("[{}] company_id={}, channel={}에 매칭되는 UsersChannel이 없어 스킵", topic, companyId, channel);
            return;
        }

        try {
            ensureChannelMaster(channel);
            switch (eventType) {
                case INQUIRY -> ingestInquiry(channel, channelProductId, timestampField, payload);
                case REVIEW -> ingestReview(channel, channelProductId, timestampField, payload);
                case ORDER -> ingestOrder(channel, channelProductId, timestampField, payload);
                case DETAIL_CHANGE -> ingestDetailChange(channel, channelProductId, timestampField, payload);
            }
            channelSyncLogService.recordSuccess(usersChannel.get(), 1);
        } catch (Exception e) {
            log.error("[{}] 이벤트 적재 실패: key={}", topic, key, e);
            channelSyncLogService.recordFailure(usersChannel.get(), e.getMessage(),
                    eventType.name(), topic, timestampField, key, payload);
            throw e;
        }
    }

    /**
     * §2-1 channel 마스터 보장. cs/reviews/orders의 channel_id가 참조하므로 먼저 있어야 한다.
     */
    private void ensureChannelMaster(String channel) {
        if (!rawChannelMasterRepository.existsById(channel)) {
            rawChannelMasterRepository.save(RawChannelMaster.of(channel));
        }
    }

    private void ingestInquiry(String channel, String channelProductId, String timestampField, String payload) {
        String id = extractString(payload, "inquiry_id");
        if (id == null) {
            throw new IllegalArgumentException("inquiry_id가 payload에 없음");
        }
        String content = extractString(payload, "content");
        LocalDateTime inquiredAt = extractDateTime(payload, timestampField);
        String productGroupId = resolveProductGroupId(channel, channelProductId);
        rawCsInquiryRepository.save(RawCsInquiry.of(
                id, channelProductId, productGroupId, channel, content, inquiredAt, LocalDateTime.now()
        ));
    }

    private void ingestReview(String channel, String channelProductId, String timestampField, String payload) {
        String id = extractString(payload, "review_id");
        if (id == null) {
            throw new IllegalArgumentException("review_id가 payload에 없음");
        }
        String content = extractString(payload, "content");
        Integer rating = extractInt(payload, "rating");
        LocalDateTime createdAt = extractDateTime(payload, timestampField);
        String productGroupId = resolveProductGroupId(channel, channelProductId);
        rawReviewRepository.save(RawReview.of(
                id, channelProductId, productGroupId, channel, content, rating, createdAt
        ));
    }

    private void ingestOrder(String channel, String channelProductId, String timestampField, String payload) {
        LocalDate orderDate = extractDate(payload, timestampField);
        if (orderDate == null) {
            throw new IllegalArgumentException("order_date 파싱 실패");
        }
        Integer quantity = extractInt(payload, "quantity");
        Integer orderAmount = extractInt(payload, "order_amount");
        rawOrderRepository.save(RawOrder.of(
                channel, channelProductId, orderDate,
                quantity == null ? 0 : quantity,
                orderAmount == null ? 0 : orderAmount,
                LocalDateTime.now()
        ));
    }

    /**
     * input_detail_changes.csv 확정 스키마(change_id/changed_field/previous_value/new_value/
     * change_type/changed_at) 그대로 파싱해 적재한다. change_id가 PK라 재생 시에도 덮어써진다.
     */
    private void ingestDetailChange(String channel, String channelProductId, String timestampField, String payload) {
        String changeId = extractString(payload, "change_id");
        if (changeId == null) {
            throw new IllegalArgumentException("change_id가 payload에 없음");
        }
        String changedField = extractString(payload, "changed_field");
        String previousValue = extractString(payload, "previous_value");
        String newValue = extractString(payload, "new_value");
        String changeType = extractString(payload, "change_type");
        LocalDateTime changedAt = extractDateTime(payload, timestampField);
        rawDetailChangeRepository.save(RawDetailChange.of(
                changeId, channel, channelProductId, changedField, previousValue, newValue, changeType, changedAt
        ));
    }

    /**
     * 확정된 상품 매핑이 있으면 product_group_id를 쓰고, 없으면 null로 둔다(AI 워커가
     * channel_product_id로 폴백한다 - classification_worker.py `_to_request_item` 참고).
     */
    private String resolveProductGroupId(String channel, String channelProductId) {
        return channelProductMappingRepository.findByChannelAndChannelProductId(channel, channelProductId).stream()
                .map(ChannelProductMapping::getProductGroupId)
                .filter(java.util.Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    /**
     * 현재는 단일 회사 운영을 상정해 company_id가 payload에 하드코딩되어 온다.
     */
    private Long extractCompanyId(String payload) {
        try {
            JsonNode node = objectMapper.readTree(payload);
            JsonNode value = node.get("company_id");
            return (value == null || value.isNull()) ? null : value.asLong();
        } catch (Exception e) {
            log.warn("company_id 파싱 실패: {}", e.toString());
            return null;
        }
    }

    private String extractString(String payload, String field) {
        try {
            JsonNode node = objectMapper.readTree(payload);
            JsonNode value = node.get(field);
            return (value == null || value.isNull()) ? null : value.asText();
        } catch (Exception e) {
            log.debug("{} 파싱 실패: {}", field, e.toString());
            return null;
        }
    }

    private Integer extractInt(String payload, String field) {
        try {
            JsonNode node = objectMapper.readTree(payload);
            JsonNode value = node.get(field);
            return (value == null || value.isNull()) ? null : value.asInt();
        } catch (Exception e) {
            log.debug("{} 파싱 실패: {}", field, e.toString());
            return null;
        }
    }

    private LocalDateTime extractDateTime(String payload, String timestampField) {
        String raw = extractString(payload, timestampField);
        if (raw == null) {
            return null;
        }
        try {
            return LocalDateTime.parse(raw);
        } catch (Exception e) {
            log.debug("타임스탬프 파싱 실패 (field={}): {}", timestampField, e.toString());
            return null;
        }
    }

    private LocalDate extractDate(String payload, String timestampField) {
        String raw = extractString(payload, timestampField);
        if (raw == null) {
            return null;
        }
        try {
            // order_date는 날짜만(§2-9) - datetime 문자열로 올 수도 있어 앞 10자리만 취한다.
            return LocalDate.parse(raw.length() >= 10 ? raw.substring(0, 10) : raw);
        } catch (Exception e) {
            log.debug("날짜 파싱 실패 (field={}): {}", timestampField, e.toString());
            return null;
        }
    }
}
