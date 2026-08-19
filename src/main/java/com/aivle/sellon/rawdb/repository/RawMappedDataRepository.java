package com.aivle.sellon.rawdb.repository;

import com.aivle.sellon.rawdb.entity.RawMappedData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface RawMappedDataRepository extends JpaRepository<RawMappedData, String> {
    @Transactional(transactionManager = "rawDbTransactionManager", readOnly = true)
    Optional<RawMappedData> findByVariantRowId(String variantRowId);

    @Transactional(transactionManager = "rawDbTransactionManager", readOnly = true)
    List<RawMappedData> findByVariantRowIdIn(Collection<String> variantRowIds);

    @Transactional(transactionManager = "rawDbTransactionManager", readOnly = true)
    List<RawMappedData> findByChannelAndChannelProductId(String channel, String channelProductId);

    @Transactional(transactionManager = "rawDbTransactionManager", readOnly = true)
    List<RawMappedData> findByProductGroupIdIsNull();

    @Transactional(transactionManager = "rawDbTransactionManager", readOnly = true)
    List<RawMappedData> findByProductGroupId(String productGroupId);
}
