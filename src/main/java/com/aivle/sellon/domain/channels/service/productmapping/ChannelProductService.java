package com.aivle.sellon.domain.channels.service.productmapping;

import com.aivle.sellon.domain.channels.dto.request.ConnectMappingRequest;
import com.aivle.sellon.domain.channels.dto.request.NewGroupRequest;
import com.aivle.sellon.domain.channels.dto.response.ChannelProductResponse;
import com.aivle.sellon.domain.channels.dto.response.MappingSummaryResponse;
import com.aivle.sellon.domain.channels.dto.response.MatchCandidateResponse;
import com.aivle.sellon.domain.channels.entity.connection.UsersChannel;
import com.aivle.sellon.domain.channels.entity.productmapping.MasterProduct;
import com.aivle.sellon.domain.channels.entity.productmapping.SkippedMapping;
import com.aivle.sellon.domain.channels.exception.ChannelAccessDeniedException;
import com.aivle.sellon.domain.channels.exception.productmapping.ChannelProductNotFoundException;
import com.aivle.sellon.domain.channels.exception.productmapping.MasterProductNotFoundException;
import com.aivle.sellon.domain.channels.repository.connection.UsersChannelRepository;
import com.aivle.sellon.domain.channels.repository.productmapping.MasterProductRepository;
import com.aivle.sellon.domain.channels.repository.productmapping.SkippedMappingRepository;
import com.aivle.sellon.domain.company.entity.Company;
import com.aivle.sellon.rawdb.entity.RawMappedData;
import com.aivle.sellon.rawdb.entity.RawProduct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

// 상품 매핑 조회/확정 - 채널 상품 원본은 raw db, 마스터 상품(이름/product_group_id)은 메인 db(MasterProduct)에서 관리
@Service
@RequiredArgsConstructor
public class ChannelProductService {

    private static final int MAX_CANDIDATES = 5;

    private final RawChannelProductMappingService rawChannelProductMappingService;
    private final MasterProductRepository masterProductRepository;
    private final UsersChannelRepository usersChannelRepository;
    private final SkippedMappingRepository skippedMappingRepository;
    private final MatchCandidateClient matchCandidateClient;

    @Transactional(readOnly = true)
    public List<ChannelProductResponse> getMappings(Long companyId, Boolean matched, String keyword) {
        List<RawProduct> products = rawChannelProductMappingService.getProductsByChannelIds(connectedChannelTypes(companyId));
        Map<String, RawMappedData> mappings = rawChannelProductMappingService.getMappingsByVariantRowIds(
                products.stream().map(RawProduct::getVariantRowId).toList());
        Set<String> skippedVariantRowIds = skippedVariantRowIds(companyId, products);

        return products.stream()
                .filter(p -> keyword == null || keyword.isBlank()
                        || p.getChannelProductName().toLowerCase().contains(keyword.toLowerCase())
                        || p.getChannelProductId().toLowerCase().contains(keyword.toLowerCase()))
                .filter(p -> matched == null || isMatched(mappings.get(p.getVariantRowId())) == matched)
                .map(p -> ChannelProductResponse.of(
                        p, mappings.get(p.getVariantRowId()), skippedVariantRowIds.contains(p.getVariantRowId())))
                .toList();
    }

    // 회사가 연동한 채널(쿠팡/지그재그/네이버) 전부의 channelType 목록 - 크로스채널 통합 조회의 기준
    private List<String> connectedChannelTypes(Long companyId) {
        return usersChannelRepository.findByCompany_Id(companyId).stream()
                .map(UsersChannel::getChannelType)
                .distinct()
                .toList();
    }

    private Set<String> skippedVariantRowIds(Long companyId, List<RawProduct> products) {
        Set<String> variantRowIds = products.stream().map(RawProduct::getVariantRowId).collect(Collectors.toSet());
        if (variantRowIds.isEmpty()) {
            return Set.of();
        }
        return skippedMappingRepository.findByCompanyIdAndVariantRowIdIn(companyId, variantRowIds).stream()
                .map(SkippedMapping::getVariantRowId)
                .collect(Collectors.toSet());
    }

    @Transactional(readOnly = true)
    public MappingSummaryResponse getSummary(Long companyId) {
        List<RawProduct> products = rawChannelProductMappingService.getProductsByChannelIds(connectedChannelTypes(companyId));
        Map<String, RawMappedData> mappings = rawChannelProductMappingService.getMappingsByVariantRowIds(
                products.stream().map(RawProduct::getVariantRowId).toList());

        long matchedCount = products.stream().filter(p -> isMatched(mappings.get(p.getVariantRowId()))).count();
        long unmatchedCount = products.size() - matchedCount;
        return MappingSummaryResponse.of(unmatchedCount, matchedCount);
    }

