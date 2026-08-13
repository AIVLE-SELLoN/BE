package com.aivle.sellon.rawdb.repository;

import com.aivle.sellon.rawdb.entity.RawProduct;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface RawChannelProductRepository extends JpaRepository<RawProduct, String> {
    List<RawProduct> findByUsersChannelKey(Long usersChannelKey);

    List<RawProduct> findByUsersChannelKeyIn(Collection<Long> usersChannelKeys);

    List<RawProduct> findByUsersChannelKeyAndChannelProductId(Long usersChannelKey, String channelProductId);

    Optional<RawProduct> findByVariantRowId(String variantRowId);
}
