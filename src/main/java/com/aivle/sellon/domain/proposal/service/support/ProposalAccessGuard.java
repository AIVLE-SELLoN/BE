package com.aivle.sellon.domain.proposal.service.support;

import com.aivle.sellon.domain.proposal.entity.Proposal;
import com.aivle.sellon.domain.proposal.exception.ProposalNotFoundException;
import org.springframework.stereotype.Component;

@Component
public class ProposalAccessGuard {
    public void requireCompanyAccess(Proposal proposal, Long companyId) {
        if (!proposal.getRootUser().getCompany().getId().equals(companyId)) {
            throw new ProposalNotFoundException();
        }
    }
}