    @Transactional(readOnly = true)
    public List<MatchCandidateResponse> getCandidates(Long companyId, String variantRowId, String keyword) {
        RawProduct product = getOwnedRawProductOrThrow(companyId, variantRowId);

        List<MasterProduct> candidates = (keyword != null && !keyword.isBlank())
                ? masterProductRepository.searchByKeyword(companyId, keyword)
                : masterProductRepository.findByCompany_Id(companyId);

        List<MatchCandidateClient.ScoredCandidate> ranked = matchCandidateClient.rank(product.getChannelProductName(), candidates);

        return ranked.stream()
                .sorted(Comparator.comparingDouble(MatchCandidateClient.ScoredCandidate::similarityScore).reversed())
                .limit(MAX_CANDIDATES)
                .map(c -> new MatchCandidateResponse(
                        c.masterProduct().getMasterProductKey(),
                        c.masterProduct().getProductGroupId(),
                        c.masterProduct().getProductName(),
                        c.similarityScore(),
                        false,
                        buildReason(c.similarityScore()),
                        rawChannelProductMappingService.getLinkedChannels(c.masterProduct().getProductGroupId())
                ))
                .toList();
    }

    // similarityScore를 사람이 읽을 수 있는 문구로 변환 (AI 서버 연동 전까지의 임시 표시용)
    private String buildReason(double similarityScore) {
        if (similarityScore >= 90) {
            return "상품명 유사도 매우 높음 (%.0f%%)".formatted(similarityScore);
        }
        if (similarityScore >= 75) {
            return "상품명 유사도 높음 (%.0f%%)".formatted(similarityScore);
        }
        return "상품명 유사도 낮음 (%.0f%%)".formatted(similarityScore);
    }

    @Transactional
    public ChannelProductResponse connect(Long companyId, String variantRowId, ConnectMappingRequest request) {
        RawProduct product = getOwnedRawProductOrThrow(companyId, variantRowId);
        MasterProduct masterProduct = masterProductRepository.findById(request.masterProductKey())
                .orElseThrow(MasterProductNotFoundException::new);
        if (!masterProduct.getCompany().getId().equals(companyId)) {
            throw new ChannelAccessDeniedException();
        }

        rawChannelProductMappingService.confirmMapping(
                variantRowId, product.getChannelId(), product.getChannelProductId(), masterProduct.getProductGroupId());

        RawMappedData mapping = rawChannelProductMappingService.getMapping(variantRowId).orElseThrow();
        return ChannelProductResponse.of(product, mapping);
    }

    // 미매칭 상품을 "건너뜀" 처리 - raw db는 안 건드리고 메인 db에 별도 기록만 남긴다 (미매칭 목록에 계속 남음).
    @Transactional
    public ChannelProductResponse skip(Long companyId, String variantRowId) {
        RawProduct product = getOwnedRawProductOrThrow(companyId, variantRowId);

        if (!skippedMappingRepository.existsByVariantRowId(variantRowId)) {
            skippedMappingRepository.save(SkippedMapping.of(variantRowId, companyId));
        }

        RawMappedData mapping = rawChannelProductMappingService.getMapping(variantRowId).orElse(null);
        return ChannelProductResponse.of(product, mapping, true);
    }

    @Transactional
    public ChannelProductResponse createNewGroup(Long companyId, String variantRowId, NewGroupRequest request) {
        RawProduct product = getOwnedRawProductOrThrow(companyId, variantRowId);
        Company company = resolveOwningChannel(companyId, product).getCompany();

        String productName = (request.productName() != null && !request.productName().isBlank())
                ? request.productName()
                : product.getChannelProductName();

        MasterProduct masterProduct = masterProductRepository.save(
                MasterProduct.of(company, generateProductGroupId(company.getId()), productName));

        try {
            rawChannelProductMappingService.confirmMapping(
                    variantRowId, product.getChannelId(), product.getChannelProductId(), masterProduct.getProductGroupId());
        } catch (RuntimeException e) {
            // raw db 반영 실패 시 방금 만든 마스터 상품을 롤백 (메인/raw db가 별도 트랜잭션이라 수동 보정)
            masterProductRepository.delete(masterProduct);
            throw e;
        }

        RawMappedData mapping = rawChannelProductMappingService.getMapping(variantRowId).orElseThrow();
        return ChannelProductResponse.of(product, mapping);
    }

    private boolean isMatched(RawMappedData mapping) {
        return mapping != null && mapping.getProductGroupId() != null;
    }

    // products에는 회사 구분 컬럼이 없어(raw db 문서 §4.3) - variantRowId로 상품을 찾은 뒤,
    // 그 상품의 channelId(채널 종류)를 이 회사가 실제로 연동해뒀는지로 접근 범위를 제한한다.
    private RawProduct getOwnedRawProductOrThrow(Long companyId, String variantRowId) {
        RawProduct product = rawChannelProductMappingService.getProduct(variantRowId)
                .orElseThrow(ChannelProductNotFoundException::new);
        resolveOwningChannel(companyId, product);
        return product;
    }

    private UsersChannel resolveOwningChannel(Long companyId, RawProduct product) {
        return usersChannelRepository.findByCompany_IdAndChannelType(companyId, product.getChannelId())
                .orElseThrow(ChannelAccessDeniedException::new);
    }

    // TODO: count 기반 임시 채번 -> 동시성 안전한 시퀀스로 교체 필요
    private String generateProductGroupId(Long companyId) {
        long nextSeq = masterProductRepository.countByCompany_Id(companyId) + 1;
        return "P%04d".formatted(nextSeq);
    }
}
