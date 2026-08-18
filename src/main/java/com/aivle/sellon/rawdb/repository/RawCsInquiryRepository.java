package com.aivle.sellon.rawdb.repository;

import com.aivle.sellon.rawdb.entity.RawCs;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

public interface RawCsInquiryRepository extends JpaRepository<RawCs, String> {
    @Transactional(transactionManager = "rawDbTransactionManager", readOnly = true)
    long countByChannelIdAndInquiredAtAfter(String channelId, OffsetDateTime inquiredAt);

    // 상품 매핑 소급 반영 계약 ①/④: (channel, channelProductId) 기준 과거 cs 행 전체를 소급 갱신 (기간 제한 없음)
    @Modifying
    @Query("UPDATE RawCs c SET c.productGroupId = :productGroupId " +
            "WHERE c.channelId = :channel AND c.channelProductId = :channelProductId")
    int updateProductGroupIdByChannelAndChannelProductId(@Param("channel") String channel,
                                                          @Param("channelProductId") String channelProductId,
                                                          @Param("productGroupId") String productGroupId);
}
