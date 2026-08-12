package com.aivle.sellon.rawdb.repository;

import com.aivle.sellon.rawdb.entity.RawChannelProduct;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RawChannelProductRepository extends JpaRepository<RawChannelProduct, Long> {
    List<RawChannelProduct> findByUsersChannelKey(Long usersChannelKey);

    List<RawChannelProduct> findByUsersChannelKeyAndChannelProductId(Long usersChannelKey, String channelProductId);

    Optional<RawChannelProduct> findByVariantRowId(String variantRowId);
}
