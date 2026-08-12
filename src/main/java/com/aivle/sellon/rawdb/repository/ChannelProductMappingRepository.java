package com.aivle.sellon.rawdb.repository;

import com.aivle.sellon.rawdb.entity.ChannelProductMapping;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChannelProductMappingRepository extends JpaRepository<ChannelProductMapping, Long> {
    Optional<ChannelProductMapping> findByVariantRowId(String variantRowId);

    List<ChannelProductMapping> findByChannelAndChannelProductId(String channel, String channelProductId);

    List<ChannelProductMapping> findByProductGroupIdIsNull();
}
