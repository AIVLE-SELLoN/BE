package com.aivle.sellon.domain.proposal.entity;

import com.aivle.sellon.global.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "report_evidence")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProposalEvidence extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "report_evidence_key")
    private Long proposalEvidenceKey;

    @Column(name = "source_field", length = 50, nullable = false)
    private String sourceField;

    @Column(name = "quote_text", length = 1000, nullable = false)
    private String quoteText;

    @Column(name = "verified", nullable = false)
    private boolean verified;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_key", nullable = false)
    private Proposal proposal;

    public static ProposalEvidence of(Proposal proposal, String sourceField, String quoteText, boolean verified) {
        ProposalEvidence entity = new ProposalEvidence();
        entity.proposal = proposal;
        entity.sourceField = sourceField;
        entity.quoteText = quoteText;
        entity.verified = verified;
        return entity;
    }
}
