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

    // "되돌리기" — 승인(APPROVED/EDITED_APPROVED) 이력만 실제 반영 대상이 있다. improvedPrevContent가
    // 그 승인 시점 "적용 전" 값이라 그걸로 상품 설명을 되돌린다. 반려 이력은 반영한 적이 없어 대상이 없다.
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
