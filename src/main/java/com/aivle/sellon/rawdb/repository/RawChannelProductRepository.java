package com.aivle.sellon.rawdb.repository;

import com.aivle.sellon.rawdb.entity.RawProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface RawChannelProductRepository extends JpaRepository<RawProduct, String> {
    @Transactional(transactionManager = "rawDbTransactionManager", readOnly = true)
    List<RawProduct> findByUsersChannelKey(Long usersChannelKey);

    @Transactional(transactionManager = "rawDbTransactionManager", readOnly = true)
    List<RawProduct> findByUsersChannelKeyIn(Collection<Long> usersChannelKeys);

    @Transactional(transactionManager = "rawDbTransactionManager", readOnly = true)
    List<RawProduct> findByUsersChannelKeyAndChannelProductId(Long usersChannelKey, String channelProductId);

    @Transactional(transactionManager = "rawDbTransactionManager", readOnly = true)
    Optional<RawProduct> findByVariantRowId(String variantRowId);
}
