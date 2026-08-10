package com.aivle.sellon.domain.proposal.service.support;

import com.aivle.sellon.domain.channels.service.ProductDescriptionService;
import com.aivle.sellon.domain.proposal.entity.Proposal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

// 개선안 승인(ProposalReviewService)/롤백(ProposalHistoryService)에서 공통으로 쓰는
// "상품 설명 실제 반영" 호출부. Proposal 쪽 null 가드(상품그룹ID/채널 없는 경우)까지 여기서 처리한다.
@Component
@RequiredArgsConstructor
public class ProposalProductDescriptionApplier {

    private final ProductDescriptionService productDescriptionService;

    public void apply(Proposal proposal, Long companyId, String description) {
        if (proposal.getProductGroupId() == null || proposal.getChannel() == null || description == null) return;
        productDescriptionService.apply(
            proposal.getRootUser(), companyId, proposal.getProductGroupId(), proposal.getChannel().name(), description
        );
    }
}
