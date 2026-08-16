package com.aivle.sellon.rawdb.repository;

import com.aivle.sellon.rawdb.entity.RawOrder;
import com.aivle.sellon.rawdb.entity.RawOrderId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface RawOrderRepository extends JpaRepository<RawOrder, RawOrderId> {
    @Transactional(transactionManager = "rawDbTransactionManager", readOnly = true)
    List<RawOrder> findByChannelId(String channelId);
}
