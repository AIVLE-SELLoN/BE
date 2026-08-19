package com.aivle.sellon.domain.proposal.entity;

import com.aivle.sellon.domain.proposal.enums.Channel;
import com.aivle.sellon.domain.proposal.enums.ConfidenceLevel;
import com.aivle.sellon.domain.proposal.enums.HitlStatus;
import com.aivle.sellon.domain.proposal.enums.MainAspect;
import com.aivle.sellon.domain.proposal.enums.ProposalType;
import com.aivle.sellon.domain.proposal.enums.RecommendedAction;
import com.aivle.sellon.domain.proposal.enums.Verdict;
import com.aivle.sellon.domain.user.entity.User;
import com.aivle.sellon.global.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// alert_id 단독 유니크가 아니라 (company_id, alert_id) 복합 유니크로 회사 간 충돌을 막는다.
@Entity
@Table(name = "proposal", uniqueConstraints = @UniqueConstraint(
    name = "uk_proposal_company_alert", columnNames = {"company_id", "alert_id"}
))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Proposal extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "report_key")
    private Long reportKey;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(name = "alert_id", length = 100, nullable = false)
    private String alertId;

    @Column(name = "detected_at")
    private LocalDateTime detectedAt;

    @Column(name = "product_group_id", length = 100)
    private String productGroupId;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", length = 10)
    private Channel channel;

    @Enumerated(EnumType.STRING)
    @Column(name = "verdict", length = 20)
    private Verdict verdict;

    @Enumerated(EnumType.STRING)
    @Column(name = "main_aspect", length = 20)
    private MainAspect mainAspect;

    @Enumerated(EnumType.STRING)
    @Column(name = "recommended_action", length = 30)
    private RecommendedAction recommendedAction;

    @Column(name = "recommendation_id", length = 100)
    private String recommendationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "proposal_type", length = 20)
    private ProposalType proposalType;

    @Column(name = "target_field", length = 100)
    private String targetField;

    @Column(name = "current_text", length = 2000)
    private String currentText;

    @Column(name = "proposed_content", length = 2000)
    private String proposedContent;

    @Column(name = "rationale", length = 1000)
    private String rationale;

    @Column(name = "detailpage_grounded", nullable = false)
    private boolean detailpageGrounded;

    @Enumerated(EnumType.STRING)
    @Column(name = "confidence_level", length = 10)
    private ConfidenceLevel confidenceLevel;

    @Column(name = "confidence_description", length = 500)
    private String confidenceDescription;

    @Column(name = "similar_case", length = 1000)
    private String similarCase;

    @Column(name = "capped_by_detection", nullable = false)
    private boolean cappedByDetection;

    @Column(name = "evaluator_passed", nullable = false)
    private boolean evaluatorPassed;

    @Enumerated(EnumType.STRING)
    @Column(name = "hitl_status", length = 20, nullable = false)
    private HitlStatus hitlStatus = HitlStatus.PENDING;

    @Column(name = "raw_payload", columnDefinition = "TEXT")
    private String rawPayload;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "root_user_id", nullable = false)
    private User rootUser;

    public static Proposal of(User rootUser, String alertId) {
        Proposal entity = new Proposal();
        entity.rootUser = rootUser;
        entity.companyId = rootUser.getCompany().getId();
        entity.alertId = alertId;
        entity.hitlStatus = HitlStatus.PENDING;
        return entity;
    }

    public void applyAnalysisResult(
        LocalDateTime detectedAt,
        String productGroupId,
        Channel channel,
        Verdict verdict,
        MainAspect mainAspect,
        RecommendedAction recommendedAction,
        String recommendationId,
        ProposalType proposalType,
        String targetField,
        String currentText,
        String proposedContent,
        String rationale,
        boolean detailpageGrounded,
        ConfidenceLevel confidenceLevel,
        String confidenceDescription,
        String similarCase,
        boolean cappedByDetection,
        boolean evaluatorPassed
    ) {
        this.detectedAt = detectedAt;
        this.productGroupId = productGroupId;
        this.channel = channel;
        this.verdict = verdict;
        this.mainAspect = mainAspect;
        this.recommendedAction = recommendedAction;
        this.recommendationId = recommendationId;
        this.proposalType = proposalType;
        this.targetField = targetField;
        this.currentText = currentText;
        this.proposedContent = proposedContent;
        this.rationale = rationale;
        this.detailpageGrounded = detailpageGrounded;
        this.confidenceLevel = confidenceLevel;
        this.confidenceDescription = confidenceDescription;
        this.similarCase = similarCase;
        this.cappedByDetection = cappedByDetection;
        this.evaluatorPassed = evaluatorPassed;
    }

    public void markReviewed(HitlStatus hitlStatus) {
        this.hitlStatus = hitlStatus;
    }

    public void updateRawPayload(String rawPayload) {
        this.rawPayload = rawPayload;
    }
}
