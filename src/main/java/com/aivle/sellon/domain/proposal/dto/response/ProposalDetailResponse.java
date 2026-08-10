package com.aivle.sellon.domain.proposal.dto.response;

import com.aivle.sellon.domain.proposal.enums.Channel;
import com.aivle.sellon.domain.proposal.enums.ConfidenceLevel;
import com.aivle.sellon.domain.proposal.enums.HitlStatus;
import com.aivle.sellon.domain.proposal.enums.MainAspect;
import com.aivle.sellon.domain.proposal.enums.ProposalType;
import com.aivle.sellon.domain.proposal.enums.Verdict;

import java.time.LocalDateTime;
import java.util.List;

// AI 인사이트 리포트 첫 페이지 상세 조회 응답 (mq_events.md §4 확정 스펙 기준)
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
    String rationale, // "CS 문의 근거 요약" 자리
    ConfidenceLevel confidenceLevel,
    String confidenceDescription,
    boolean cappedByDetection,
    String similarCase,
    boolean detailpageGrounded,
    boolean evaluatorPassed, // "검증된 개선안" 배지 여부
    String proposedContent, // 개선안 입력창 초기값
    List<ProposalEvidenceResponse> evidences,
    HitlStatus hitlStatus
) {}
