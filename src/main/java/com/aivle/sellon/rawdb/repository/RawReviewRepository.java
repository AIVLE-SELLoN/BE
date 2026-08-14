package com.aivle.sellon.rawdb.repository;

import com.aivle.sellon.rawdb.entity.RawReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

public interface RawReviewRepository extends JpaRepository<RawReview, String> {
    @Transactional(transactionManager = "rawDbTransactionManager", readOnly = true)
    List<RawReview> findByChannelId(String channelId);

    @Transactional(transactionManager = "rawDbTransactionManager", readOnly = true)
    long countByChannelIdAndCreatedAtAfter(String channelId, OffsetDateTime createdAt);

    // 상품 매핑 소급 반영 계약 ①/④: (channel, channelProductId) 기준 과거 reviews 행 전체를 소급 갱신 (기간 제한 없음)
    @Modifying
    @Query("UPDATE RawReview r SET r.productGroupId = :productGroupId " +
            "WHERE r.channelId = :channel AND r.channelProductId = :channelProductId")
    int updateProductGroupIdByChannelAndChannelProductId(@Param("channel") String channel,
                                                          @Param("channelProductId") String channelProductId,
                                                          @Param("productGroupId") String productGroupId);
}
