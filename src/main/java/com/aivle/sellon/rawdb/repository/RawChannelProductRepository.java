package com.aivle.sellon.rawdb.repository;

import com.aivle.sellon.rawdb.entity.RawProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface RawChannelProductRepository extends JpaRepository<RawProduct, String> {
    // raw db 문서 §4.3 기준 products에는 회사 구분 컬럼이 없어 channelId(채널 종류)로 스코핑한다.
    @Transactional(transactionManager = "rawDbTransactionManager", readOnly = true)
    List<RawProduct> findByChannelIdIn(Collection<String> channelIds);

    @Transactional(transactionManager = "rawDbTransactionManager", readOnly = true)
    Optional<RawProduct> findByVariantRowId(String variantRowId);
}
