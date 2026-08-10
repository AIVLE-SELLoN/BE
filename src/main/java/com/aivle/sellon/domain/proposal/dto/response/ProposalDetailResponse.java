package com.aivle.sellon.domain.proposal.dto.response;

import com.aivle.sellon.domain.proposal.enums.Channel;
import com.aivle.sellon.domain.proposal.enums.ConfidenceLevel;
import com.aivle.sellon.domain.proposal.enums.HitlStatus;
import com.aivle.sellon.domain.proposal.enums.MainAspect;
import com.aivle.sellon.domain.proposal.enums.ProposalType;
import com.aivle.sellon.domain.proposal.enums.Verdict;

import java.time.LocalDateTime;
import java.util.List;

public record ProposalDetailResponse(
    Long reportKey,
    String alertId,
    LocalDateTime detectedAt,
    String productGroupId,
    Channel channel,
    Verdict verdict,
    MainAspect mainAspect,
    ProposalType proposalType,
    String targetField,
    String currentText,
    String rationale,
    ConfidenceLevel confidenceLevel,
    String confidenceDescription,
    boolean cappedByDetection,
    String similarCase,
    boolean detailpageGrounded,
    boolean evaluatorPassed,
    String proposedContent,
    List<ProposalEvidenceResponse> evidences,
    HitlStatus hitlStatus
) {}
