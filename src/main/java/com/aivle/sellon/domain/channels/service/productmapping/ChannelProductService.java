package com.aivle.sellon.domain.channels.service.productmapping;

import com.aivle.sellon.domain.channels.dto.request.ConnectMappingRequest;
import com.aivle.sellon.domain.channels.dto.request.NewGroupRequest;
import com.aivle.sellon.domain.channels.dto.response.ChannelProductResponse;
import com.aivle.sellon.domain.channels.dto.response.MappingSummaryResponse;
import com.aivle.sellon.domain.channels.dto.response.MatchCandidateResponse;
import com.aivle.sellon.domain.channels.entity.productmapping.ChannelProduct;
import com.aivle.sellon.domain.channels.entity.productmapping.MasterProduct;
import com.aivle.sellon.domain.channels.entity.connection.UsersChannel;
import com.aivle.sellon.domain.channels.enums.MappingStatus;
import com.aivle.sellon.domain.channels.exception.ChannelAccessDeniedException;
import com.aivle.sellon.domain.channels.exception.productmapping.ChannelProductNotFoundException;
import com.aivle.sellon.domain.channels.exception.productmapping.MasterProductNotFoundException;
import com.aivle.sellon.domain.channels.exception.connection.UsersChannelNotFoundException;
import com.aivle.sellon.domain.channels.repository.productmapping.ChannelProductRepository;
import com.aivle.sellon.domain.channels.repository.productmapping.MasterProductRepository;
import com.aivle.sellon.domain.channels.repository.connection.UsersChannelRepository;
import com.aivle.sellon.domain.company.entity.Company;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChannelProductService {

    private static final int MAX_CANDIDATES = 5;

    private final ChannelProductRepository channelProductRepository;
    private final MasterProductRepository masterProductRepository;
    private final UsersChannelRepository usersChannelRepository;
    private final MatchCandidateClient matchCandidateClient;

    @Transactional(readOnly = true)
    public List<ChannelProductResponse> getMappings(Long companyId, Long usersChannelKey, Boolean matched, String keyword) {
        verifyOwnership(usersChannelKey, companyId);
        List<ChannelProduct> products;
        if (keyword != null && !keyword.isBlank()) {
            products = channelProductRepository.searchByKeyword(usersChannelKey, keyword);
        } else if (matched == null) {
            products = channelProductRepository.findByUsersChannel_UsersChannelKey(usersChannelKey);
        } else if (matched) {
            products = channelProductRepository.findByUsersChannel_UsersChannelKeyAndMappingStatusNot(usersChannelKey, MappingStatus.UNMATCHED);
        } else {
            products = channelProductRepository.findByUsersChannel_UsersChannelKeyAndMappingStatus(usersChannelKey, MappingStatus.UNMATCHED);
        }
        return products.stream().map(ChannelProductResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public MappingSummaryResponse getSummary(Long companyId, Long usersChannelKey) {
        verifyOwnership(usersChannelKey, companyId);
        long unmatchedCount = channelProductRepository.countByUsersChannel_UsersChannelKeyAndMappingStatus(usersChannelKey, MappingStatus.UNMATCHED);
        long matchedCount = channelProductRepository.countByUsersChannel_UsersChannelKeyAndMappingStatusNot(usersChannelKey, MappingStatus.UNMATCHED);
        return MappingSummaryResponse.of(unmatchedCount, matchedCount);
    }

    @Transactional(readOnly = true)
    public List<MatchCandidateResponse> getCandidates(Long companyId, Long usersChannelKey, Long channelProductKey, String keyword) {
        ChannelProduct channelProduct = getOwnedChannelProductOrThrow(usersChannelKey, channelProductKey, companyId);
        Company company = channelProduct.getUsersChannel().getCompany();

        List<MasterProduct> candidates = (keyword != null && !keyword.isBlank())
                ? masterProductRepository.searchByKeyword(company.getId(), keyword)
                : masterProductRepository.findByCompany_Id(company.getId());

        List<MatchCandidateClient.ScoredCandidate> ranked = matchCandidateClient.rank(channelProduct.getProductName(), candidates);

        return ranked.stream()
                .sorted(Comparator.comparingDouble(MatchCandidateClient.ScoredCandidate::similarityScore).reversed())
                .limit(MAX_CANDIDATES)
                .map(c -> new MatchCandidateResponse(
                        c.masterProduct().getMasterProductKey(),
                        c.masterProduct().getMasterSku(),
                        c.masterProduct().getProductName(),
                        c.similarityScore(),
                        false
                ))
                .toList();
    }

    @Transactional
    public ChannelProductResponse connect(Long companyId, Long usersChannelKey, Long channelProductKey, ConnectMappingRequest request) {
        ChannelProduct channelProduct = getOwnedChannelProductOrThrow(usersChannelKey, channelProductKey, companyId);
        MasterProduct masterProduct = masterProductRepository.findById(request.masterProductKey())
                .orElseThrow(MasterProductNotFoundException::new);
        if (!masterProduct.getCompany().getId().equals(companyId)) {
            throw new ChannelAccessDeniedException();
        }

        channelProduct.manualConfirm(masterProduct);
        cascadeToSiblingOptions(channelProduct, masterProduct);
        return ChannelProductResponse.from(channelProduct);
    }

    @Transactional
    public ChannelProductResponse createNewGroup(Long companyId, Long usersChannelKey, Long channelProductKey, NewGroupRequest request) {
        ChannelProduct channelProduct = getOwnedChannelProductOrThrow(usersChannelKey, channelProductKey, companyId);
        UsersChannel usersChannel = channelProduct.getUsersChannel();
        Company company = usersChannel.getCompany();

        String productName = (request.productName() != null && !request.productName().isBlank())
                ? request.productName()
                : channelProduct.getProductName();

        MasterProduct masterProduct = MasterProduct.of(company, generateMasterSku(company.getId()), productName);
        masterProductRepository.save(masterProduct);

        channelProduct.manualConfirm(masterProduct);
        cascadeToSiblingOptions(channelProduct, masterProduct);
        return ChannelProductResponse.from(channelProduct);
    }

    /**
     * 같은 상품(channelItemId)의 나머지 미매칭 옵션들을 이번에 확정한 마스터 상품으로 같이 매칭 처리.
     * 화면은 그대로 옵션별 개별 행을 보여주지만, "미매칭" 카운트에서는 한꺼번에 빠지게 된다.
     */
    private void cascadeToSiblingOptions(ChannelProduct confirmed, MasterProduct masterProduct) {
        List<ChannelProduct> siblings = channelProductRepository
                .findByUsersChannel_UsersChannelKeyAndChannelItemIdAndMappingStatus(
                        confirmed.getUsersChannel().getUsersChannelKey(),
                        confirmed.getChannelItemId(),
                        MappingStatus.UNMATCHED
                );
        siblings.forEach(sibling -> sibling.manualConfirm(masterProduct));
    }

    private ChannelProduct getChannelProductOrThrow(Long channelProductKey) {
        return channelProductRepository.findById(channelProductKey)
                .orElseThrow(ChannelProductNotFoundException::new);
    }

    /**
     * channelProductKey가 실제로 path의 usersChannelKey 소속이고, 그 usersChannel이 요청자 회사 소유인지까지 검증.
     */
    private ChannelProduct getOwnedChannelProductOrThrow(Long usersChannelKey, Long channelProductKey, Long companyId) {
        ChannelProduct channelProduct = getChannelProductOrThrow(channelProductKey);
        UsersChannel usersChannel = channelProduct.getUsersChannel();
        if (!usersChannel.getUsersChannelKey().equals(usersChannelKey)
                || !usersChannel.getCompany().getId().equals(companyId)) {
            throw new ChannelAccessDeniedException();
        }
        return channelProduct;
    }

    private void verifyOwnership(Long usersChannelKey, Long companyId) {
        UsersChannel usersChannel = usersChannelRepository.findById(usersChannelKey)
                .orElseThrow(UsersChannelNotFoundException::new);
        if (!usersChannel.getCompany().getId().equals(companyId)) {
            throw new ChannelAccessDeniedException();
        }
    }

    // TODO: 동시성 안전한 시퀀스로 교체 필요 (지금은 count 기반 임시 채번)
    private String generateMasterSku(Long companyId) {
        long nextSeq = masterProductRepository.countByCompany_Id(companyId) + 1;
        return "SLN-%04d".formatted(nextSeq);
    }
}
