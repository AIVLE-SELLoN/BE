package com.aivle.sellon.domain.proposal.service.support;

import com.aivle.sellon.domain.proposal.entity.Proposal;
import com.aivle.sellon.domain.proposal.exception.ProposalNotFoundException;
import org.springframework.stereotype.Component;

// 같은 회사 소속인지 확인 — 다른 회사의 리포트/이력에 접근하지 못하도록 방지.
// Query/Review/History 서비스가 공통으로 쓴다.
@Component
public class ProposalAccessGuard {
    public void requireCompanyAccess(Proposal proposal, Long companyId) {
        if (!proposal.getRootUser().getCompany().getId().equals(companyId)) {
            throw new ProposalNotFoundException();
        }
    }
}
