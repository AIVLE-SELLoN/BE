package com.aivle.sellon.domain.proposal.service;

import com.aivle.sellon.domain.proposal.dto.response.ProposalAcceptHistoryResponse;
import com.aivle.sellon.domain.proposal.entity.Proposal;
import com.aivle.sellon.domain.proposal.entity.ProposalAcceptHistory;
import com.aivle.sellon.domain.proposal.enums.HitlStatus;
import com.aivle.sellon.domain.proposal.exception.ProposalAcceptHistoryNotFoundException;
import com.aivle.sellon.domain.proposal.exception.ProposalNotFoundException;
import com.aivle.sellon.domain.proposal.repository.ProposalAcceptHistoryRepository;
import com.aivle.sellon.domain.proposal.repository.ProposalRepository;
import com.aivle.sellon.domain.proposal.service.support.ProposalAccessGuard;
import com.aivle.sellon.domain.proposal.service.support.ProposalProductDescriptionApplier;
import com.aivle.sellon.domain.proposal.service.support.ProposalResponseMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// 개선안 반영 이력 조회 및 롤백.
@Service
@RequiredArgsConstructor
public class ProposalHistoryService {

    private final ProposalRepository proposalRepository;
    private final ProposalAcceptHistoryRepository proposalAcceptHistoryRepository;
    private final ProposalAccessGuard accessGuard;
    private final ProposalProductDescriptionApplier productDescriptionApplier;
    private final ProposalResponseMapper responseMapper;

    @Transactional(readOnly = true)
    public List<ProposalAcceptHistoryResponse> getAcceptHistory(Long reportKey, Long companyId) {
        Proposal proposal = proposalRepository.findById(reportKey)
            .orElseThrow(ProposalNotFoundException::new);
        accessGuard.requireCompanyAccess(proposal, companyId);

        return proposalAcceptHistoryRepository.findByProposal_ReportKey(reportKey).stream()
            .map(responseMapper::toResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<ProposalAcceptHistoryResponse> getAllAcceptHistory(Long companyId) {
        return proposalAcceptHistoryRepository.findByProposal_RootUser_Company_IdOrderByProcessedAtDesc(companyId).stream()
            .map(responseMapper::toResponse)
            .toList();
    }

    @Transactional
    public ProposalAcceptHistoryResponse rollbackAcceptHistory(Long historyKey, Long companyId) {
        ProposalAcceptHistory history = proposalAcceptHistoryRepository.findById(historyKey)
            .orElseThrow(ProposalAcceptHistoryNotFoundException::new);
        Proposal proposal = history.getProposal();
        accessGuard.requireCompanyAccess(proposal, companyId);

        boolean wasApplied = history.getHitlStatus() == HitlStatus.APPROVED || history.getHitlStatus() == HitlStatus.EDITED_APPROVED;
        if (wasApplied && history.getImprovedPrevContent() != null) {
            productDescriptionApplier.apply(proposal, companyId, history.getImprovedPrevContent());
        }

        history.rollback();
        return responseMapper.toResponse(history);
    }
}
