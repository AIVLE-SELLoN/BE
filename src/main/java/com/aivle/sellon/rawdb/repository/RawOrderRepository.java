package com.aivle.sellon.rawdb.repository;

import com.aivle.sellon.rawdb.entity.RawOrder;
import com.aivle.sellon.rawdb.entity.RawOrderId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public interface RawOrderRepository extends JpaRepository<RawOrder, RawOrderId> {
    @Transactional(transactionManager = "rawDbTransactionManager", readOnly = true)
    List<RawOrder> findByChannelId(String channelId);

    @Transactional(transactionManager = "rawDbTransactionManager", readOnly = true)
    long countByChannelIdAndCreatedAtAfter(String channelId, OffsetDateTime createdAt);

    // feat/channel-comparison에서 사용 - 채널 비교 지표(판매량 대비 문의/리뷰율) 계산용
    @Transactional(transactionManager = "rawDbTransactionManager", readOnly = true)
    List<RawOrder> findByChannelIdAndOrderDateGreaterThanEqual(String channelId, LocalDate orderDate);
}
