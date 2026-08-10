package com.aivle.sellon.domain.proposal.controller;

import com.aivle.sellon.domain.proposal.dto.response.ProposalAcceptHistoryResponse;
import com.aivle.sellon.domain.proposal.dto.request.ProposalAcceptRequest;
import com.aivle.sellon.domain.proposal.dto.request.ProposalRegenerateRequest;
import com.aivle.sellon.domain.proposal.dto.request.ProposalRejectRequest;
import com.aivle.sellon.domain.proposal.dto.response.ProposalDetailResponse;
import com.aivle.sellon.domain.proposal.dto.response.ProposalResponse;
import com.aivle.sellon.domain.proposal.service.ProposalService;
import com.aivle.sellon.global.security.principal.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
public class ProposalController {

    private final ProposalService proposalService;

    // 같은 회사면 루트/일반 계정 모두 조회 가능 (company 기준 스코핑)
    @GetMapping
    public ResponseEntity<List<ProposalResponse>> getProposals(@AuthenticationPrincipal UserPrincipal principal) {
        List<ProposalResponse> proposals = proposalService.getProposals(principal.getCompanyId());
        return ResponseEntity.ok(proposals);
    }

    @GetMapping("/{reportKey}")
    public ResponseEntity<ProposalDetailResponse> getProposalDetail(
        @PathVariable Long reportKey,
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        ProposalDetailResponse response = proposalService.getProposalDetail(reportKey, principal.getCompanyId());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{reportKey}/regenerate")
    public ResponseEntity<ProposalDetailResponse> regenerateProposal(
        @PathVariable Long reportKey,
        @RequestBody ProposalRegenerateRequest request,
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        ProposalDetailResponse response = proposalService.regenerateProposal(reportKey, request, principal.getCompanyId());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{reportKey}/accept")
    public ResponseEntity<ProposalAcceptHistoryResponse> acceptProposal(
        @PathVariable Long reportKey,
        @RequestBody ProposalAcceptRequest request,
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        ProposalAcceptHistoryResponse response = proposalService.acceptProposal(reportKey, request, principal.getCompanyId());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{reportKey}/reject")
    public ResponseEntity<ProposalAcceptHistoryResponse> rejectProposal(
        @PathVariable Long reportKey,
        @RequestBody ProposalRejectRequest request,
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        ProposalAcceptHistoryResponse response = proposalService.rejectProposal(reportKey, request, principal.getCompanyId());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{reportKey}/history")
    public ResponseEntity<List<ProposalAcceptHistoryResponse>> getAcceptHistory(
        @PathVariable Long reportKey,
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        List<ProposalAcceptHistoryResponse> history = proposalService.getAcceptHistory(reportKey, principal.getCompanyId());
        return ResponseEntity.ok(history);
    }

    @GetMapping("/history")
    public ResponseEntity<List<ProposalAcceptHistoryResponse>> getAllAcceptHistory(
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        List<ProposalAcceptHistoryResponse> history = proposalService.getAllAcceptHistory(principal.getCompanyId());
        return ResponseEntity.ok(history);
    }

    @PostMapping("/{historyKey}/rollback")
    public ResponseEntity<ProposalAcceptHistoryResponse> rollbackAcceptHistory(
        @PathVariable Long historyKey,
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        ProposalAcceptHistoryResponse response = proposalService.rollbackAcceptHistory(historyKey, principal.getCompanyId());
        return ResponseEntity.ok(response);
    }
}
