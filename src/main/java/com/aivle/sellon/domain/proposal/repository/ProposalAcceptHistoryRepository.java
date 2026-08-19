package com.aivle.sellon.domain.proposal.repository;

import com.aivle.sellon.domain.proposal.entity.ProposalAcceptHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProposalAcceptHistoryRepository extends JpaRepository<ProposalAcceptHistory, Long> {
    List<ProposalAcceptHistory> findByProposal_ReportKey(Long reportKey);

    List<ProposalAcceptHistory> findByProposal_RootUser_Company_IdOrderByProcessedAtDesc(Long companyId);
}
