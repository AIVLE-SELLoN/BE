package com.aivle.sellon.domain.proposal.controller;

import com.aivle.sellon.domain.proposal.dto.response.ProposalAcceptHistoryResponse;
import com.aivle.sellon.domain.proposal.dto.request.ProposalAcceptRequest;
import com.aivle.sellon.domain.proposal.dto.request.ProposalRegenerateRequest;
import com.aivle.sellon.domain.proposal.dto.request.ProposalRejectRequest;
import com.aivle.sellon.domain.proposal.dto.response.ProposalDetailResponse;
import com.aivle.sellon.domain.proposal.dto.response.ProposalResponse;
import com.aivle.sellon.domain.proposal.service.ProposalHistoryService;
import com.aivle.sellon.domain.proposal.service.ProposalQueryService;
import com.aivle.sellon.domain.proposal.service.ProposalReviewService;
import com.aivle.sellon.global.common.ApiResponse;
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

    private final ProposalQueryService proposalQueryService;
    private final ProposalReviewService proposalReviewService;
    private final ProposalHistoryService proposalHistoryService;

    // 같은 회사면 루트/일반 계정 모두 조회 가능 (company 기준 스코핑)
    @GetMapping
    public ResponseEntity<ApiResponse<List<ProposalResponse>>> getProposals(@AuthenticationPrincipal UserPrincipal principal) {
        List<ProposalResponse> proposals = proposalQueryService.getProposals(principal.getCompanyId());
        return ApiResponse.ok(proposals);
    }

    @GetMapping("/{reportKey}")
    public ResponseEntity<ApiResponse<ProposalDetailResponse>> getProposalDetail(
        @PathVariable Long reportKey,
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        ProposalDetailResponse response = proposalQueryService.getProposalDetail(reportKey, principal.getCompanyId());
        return ApiResponse.ok(response);
    }

    @PostMapping("/{reportKey}/regenerate")
    public ResponseEntity<ApiResponse<ProposalAcceptHistoryResponse>> regenerateProposal(
        @PathVariable Long reportKey,
        @RequestBody ProposalRegenerateRequest request,
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        ProposalAcceptHistoryResponse response = proposalReviewService.regenerateProposal(reportKey, request, principal.getCompanyId());
        return ApiResponse.ok(response);
    }

    @PostMapping("/{reportKey}/accept")
    public ResponseEntity<ApiResponse<ProposalAcceptHistoryResponse>> acceptProposal(
        @PathVariable Long reportKey,
        @RequestBody ProposalAcceptRequest request,
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        ProposalAcceptHistoryResponse response = proposalReviewService.acceptProposal(reportKey, request, principal.getCompanyId());
        return ApiResponse.ok(response);
    }

    @PostMapping("/{reportKey}/reject")
    public ResponseEntity<ApiResponse<ProposalAcceptHistoryResponse>> rejectProposal(
        @PathVariable Long reportKey,
        @RequestBody ProposalRejectRequest request,
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        ProposalAcceptHistoryResponse response = proposalReviewService.rejectProposal(reportKey, request, principal.getCompanyId());
        return ApiResponse.ok(response);
    }

    @GetMapping("/{reportKey}/history")
    public ResponseEntity<ApiResponse<List<ProposalAcceptHistoryResponse>>> getAcceptHistory(
        @PathVariable Long reportKey,
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        List<ProposalAcceptHistoryResponse> history = proposalHistoryService.getAcceptHistory(reportKey, principal.getCompanyId());
        return ApiResponse.ok(history);
    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<ProposalAcceptHistoryResponse>>> getAllAcceptHistory(
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        List<ProposalAcceptHistoryResponse> history = proposalHistoryService.getAllAcceptHistory(principal.getCompanyId());
        return ApiResponse.ok(history);
    }

    @PostMapping("/{historyKey}/rollback")
    public ResponseEntity<ApiResponse<ProposalAcceptHistoryResponse>> rollbackAcceptHistory(
        @PathVariable Long historyKey,
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        ProposalAcceptHistoryResponse response = proposalHistoryService.rollbackAcceptHistory(historyKey, principal.getCompanyId());
        return ApiResponse.ok(response);
    }
}
