package com.aivle.sellon.rawdb.repository;

import com.aivle.sellon.rawdb.dto.ChannelMetricRow;
import com.aivle.sellon.rawdb.entity.RawOrder;
import com.aivle.sellon.rawdb.entity.RawOrderId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

public interface RawOrderRepository extends JpaRepository<RawOrder, RawOrderId> {

    @Transactional(transactionManager = "rawDbTransactionManager", readOnly = true)
    @Query("""
            SELECT orders.channelId AS channelId,
                   SUM(orders.quantity) AS totalQuantity
            FROM RawOrder orders
            WHERE orders.orderDate >= :fromInclusive
              AND orders.orderDate < :toExclusive
            GROUP BY orders.channelId
            ORDER BY orders.channelId
            """)
    List<ChannelMetricRow.OrderQuantity> findTotalQuantitiesByChannel(
            @Param("fromInclusive") LocalDate fromInclusive,
            @Param("toExclusive") LocalDate toExclusive
    );
}
