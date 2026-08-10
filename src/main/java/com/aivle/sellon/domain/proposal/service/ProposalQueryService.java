package com.aivle.sellon.domain.proposal.service;

import com.aivle.sellon.domain.proposal.dto.response.ProposalDetailResponse;
import com.aivle.sellon.domain.proposal.dto.response.ProposalResponse;
import com.aivle.sellon.domain.proposal.entity.Proposal;
import com.aivle.sellon.domain.proposal.exception.ProposalNotFoundException;
import com.aivle.sellon.domain.proposal.repository.ProposalRepository;
import com.aivle.sellon.domain.proposal.service.support.ProposalAccessGuard;
import com.aivle.sellon.domain.proposal.service.support.ProposalResponseMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// 개선안 리포트 조회 전용.
@Service
@RequiredArgsConstructor
public class ProposalQueryService {

    private final ProposalRepository proposalRepository;
    private final ProposalAccessGuard accessGuard;
    private final ProposalResponseMapper responseMapper;

    // 같은 회사 소속이면 루트/일반 계정 상관없이 조회 가능
    @Transactional(readOnly = true)
    public List<ProposalResponse> getProposals(Long companyId) {
        return proposalRepository.findByRootUser_Company_Id(companyId).stream()
            .map(responseMapper::toResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public ProposalDetailResponse getProposalDetail(Long reportKey, Long companyId) {
        Proposal proposal = proposalRepository.findById(reportKey)
            .orElseThrow(ProposalNotFoundException::new);
        accessGuard.requireCompanyAccess(proposal, companyId);
        return responseMapper.toDetailResponse(proposal);
    }
}
