package com.aivle.sellon.domain.proposal.repository;

import com.aivle.sellon.domain.proposal.entity.ProposalEvidence;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProposalEvidenceRepository extends JpaRepository<ProposalEvidence, Long> {
    List<ProposalEvidence> findByProposal_ReportKey(Long reportKey);

    void deleteByProposal_ReportKey(Long reportKey);
}
