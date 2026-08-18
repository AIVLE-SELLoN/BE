package com.aivle.sellon.domain.guideline.entity;

import com.aivle.sellon.global.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 운영 MD가 가이드라인 상세 페이지에서 승인하며 남기는 코멘트.
 * 가이드라인과 1:1이며, 레코드 존재 여부가 곧 승인 여부다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GuidelineApproval extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guideline_id", nullable = false, unique = true)
    private Guideline guideline;

    @Column(nullable = false, length = 1000)
    private String comment;

    private GuidelineApproval(Guideline guideline, String comment) {
        this.guideline = guideline;
        this.comment = comment;
    }

    public static GuidelineApproval create(Guideline guideline, String comment) {
        return new GuidelineApproval(guideline, comment);
    }

    public void updateComment(String comment) {
        this.comment = comment;
    }
}
