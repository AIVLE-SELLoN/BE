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
import com.aivle.sellon.rawdb.entity.ChannelProductMapping;
import com.aivle.sellon.rawdb.entity.RawChannelProduct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * 상품 매핑은 raw db(products/mapped_data)의 실제 채널 상품·매핑 데이터를 조회/확정하는 기능이다.
 * 채널 상품 원본 자체는 raw db에만 있고(메인 db에 별도 복제하지 않음), 마스터 상품(이름/product_group_id)만
 * 메인 db(MasterProduct)에서 관리한다 - raw db 확정 스키마엔 마스터 상품명을 담는 테이블이 없기 때문.
 * raw db 조회/쓰기는 전부 RawChannelProductMappingService(별도 트랜잭션)에 위임한다.
 */
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

        List<RawChannelProduct> products = rawChannelProductMappingService.getProducts(usersChannelKey);
        Map<String, ChannelProductMapping> mappings = rawChannelProductMappingService.getMappingsByVariantRowIds(
                products.stream().map(RawChannelProduct::getVariantRowId).toList());

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

        List<RawChannelProduct> products = rawChannelProductMappingService.getProducts(usersChannelKey);
        Map<String, ChannelProductMapping> mappings = rawChannelProductMappingService.getMappingsByVariantRowIds(
                products.stream().map(RawChannelProduct::getVariantRowId).toList());

        long matchedCount = products.stream().filter(p -> isMatched(mappings.get(p.getVariantRowId()))).count();
        long unmatchedCount = products.size() - matchedCount;
        return MappingSummaryResponse.of(unmatchedCount, matchedCount);
    }

    @Transactional(readOnly = true)
    public List<MatchCandidateResponse> getCandidates(Long companyId, Long usersChannelKey, String variantRowId, String keyword) {
        RawChannelProduct product = getOwnedRawProductOrThrow(usersChannelKey, variantRowId, companyId);

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
        RawChannelProduct product = getOwnedRawProductOrThrow(usersChannelKey, variantRowId, companyId);
        MasterProduct masterProduct = masterProductRepository.findById(request.masterProductKey())
                .orElseThrow(MasterProductNotFoundException::new);
        if (!masterProduct.getCompany().getId().equals(companyId)) {
            throw new ChannelAccessDeniedException();
        }

        rawChannelProductMappingService.confirmMapping(
                variantRowId, product.getChannelId(), product.getChannelProductId(), masterProduct.getProductGroupId());

        ChannelProductMapping mapping = rawChannelProductMappingService.getMapping(variantRowId).orElseThrow();
        return ChannelProductResponse.of(product, mapping);
    }

    @Transactional
    public ChannelProductResponse createNewGroup(Long companyId, Long usersChannelKey, String variantRowId, NewGroupRequest request) {
        RawChannelProduct product = getOwnedRawProductOrThrow(usersChannelKey, variantRowId, companyId);
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
            // raw db(mapped_data) 반영이 실패하면 방금 메인 db에 만든 마스터 상품이 아무 상품도
            // 연결되지 않은 빈 채로 남는다 - 메인 db와 raw db가 물리적으로 다른 데이터소스라 하나의
            // 트랜잭션으로 묶을 수 없어서(known limitation), 실패 시 방금 만든 행을 명시적으로 되돌려
            // "완전 성공 아니면 완전 실패"에 가깝게 만든다.
            masterProductRepository.delete(masterProduct);
            throw e;
        }

        ChannelProductMapping mapping = rawChannelProductMappingService.getMapping(variantRowId).orElseThrow();
        return ChannelProductResponse.of(product, mapping);
    }

    private boolean isMatched(ChannelProductMapping mapping) {
        return mapping != null && mapping.getProductGroupId() != null;
    }

    private RawChannelProduct getOwnedRawProductOrThrow(Long usersChannelKey, String variantRowId, Long companyId) {
        verifyOwnership(usersChannelKey, companyId);
        RawChannelProduct product = rawChannelProductMappingService.getProduct(variantRowId)
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

    // TODO: 동시성 안전한 시퀀스로 교체 필요 (지금은 count 기반 임시 채번)
    // 매칭 툴(mapping_result.csv)의 mapped_product_code 규칙(P{숫자})과 동일한 형식으로 채번한다.
    private String generateProductGroupId(Long companyId) {
        long nextSeq = masterProductRepository.countByCompany_Id(companyId) + 1;
        return "P%04d".formatted(nextSeq);
    }
}
