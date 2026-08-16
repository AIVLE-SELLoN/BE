package com.aivle.sellon.rawdb.repository;

import com.aivle.sellon.rawdb.entity.RawReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

public interface RawReviewRepository extends JpaRepository<RawReview, String> {
    @Transactional(transactionManager = "rawDbTransactionManager", readOnly = true)
    List<RawReview> findByChannelId(String channelId);

    @Transactional(transactionManager = "rawDbTransactionManager", readOnly = true)
    long countByChannelIdAndCreatedAtAfter(String channelId, OffsetDateTime createdAt);
}
