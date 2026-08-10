package com.aivle.sellon.domain.proposal.entity;

import com.aivle.sellon.global.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// mq_events.md §4.2 recommendation.citations[] = {inquiry_id, quote}에 대응.
// 기존 sourceField/verified는 실제 계약에 없어 제거 — 현재 citations는 항상 빈 배열로 온다.
@Entity
@Table(name = "report_evidence")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProposalEvidence extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "report_evidence_key")
    private Long proposalEvidenceKey;

    @Column(name = "inquiry_id", length = 50, nullable = false)
    private String inquiryId;

    @Column(name = "quote_text", length = 1000, nullable = false)
    private String quoteText;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_key", nullable = false)
    private Proposal proposal;

    public static ProposalEvidence of(Proposal proposal, String inquiryId, String quoteText) {
        ProposalEvidence entity = new ProposalEvidence();
        entity.proposal = proposal;
        entity.inquiryId = inquiryId;
        entity.quoteText = quoteText;
        return entity;
    }
}
