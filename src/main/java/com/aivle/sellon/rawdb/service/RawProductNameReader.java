package com.aivle.sellon.rawdb.service;

import com.aivle.sellon.rawdb.repository.RawMappedDataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

// product_group_id -> 상품명(channel_product_name) 조회. mapped_data -> products 역방향 2단 조인을 raw DB 단일 트랜잭션에서 처리한다.
@Component
@RequiredArgsConstructor
public class RawProductNameReader {

    private final RawMappedDataRepository rawMappedDataRepository;

    @Transactional(transactionManager = "rawDbTransactionManager", readOnly = true)
    public Map<String, String> readProductNamesByProductGroupIds(Collection<String> productGroupIds) {
        if (productGroupIds.isEmpty()) {
            return Map.of();
        }
        // product_group_id 하나에 variant 행이 여러 개 매달려 있어 first-wins로 dedup 한다.
        // Collectors.toMap은 value가 null이면 NPE라 쓰지 않는다. 상품명 한 건 때문에 대시보드 전체가 실패하면 안 된다.
        Map<String, String> productNames = new HashMap<>();
        rawMappedDataRepository.findProductNamesByProductGroupIdIn(productGroupIds)
                .forEach(row -> productNames.putIfAbsent(row.getProductGroupId(), row.getProductName()));
        return productNames;
    }
}
