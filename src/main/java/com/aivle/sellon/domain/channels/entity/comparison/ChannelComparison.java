package com.aivle.sellon.domain.channels.entity.comparison;

import com.aivle.sellon.domain.channels.entity.connection.UsersChannel;
import com.aivle.sellon.global.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 채널별 핵심 지표 요약(최근 N일 기준 스냅샷). Agent2가 계산한 집계·코멘트를
 * Normalized Fact-Dimension -> Serving 단계에서 저장해두는 테이블.
 */
@Entity
@Table(name = "channel_comparison")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChannelComparison extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "comparison_key")
    private Long comparisonKey;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "users_channel_key", nullable = false)
    private UsersChannel usersChannel;

    @Column(name = "total_inquiry_count")
    private Integer totalInquiryCount;

    @Column(name = "total_inquiry_change_rate")
    private Double totalInquiryChangeRate;

    @Column(name = "total_inquiry_comment")
    private String totalInquiryComment;

    /**
     * 응답시간/해결율/재문의율은 raw db에 원본 데이터가 존재하지 않아(2026-08-12 원본 CSV로 확인)
     * 대체 지표로 교체 - 판매량(orders) 대비 문의 발생률/평균 평점/리뷰 작성률.
     */
    @Column(name = "inquiry_rate_per_order")
    private Double inquiryRatePerOrder;

    @Column(name = "inquiry_rate_comment")
    private String inquiryRateComment;

    @Column(name = "avg_rating")
    private Double avgRating;

    @Column(name = "avg_rating_comment")
    private String avgRatingComment;

    @Column(name = "review_rate_per_order")
    private Double reviewRatePerOrder;

    @Column(name = "review_rate_comment")
    private String reviewRateComment;

    @Column(name = "positive_ratio")
    private Double positiveRatio;

    @Column(name = "neutral_ratio")
    private Double neutralRatio;

    @Column(name = "negative_ratio")
    private Double negativeRatio;

    @Column(name = "sentiment_comment")
    private String sentimentComment;

    @Column(name = "aspect_comment")
    private String aspectComment;

    public static ChannelComparison of(UsersChannel usersChannel, Integer totalInquiryCount,
                                        Double totalInquiryChangeRate, String totalInquiryComment,
                                        Double inquiryRatePerOrder, String inquiryRateComment,
                                        Double avgRating, String avgRatingComment,
                                        Double reviewRatePerOrder, String reviewRateComment,
                                        Double positiveRatio, Double neutralRatio, Double negativeRatio,
                                        String sentimentComment, String aspectComment) {
        ChannelComparison entity = new ChannelComparison();
        entity.usersChannel = usersChannel;
        entity.apply(totalInquiryCount, totalInquiryChangeRate, totalInquiryComment,
                inquiryRatePerOrder, inquiryRateComment,
                avgRating, avgRatingComment,
                reviewRatePerOrder, reviewRateComment,
                positiveRatio, neutralRatio, negativeRatio,
                sentimentComment, aspectComment);
        return entity;
    }

    /**
     * 재조회 결과로 기존 스냅샷을 덮어쓸 때 사용 (upsert의 update 경로).
     */
    public void update(Integer totalInquiryCount, Double totalInquiryChangeRate, String totalInquiryComment,
                        Double inquiryRatePerOrder, String inquiryRateComment,
                        Double avgRating, String avgRatingComment,
                        Double reviewRatePerOrder, String reviewRateComment,
                        Double positiveRatio, Double neutralRatio, Double negativeRatio,
                        String sentimentComment, String aspectComment) {
        apply(totalInquiryCount, totalInquiryChangeRate, totalInquiryComment,
                inquiryRatePerOrder, inquiryRateComment,
                avgRating, avgRatingComment,
                reviewRatePerOrder, reviewRateComment,
                positiveRatio, neutralRatio, negativeRatio,
                sentimentComment, aspectComment);
    }

    private void apply(Integer totalInquiryCount, Double totalInquiryChangeRate, String totalInquiryComment,
                        Double inquiryRatePerOrder, String inquiryRateComment,
                        Double avgRating, String avgRatingComment,
                        Double reviewRatePerOrder, String reviewRateComment,
                        Double positiveRatio, Double neutralRatio, Double negativeRatio,
                        String sentimentComment, String aspectComment) {
        this.totalInquiryCount = totalInquiryCount;
        this.totalInquiryChangeRate = totalInquiryChangeRate;
        this.totalInquiryComment = totalInquiryComment;
        this.inquiryRatePerOrder = inquiryRatePerOrder;
        this.inquiryRateComment = inquiryRateComment;
        this.avgRating = avgRating;
        this.avgRatingComment = avgRatingComment;
        this.reviewRatePerOrder = reviewRatePerOrder;
        this.reviewRateComment = reviewRateComment;
        this.positiveRatio = positiveRatio;
        this.neutralRatio = neutralRatio;
        this.negativeRatio = negativeRatio;
        this.sentimentComment = sentimentComment;
        this.aspectComment = aspectComment;
    }
}
