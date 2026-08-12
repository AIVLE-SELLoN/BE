package com.aivle.sellon.rawdb.repository;

import com.aivle.sellon.rawdb.dto.ChannelMetricRow;
import com.aivle.sellon.rawdb.entity.RawCs;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

public interface RawCsRepository extends JpaRepository<RawCs, String> {

    @Transactional(transactionManager = "rawDbTransactionManager", readOnly = true)
    @Query("""
            SELECT cs.channelId AS channelId,
                   COUNT(cs) AS count
            FROM RawCs cs
            WHERE cs.inquiredAt >= :fromInclusive
              AND cs.inquiredAt < :toExclusive
            GROUP BY cs.channelId
            ORDER BY cs.channelId
            """)
    List<ChannelMetricRow.CsCount> findCountsByChannel(
            @Param("fromInclusive") OffsetDateTime fromInclusive,
            @Param("toExclusive") OffsetDateTime toExclusive
    );
}
