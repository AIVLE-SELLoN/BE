package com.aivle.sellon.rawdb.repository;

import com.aivle.sellon.rawdb.dto.ProductNameRow;
import com.aivle.sellon.rawdb.entity.RawMappedData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface RawMappedDataRepository extends JpaRepository<RawMappedData, String> {

    // product_group_id 하나에 variant 행(채널 x 옵션)이 여러 개 매달려 있어 결과에 중복 product_group_id가 나올 수 있다.
    // variant_row_id 오름차순으로 고정해 항상 동일한 행이 채택되도록 한다 - 호출측에서 첫 값만 dedup
    @Transactional(transactionManager = "rawDbTransactionManager", readOnly = true)
    @Query("SELECT m.productGroupId AS productGroupId, p.channelProductName AS productName " +
            "FROM RawMappedData m, RawProduct p " +
            "WHERE m.variantRowId = p.variantRowId " +
            "AND m.productGroupId IN :productGroupIds " +
            "ORDER BY m.variantRowId ASC")
    List<ProductNameRow> findProductNamesByProductGroupIdIn(@Param("productGroupIds") Collection<String> productGroupIds);

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
