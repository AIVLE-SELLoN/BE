package com.aivle.sellon.domain.channels.entity.productmapping;

import com.aivle.sellon.domain.company.entity.Company;
import com.aivle.sellon.global.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 매칭 툴(docker_mapping_tool)의 review_queue.csv 한 행 — 자동으로 같음/다름을 확정하지 못하고
 * "보류(hold)"로 넘어온 두 채널상품 쌍. 사람이 직접 확인해서 resolve 처리해야 한다.
 * 두 상품이 서로 다른 채널(channel_a/channel_b)에 속할 수 있어 특정 UsersChannel이 아닌 Company 단위로 스코프한다.
 */
@Entity
@Table(name = "product_mapping_review_item")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductMappingReviewItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_mapping_review_item_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "channel_a", nullable = false)
    private String channelA;

    @Column(name = "product_key_a", nullable = false)
    private String productKeyA;

    @Column(name = "channel_b", nullable = false)
    private String channelB;

    @Column(name = "product_key_b", nullable = false)
    private String productKeyB;

    @Column(name = "rule_score")
    private Double ruleScore;

    @Column(name = "emb_score")
    private Double embScore;

    @Column(name = "verdict")
    private String verdict;

    @Column(name = "basis")
    private String basis;

    @Column(name = "resolved", nullable = false)
    private boolean resolved;

    public static ProductMappingReviewItem of(Company company, String channelA, String productKeyA,
                                                String channelB, String productKeyB,
                                                Double ruleScore, Double embScore, String verdict, String basis) {
        ProductMappingReviewItem entity = new ProductMappingReviewItem();
        entity.company = company;
        entity.channelA = channelA;
        entity.productKeyA = productKeyA;
        entity.channelB = channelB;
        entity.productKeyB = productKeyB;
        entity.ruleScore = ruleScore;
        entity.embScore = embScore;
        entity.verdict = verdict;
        entity.basis = basis;
        entity.resolved = false;
        return entity;
    }

    public void resolve() {
        this.resolved = true;
    }
}
