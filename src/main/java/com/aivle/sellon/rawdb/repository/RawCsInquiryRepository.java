package com.aivle.sellon.rawdb.repository;

import com.aivle.sellon.rawdb.entity.RawCs;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

public interface RawCsInquiryRepository extends JpaRepository<RawCs, String> {
    @Transactional(transactionManager = "rawDbTransactionManager", readOnly = true)
    long countByChannelIdAndInquiredAtAfter(String channelId, OffsetDateTime inquiredAt);
}
