package com.aivle.sellon.rawdb.repository;

import com.aivle.sellon.rawdb.entity.RawMappedData;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface RawMappedDataRepository extends JpaRepository<RawMappedData, Long> {
    Optional<RawMappedData> findByVariantRowId(String variantRowId);

    List<RawMappedData> findByVariantRowIdIn(Collection<String> variantRowIds);

    List<RawMappedData> findByChannelAndChannelProductId(String channel, String channelProductId);

    List<RawMappedData> findByProductGroupIdIsNull();
}
