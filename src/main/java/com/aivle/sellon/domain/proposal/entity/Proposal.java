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

// mq_events.md §4 (ai.anomaly.analyzed) 확정 스펙 기준으로 재설계.
// report_url/product_sku/product_name/cs_summary는 실제 계약에 없어 제거했다
// (ddl-auto: update라 DB에는 옛 컬럼이 남아있을 수 있음 — 필요시 수동 정리).
// 테이블명을 report -> proposal로 분리 (CS PR 리뷰 반영). 월간 리포트 엔티티도 @Table(name = "report")를
// 써서 한 테이블에 두 엔티티 컬럼이 섞여 있었고, PK 충돌로 report_mail_delivery의 FK가 엉뚱한 컬럼을
// 참조하는 버그까지 있었다(월간 리포트 팀에 별도 공유 필요).
@Entity
@Table(name = "proposal")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Proposal extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "report_key")
    private Long reportKey;

    @Column(name = "alert_id", length = 100, nullable = false, unique = true)
    private String alertId;

    // occurredAt이 아니라 payload.detected_at을 써야 한다 (mq_events.md §3 경고)
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

    // recommendation.proposal.rationale — AI 인사이트 리포트의 "CS 문의 근거 요약"에 대응
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

    // evaluator.passed — "검증된 개선안" 배지 표시 여부에 대응
    @Column(name = "evaluator_passed", nullable = false)
    private boolean evaluatorPassed;

    @Enumerated(EnumType.STRING)
    @Column(name = "hitl_status", length = 20, nullable = false)
    private HitlStatus hitlStatus = HitlStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "root_user_id", nullable = false)
    private User rootUser;

    public static Proposal of(User rootUser, String alertId) {
        Proposal entity = new Proposal();
        entity.rootUser = rootUser;
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
}
