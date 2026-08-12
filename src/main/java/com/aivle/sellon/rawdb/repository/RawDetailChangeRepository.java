package com.aivle.sellon.rawdb.repository;

import com.aivle.sellon.rawdb.entity.RawDetailChange;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RawDetailChangeRepository extends JpaRepository<RawDetailChange, String> {
    List<RawDetailChange> findByChannelIdAndChannelProductId(String channelId, String channelProductId);
}
