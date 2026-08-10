package com.aivle.sellon.domain.proposal.entity;

import com.aivle.sellon.domain.proposal.enums.ConfidenceLevel;
import com.aivle.sellon.domain.proposal.enums.HitlStatus;
import com.aivle.sellon.domain.user.entity.User;
import com.aivle.sellon.global.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "report")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Proposal extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "report_key")
    private Long reportKey;

    @Column(name = "alert_id", length = 100, nullable = false, unique = true)
    private String alertId;

    @Column(name = "recommendation_id", length = 100)
    private String recommendationId;

    @Column(name = "report_url", length = 255)
    private String proposalUrl;

    @Column(name = "product_sku", length = 100)
    private String productSku;

    @Column(name = "product_name", length = 255)
    private String productName;

    @Column(name = "cs_summary", length = 1000)
    private String csSummary;

    @Enumerated(EnumType.STRING)
    @Column(name = "confidence_level", length = 10)
    private ConfidenceLevel confidenceLevel;

    @Column(name = "confidence_description", length = 500)
    private String confidenceDescription;

    @Column(name = "similar_case", length = 1000)
    private String similarCase;

    @Column(name = "proposed_content", length = 2000)
    private String proposedContent;

    @Enumerated(EnumType.STRING)
    @Column(name = "hitl_status", length = 20, nullable = false)
    private HitlStatus hitlStatus = HitlStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "root_user_id", nullable = false)
    private User rootUser;

    public static Proposal of(User rootUser, String alertId, String recommendationId, String proposalUrl) {
        Proposal entity = new Proposal();
        entity.rootUser = rootUser;
        entity.alertId = alertId;
        entity.recommendationId = recommendationId;
        entity.proposalUrl = proposalUrl;
        entity.hitlStatus = HitlStatus.PENDING;
        return entity;
    }

    public void updateFromAi(String recommendationId, String proposalUrl) {
        this.recommendationId = recommendationId;
        this.proposalUrl = proposalUrl;
    }

    public void applyReportContent(
        String productSku,
        String productName,
        String csSummary,
        ConfidenceLevel confidenceLevel,
        String confidenceDescription,
        String similarCase,
        String proposedContent
    ) {
        this.productSku = productSku;
        this.productName = productName;
        this.csSummary = csSummary;
        this.confidenceLevel = confidenceLevel;
        this.confidenceDescription = confidenceDescription;
        this.similarCase = similarCase;
        this.proposedContent = proposedContent;
    }

    public void markReviewed(HitlStatus hitlStatus) {
        this.hitlStatus = hitlStatus;
    }
}
