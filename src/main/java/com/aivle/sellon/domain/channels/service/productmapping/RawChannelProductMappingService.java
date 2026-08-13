package com.aivle.sellon.domain.channels.service.productmapping;

import com.aivle.sellon.domain.channels.enums.MappingMethod;
import com.aivle.sellon.rawdb.entity.ChannelProductMapping;
import com.aivle.sellon.rawdb.entity.RawChannelProduct;
import com.aivle.sellon.rawdb.repository.ChannelProductMappingRepository;
import com.aivle.sellon.rawdb.repository.RawChannelProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * raw db(products/mapped_data)에 대한 읽기/쓰기 전담.
 * 메인 db(ChannelProductService)와는 물리적으로 다른 데이터소스(RawDataSourceConfig)라
 * 하나의 @Transactional로 묶을 수 없어서, raw db 쪽 작업만 별도 서비스/트랜잭션으로 분리했다.
 * 즉 "메인 db에서 MasterProduct 확정" + "raw db에 productGroupId 반영"은 각각 별도 트랜잭션으로
 * 커밋되며, 두 번째 단계 실패 시 수동 재시도가 필요할 수 있다 (원자적 트랜잭션 아님 - 알려진 한계).
 */
@Service
@RequiredArgsConstructor
public class RawChannelProductMappingService {

    private final RawChannelProductRepository rawChannelProductRepository;
    private final ChannelProductMappingRepository channelProductMappingRepository;

    @Transactional(value = "rawDbTransactionManager", readOnly = true)
    public List<RawChannelProduct> getProducts(Long usersChannelKey) {
        return rawChannelProductRepository.findByUsersChannelKey(usersChannelKey);
    }

    @Transactional(value = "rawDbTransactionManager", readOnly = true)
    public List<RawChannelProduct> getProducts(List<Long> usersChannelKeys) {
        return rawChannelProductRepository.findByUsersChannelKeyIn(usersChannelKeys);
    }

    @Transactional(value = "rawDbTransactionManager", readOnly = true)
    public Optional<RawChannelProduct> getProduct(String variantRowId) {
        return rawChannelProductRepository.findByVariantRowId(variantRowId);
    }

    @Transactional(value = "rawDbTransactionManager", readOnly = true)
    public Map<String, ChannelProductMapping> getMappingsByVariantRowIds(List<String> variantRowIds) {
        return channelProductMappingRepository.findByVariantRowIdIn(variantRowIds).stream()
                .collect(Collectors.toMap(ChannelProductMapping::getVariantRowId, Function.identity()));
    }

    @Transactional(value = "rawDbTransactionManager", readOnly = true)
    public Optional<ChannelProductMapping> getMapping(String variantRowId) {
        return channelProductMappingRepository.findByVariantRowId(variantRowId);
    }

    /**
     * Mock Producer의 채널 상품 카탈로그 한 행을 적재. 이미 존재하는 variant_row_id는 스킵한다
     * (카탈로그성 정적 데이터라 최초 1회 적재로 충분 - 가격 변동 등 갱신은 추후 별도 처리 필요).
     */
    @Transactional("rawDbTransactionManager")
    public void upsertProduct(Long usersChannelKey, String variantRowId, String channel, String channelProductId,
                               String channelProductName, String optionGroupNames, String channelOptionName,
                               Long salePrice, Long originalPrice) {
        if (rawChannelProductRepository.findByVariantRowId(variantRowId).isPresent()) {
            return;
        }
        rawChannelProductRepository.save(RawChannelProduct.of(
                usersChannelKey, variantRowId, channel, channelProductId, channelProductName,
                optionGroupNames, channelOptionName, salePrice, originalPrice));

        if (channelProductMappingRepository.findByVariantRowId(variantRowId).isEmpty()) {
            channelProductMappingRepository.save(ChannelProductMapping.pending(variantRowId, channel, channelProductId));
        }
    }

    /**
     * variantRowId 하나를 productGroupId(=masterSku)로 확정하고, 같은 channelProductId를 공유하는
     * 나머지 미확정(productGroupId == null) 옵션들도 같이 확정 처리한다 (cascade).
     */
    @Transactional("rawDbTransactionManager")
    public void confirmMapping(String variantRowId, String channel, String channelProductId, String productGroupId) {
        LocalDateTime now = LocalDateTime.now();

        ChannelProductMapping mapping = channelProductMappingRepository.findByVariantRowId(variantRowId)
                .orElseGet(() -> ChannelProductMapping.pending(variantRowId, channel, channelProductId));
        mapping.confirm(productGroupId, MappingMethod.MANUAL, null, now);
        channelProductMappingRepository.save(mapping);

        List<ChannelProductMapping> siblings = channelProductMappingRepository
                .findByChannelAndChannelProductId(channel, channelProductId);
        for (ChannelProductMapping sibling : siblings) {
            if (sibling.getVariantRowId().equals(variantRowId) || sibling.getProductGroupId() != null) {
                continue;
            }
            sibling.confirm(productGroupId, MappingMethod.MANUAL, null, now);
            channelProductMappingRepository.save(sibling);
        }
    }

    /**
     * (channel, channelProductId)를 공유하는 미확정(productGroupId == null) 매핑 전부를
     * 배치 매칭 결과로 확정한다 - mapping_result.csv import 전용 (variant 단위가 아니라
     * channel_product_id 단위 클러스터 결과를 한번에 반영).
     * @return 실제로 확정 처리된 행 수
     */
    @Transactional("rawDbTransactionManager")
    public int confirmMappingForChannelProduct(String channel, String channelProductId, String productGroupId,
                                                MappingMethod mappingMethod, Double mappingConfidence) {
        LocalDateTime now = LocalDateTime.now();
        List<ChannelProductMapping> siblings = channelProductMappingRepository
                .findByChannelAndChannelProductId(channel, channelProductId);

        int count = 0;
        for (ChannelProductMapping sibling : siblings) {
            if (sibling.getProductGroupId() != null) {
                continue;
            }
            sibling.confirm(productGroupId, mappingMethod, mappingConfidence, now);
            channelProductMappingRepository.save(sibling);
            count++;
        }
        return count;
    }
}
