package com.aivle.sellon.rawdb.repository;

import com.aivle.sellon.rawdb.entity.RawOrder;
import com.aivle.sellon.rawdb.entity.RawOrderId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RawOrderRepository extends JpaRepository<RawOrder, RawOrderId> {
    List<RawOrder> findByChannelId(String channelId);
}
