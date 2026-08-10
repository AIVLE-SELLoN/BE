package com.aivle.sellon.domain.proposal.entity;

import com.aivle.sellon.domain.proposal.enums.HitlStatus;
import com.aivle.sellon.global.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "report_accept_history")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProposalAcceptHistory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "report_accept_history_key")
    private Long proposalAcceptHistoryKey;

    @Column(name = "improved_content")
    private String improvedContent;

    @Column(name = "improved_prev_content")
    private String improvedPrevContent;

    // 승인 시점의 "적용된 개선안"(AI 제안 원문) 스냅샷 — 개선안 히스토리 모달의 "적용된 개선안"에 대응.
    // Proposal.proposedContent는 재요청 시 덮어써질 수 있어서 승인 시점 값을 별도로 보관한다.
    @Column(name = "applied_proposed_content", length = 2000)
    private String appliedProposedContent;

    @Enumerated(EnumType.STRING)
    @Column(name = "hitl_status", length = 20, nullable = false)
    private HitlStatus hitlStatus;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "processed_by", length = 100)
    private String processedBy;

    @Column(name = "rejection_reason_code", length = 30)
    private String rejectionReasonCode;

    @Column(name = "rejection_reason_text", length = 500)
    private String rejectionReasonText;

    @Column(name = "rolled_back", nullable = false)
    private boolean rolledBack;

    @Column(name = "rolled_back_at")
    private LocalDateTime rolledBackAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_key", nullable = false)
    private Proposal proposal;

    public static ProposalAcceptHistory ofAccept(
        Proposal proposal,
        HitlStatus hitlStatus,
        String appliedProposedContent,
        String improvedContent,
        String improvedPrevContent,
        String processedBy
    ) {
        ProposalAcceptHistory entity = new ProposalAcceptHistory();
        entity.proposal = proposal;
        entity.hitlStatus = hitlStatus;
        entity.appliedProposedContent = appliedProposedContent;
        entity.improvedContent = improvedContent;
        entity.improvedPrevContent = improvedPrevContent;
        entity.processedAt = LocalDateTime.now();
        entity.processedBy = processedBy;
        return entity;
    }

    public void rollback() {
        this.rolledBack = true;
        this.rolledBackAt = LocalDateTime.now();
    }

    public static ProposalAcceptHistory ofReject(
        Proposal proposal,
        String rejectionReasonCode,
        String rejectionReasonText,
        String processedBy
    ) {
        ProposalAcceptHistory entity = new ProposalAcceptHistory();
        entity.proposal = proposal;
        entity.hitlStatus = HitlStatus.REJECTED;
        entity.rejectionReasonCode = rejectionReasonCode;
        entity.rejectionReasonText = rejectionReasonText;
        entity.processedAt = LocalDateTime.now();
        entity.processedBy = processedBy;
        return entity;
    }
}
