package com.aivle.sellon.domain.channels.service.productmapping;

import com.aivle.sellon.domain.channels.enums.MappingMethod;
import com.aivle.sellon.rawdb.entity.ChannelProductMapping;
import com.aivle.sellon.rawdb.entity.RawChannelProduct;
import com.aivle.sellon.rawdb.repository.ChannelProductMappingRepository;
import com.aivle.sellon.rawdb.repository.RawChannelProductRepository;
import com.aivle.sellon.rawdb.repository.RawCsInquiryRepository;
import com.aivle.sellon.rawdb.repository.RawReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
@Service
@RequiredArgsConstructor
public class RawChannelProductMappingService {

    private final RawChannelProductRepository rawChannelProductRepository;
    private final ChannelProductMappingRepository channelProductMappingRepository;
    private final RawCsInquiryRepository rawCsInquiryRepository;
    private final RawReviewRepository rawReviewRepository;

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
     * variantRowId 하나를 productGroupId(=MasterProduct.productGroupId)로 확정하고, 같은 channelProductId를 공유하는
     * 나머지 미확정(productGroupId == null) 옵션들도 같이 확정 처리한다 (cascade).
     * 상품 매핑 소급 반영 계약 ①/④에 따라, 이미 적재된 과거 cs/reviews 행도
     * 같은 (channel, channelProductId) 기준으로 전량 소급 갱신한다 (기간 제한 없이 전체).
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

        backfillProductGroupId(channel, channelProductId, productGroupId);
    }

    /**
     * (channel, channelProductId)를 공유하는 매핑을 배치 매칭 결과(mapping_result.csv import)로
     * 확정한다 - variant 단위가 아니라 channel_product_id 단위 클러스터 결과를 한번에 반영한다.
     * 정책: "사람 개입 우선" - 이미 사람이 화면에서 직접 확정한 매핑(MappingMethod.MANUAL)은
     * 배치 재구성(§5-3 주 1회) 결과가 들어와도 덮어쓰지 않는다. 아직 미확정이거나 이전에
     * 배치(RULE/EMBEDDING 등)로만 자동 매핑됐던 건은 최신 배치 결과로 갱신한다.
     * 상품 매핑 소급 반영 계약 ①/④에 따라, 실제로 갱신된 경우 이미 적재된 과거
     * cs/reviews 행도 같은 (channel, channelProductId) 기준으로 전량 소급 갱신한다 (기간 제한 없이 전체).
     * @return 실제로 확정/갱신 처리된 행 수 (MANUAL 보호로 스킵된 행은 제외)
     */
    @Transactional("rawDbTransactionManager")
    public int confirmMappingForChannelProduct(String channel, String channelProductId, String productGroupId,
                                                MappingMethod mappingMethod, Double mappingConfidence) {
        LocalDateTime now = LocalDateTime.now();
        List<ChannelProductMapping> siblings = channelProductMappingRepository
                .findByChannelAndChannelProductId(channel, channelProductId);

        int count = 0;
        for (ChannelProductMapping sibling : siblings) {
            if (sibling.getMappingMethod() == MappingMethod.MANUAL) {
                // 사람이 직접 확정한 매핑은 배치 재구성으로 덮어쓰지 않는다 (사람 개입 우선 정책).
                continue;
            }
            sibling.confirm(productGroupId, mappingMethod, mappingConfidence, now);
            channelProductMappingRepository.save(sibling);
            count++;
        }

        if (count > 0) {
            backfillProductGroupId(channel, channelProductId, productGroupId);
        }
        return count;
    }

    /**
     * mapped_data가 (channel, channelProductId) 기준으로 새 product_group_id로 확정/변경될 때마다
     * 호출되어, 이미 적재된 과거 cs/reviews 행의 product_group_id를 같은 값으로 소급 갱신한다.
     * "최근 N일만" 갱신하면 이상탐지 분모가 시간축 앞쪽만 깎여 과거 비율이 왜곡되므로,
     * 시간 조건 없이 해당 (channel, channelProductId)의 전체 행을 갱신한다.
     */
    private void backfillProductGroupId(String channel, String channelProductId, String productGroupId) {
        int updatedCsCount = rawCsInquiryRepository.updateProductGroupIdByChannelAndChannelProductId(
                channel, channelProductId, productGroupId);
        int updatedReviewCount = rawReviewRepository.updateProductGroupIdByChannelAndChannelProductId(
                channel, channelProductId, productGroupId);
        log.info("[상품 매핑 소급 반영] channel={}, channelProductId={}, productGroupId={} -> cs {}건, reviews {}건 갱신",
                channel, channelProductId, productGroupId, updatedCsCount, updatedReviewCount);
    }
}
