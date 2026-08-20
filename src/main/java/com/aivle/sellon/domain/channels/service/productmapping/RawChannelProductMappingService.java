package com.aivle.sellon.domain.channels.service.productmapping;

import com.aivle.sellon.domain.channels.enums.MappingMethod;
import com.aivle.sellon.rawdb.entity.RawMappedData;
import com.aivle.sellon.rawdb.entity.RawProduct;
import com.aivle.sellon.rawdb.repository.RawMappedDataRepository;
import com.aivle.sellon.rawdb.repository.RawChannelProductRepository;
import com.aivle.sellon.rawdb.repository.RawCsRepository;
import com.aivle.sellon.rawdb.repository.RawReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

// raw db(products/mapped_data) 읽기/쓰기 전담 - 메인 db와 물리적으로 다른 데이터소스라 별도 트랜잭션으로 분리 (원자성 보장 안 됨)
@Slf4j
@Service
@RequiredArgsConstructor
public class RawChannelProductMappingService {

    private final RawChannelProductRepository rawChannelProductRepository;
    private final RawMappedDataRepository channelProductMappingRepository;
    private final RawCsRepository rawCsInquiryRepository;
    private final RawReviewRepository rawReviewRepository;

    @Transactional(value = "rawDbTransactionManager", readOnly = true)
    public List<RawProduct> getProductsByChannelIds(List<String> channelIds) {
        return rawChannelProductRepository.findByChannelIdIn(channelIds);
    }

    @Transactional(value = "rawDbTransactionManager", readOnly = true)
    public Optional<RawProduct> getProduct(String variantRowId) {
        return rawChannelProductRepository.findByVariantRowId(variantRowId);
    }

    @Transactional(value = "rawDbTransactionManager", readOnly = true)
    public Map<String, RawMappedData> getMappingsByVariantRowIds(List<String> variantRowIds) {
        return channelProductMappingRepository.findByVariantRowIdIn(variantRowIds).stream()
                .collect(Collectors.toMap(RawMappedData::getVariantRowId, Function.identity()));
    }

    @Transactional(value = "rawDbTransactionManager", readOnly = true)
    public Optional<RawMappedData> getMapping(String variantRowId) {
        return channelProductMappingRepository.findByVariantRowId(variantRowId);
    }

    // 이 productGroupId로 이미 확정된 매핑들이 어느 채널들에 걸쳐 있는지 (후보 카드에 "다른 채널에도 있음" 표시용)
    @Transactional(value = "rawDbTransactionManager", readOnly = true)
    public List<String> getLinkedChannels(String productGroupId) {
        return channelProductMappingRepository.findDistinctChannelsByProductGroupId(productGroupId);
    }

    // 채널 상품 카탈로그 한 행을 적재 (이미 존재하는 variant_row_id는 스킵, 최초 1회 적재)
    @Transactional("rawDbTransactionManager")
    public void upsertProduct(String variantRowId, String channel, String channelProductId,
                               String channelProductName, String optionGroupNames, String channelOptionName,
                               Integer salePrice, Integer originalPrice) {
        if (rawChannelProductRepository.findByVariantRowId(variantRowId).isPresent()) {
            return;
        }
        rawChannelProductRepository.save(RawProduct.of(
                variantRowId, channel, channelProductId, channelProductName,
                optionGroupNames, channelOptionName, salePrice, originalPrice));

        if (channelProductMappingRepository.findByVariantRowId(variantRowId).isEmpty()) {
            channelProductMappingRepository.save(RawMappedData.pending(variantRowId));
        }
    }

    // variantRowId를 확정하고 같은 channelProductId의 미확정 옵션들도 함께 확정(cascade) + cs/reviews 소급 갱신
    @Transactional("rawDbTransactionManager")
    public void confirmMapping(String variantRowId, String channel, String channelProductId, String productGroupId) {
        OffsetDateTime now = OffsetDateTime.now();

        RawMappedData mapping = channelProductMappingRepository.findByVariantRowId(variantRowId)
                .orElseGet(() -> RawMappedData.pending(variantRowId));
        mapping.confirm(productGroupId, MappingMethod.MANUAL, null, now);
        channelProductMappingRepository.save(mapping);

        List<RawMappedData> siblings = channelProductMappingRepository
                .findByChannelAndChannelProductId(channel, channelProductId);
        for (RawMappedData sibling : siblings) {
            if (sibling.getVariantRowId().equals(variantRowId) || sibling.getProductGroupId() != null) {
                continue;
            }
            sibling.confirm(productGroupId, MappingMethod.MANUAL, null, now);
            channelProductMappingRepository.save(sibling);
        }

        backfillProductGroupId(channel, channelProductId, productGroupId);
    }

    // 배치 매칭 결과(mapping_result.csv)로 channelProductId 단위 클러스터 확정 - MANUAL은 덮어쓰지 않음(사람 개입 우선)
    @Transactional("rawDbTransactionManager")
    public int confirmMappingForChannelProduct(String channel, String channelProductId, String productGroupId,
                                                MappingMethod mappingMethod, Double mappingConfidence) {
        OffsetDateTime now = OffsetDateTime.now();
        List<RawMappedData> siblings = channelProductMappingRepository
                .findByChannelAndChannelProductId(channel, channelProductId);

        int count = 0;
        for (RawMappedData sibling : siblings) {
            if (MappingMethod.MANUAL.name().equals(sibling.getMappingMethod())) {
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

    // 과거 cs/reviews 행의 product_group_id를 (channel, channelProductId) 기준 전량 소급 갱신 (기간 제한 없음)
    private void backfillProductGroupId(String channel, String channelProductId, String productGroupId) {
        int updatedCsCount = rawCsInquiryRepository.updateProductGroupIdByChannelAndChannelProductId(
                channel, channelProductId, productGroupId);
        int updatedReviewCount = rawReviewRepository.updateProductGroupIdByChannelAndChannelProductId(
                channel, channelProductId, productGroupId);
        log.info("[상품 매핑 소급 반영] channel={}, channelProductId={}, productGroupId={} -> cs {}건, reviews {}건 갱신",
                channel, channelProductId, productGroupId, updatedCsCount, updatedReviewCount);
    }
}
