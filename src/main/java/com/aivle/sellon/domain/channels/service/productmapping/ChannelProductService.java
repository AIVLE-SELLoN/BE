package com.aivle.sellon.domain.channels.service.productmapping;

import com.aivle.sellon.domain.channels.dto.request.ConnectMappingRequest;
import com.aivle.sellon.domain.channels.dto.request.NewGroupRequest;
import com.aivle.sellon.domain.channels.dto.response.ChannelProductResponse;
import com.aivle.sellon.domain.channels.dto.response.MappingSummaryResponse;
import com.aivle.sellon.domain.channels.dto.response.MatchCandidateResponse;
import com.aivle.sellon.domain.channels.entity.connection.UsersChannel;
import com.aivle.sellon.domain.channels.entity.productmapping.MasterProduct;
import com.aivle.sellon.domain.channels.exception.ChannelAccessDeniedException;
import com.aivle.sellon.domain.channels.exception.connection.UsersChannelNotFoundException;
import com.aivle.sellon.domain.channels.exception.productmapping.ChannelProductNotFoundException;
import com.aivle.sellon.domain.channels.exception.productmapping.MasterProductNotFoundException;
import com.aivle.sellon.domain.channels.repository.connection.UsersChannelRepository;
import com.aivle.sellon.domain.channels.repository.productmapping.MasterProductRepository;
import com.aivle.sellon.domain.company.entity.Company;
import com.aivle.sellon.rawdb.entity.RawMappedData;
import com.aivle.sellon.rawdb.entity.RawProduct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

// 상품 매핑 조회/확정 - 채널 상품 원본은 raw db, 마스터 상품(이름/product_group_id)은 메인 db(MasterProduct)에서 관리
@Service
@RequiredArgsConstructor
public class ChannelProductService {

    private static final int MAX_CANDIDATES = 5;

    private final RawChannelProductMappingService rawChannelProductMappingService;
    private final MasterProductRepository masterProductRepository;
    private final UsersChannelRepository usersChannelRepository;
    private final MatchCandidateClient matchCandidateClient;

    @Transactional(readOnly = true)
    public List<ChannelProductResponse> getMappings(Long companyId, Long usersChannelKey, Boolean matched, String keyword) {
        verifyOwnership(usersChannelKey, companyId);

        List<RawProduct> products = rawChannelProductMappingService.getProducts(usersChannelKey);
        Map<String, RawMappedData> mappings = rawChannelProductMappingService.getMappingsByVariantRowIds(
                products.stream().map(RawProduct::getVariantRowId).toList());

        return products.stream()
                .filter(p -> keyword == null || keyword.isBlank()
                        || p.getChannelProductName().toLowerCase().contains(keyword.toLowerCase())
                        || p.getChannelProductId().toLowerCase().contains(keyword.toLowerCase()))
                .filter(p -> matched == null || isMatched(mappings.get(p.getVariantRowId())) == matched)
                .map(p -> ChannelProductResponse.of(p, mappings.get(p.getVariantRowId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public MappingSummaryResponse getSummary(Long companyId, Long usersChannelKey) {
        verifyOwnership(usersChannelKey, companyId);

        List<RawProduct> products = rawChannelProductMappingService.getProducts(usersChannelKey);
        Map<String, RawMappedData> mappings = rawChannelProductMappingService.getMappingsByVariantRowIds(
                products.stream().map(RawProduct::getVariantRowId).toList());

        long matchedCount = products.stream().filter(p -> isMatched(mappings.get(p.getVariantRowId()))).count();
        long unmatchedCount = products.size() - matchedCount;
        return MappingSummaryResponse.of(unmatchedCount, matchedCount);
    }

    @Transactional(readOnly = true)
    public List<MatchCandidateResponse> getCandidates(Long companyId, Long usersChannelKey, String variantRowId, String keyword) {
        RawProduct product = getOwnedRawProductOrThrow(usersChannelKey, variantRowId, companyId);

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
                        false
                ))
                .toList();
    }

    @Transactional
    public ChannelProductResponse connect(Long companyId, Long usersChannelKey, String variantRowId, ConnectMappingRequest request) {
        RawProduct product = getOwnedRawProductOrThrow(usersChannelKey, variantRowId, companyId);
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

    @Transactional
    public ChannelProductResponse createNewGroup(Long companyId, Long usersChannelKey, String variantRowId, NewGroupRequest request) {
        RawProduct product = getOwnedRawProductOrThrow(usersChannelKey, variantRowId, companyId);
        UsersChannel usersChannel = usersChannelRepository.findById(usersChannelKey)
                .orElseThrow(UsersChannelNotFoundException::new);
        Company company = usersChannel.getCompany();

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

    private RawProduct getOwnedRawProductOrThrow(Long usersChannelKey, String variantRowId, Long companyId) {
        verifyOwnership(usersChannelKey, companyId);
        RawProduct product = rawChannelProductMappingService.getProduct(variantRowId)
                .orElseThrow(ChannelProductNotFoundException::new);
        if (!product.getUsersChannelKey().equals(usersChannelKey)) {
            throw new ChannelAccessDeniedException();
        }
        return product;
    }

    private UsersChannel verifyOwnership(Long usersChannelKey, Long companyId) {
        UsersChannel usersChannel = usersChannelRepository.findById(usersChannelKey)
                .orElseThrow(UsersChannelNotFoundException::new);
        if (!usersChannel.getCompany().getId().equals(companyId)) {
            throw new ChannelAccessDeniedException();
        }
        return usersChannel;
    }

    // TODO: count 기반 임시 채번 -> 동시성 안전한 시퀀스로 교체 필요
    private String generateProductGroupId(Long companyId) {
        long nextSeq = masterProductRepository.countByCompany_Id(companyId) + 1;
        return "P%04d".formatted(nextSeq);
    }
}
