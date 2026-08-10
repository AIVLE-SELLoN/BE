package com.aivle.sellon.domain.proposal.service.support;

import com.aivle.sellon.domain.channels.service.ProductDescriptionService;
import com.aivle.sellon.domain.proposal.entity.Proposal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

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
