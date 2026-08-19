package com.aivle.sellon.domain.channels.service.comparison;

import com.aivle.sellon.domain.channels.dto.response.*;
import com.aivle.sellon.domain.channels.entity.comparison.ChannelComparison;
import com.aivle.sellon.domain.channels.entity.comparison.ChannelInquiryTypeStat;
import com.aivle.sellon.domain.channels.entity.comparison.ChannelInsight;
import com.aivle.sellon.domain.channels.entity.comparison.ChannelMonthlyInquiry;
import com.aivle.sellon.domain.channels.entity.connection.UsersChannel;
import com.aivle.sellon.domain.channels.enums.InquiryType;
import com.aivle.sellon.domain.channels.exception.ChannelAccessDeniedException;
import com.aivle.sellon.domain.channels.exception.connection.UsersChannelNotFoundException;
import com.aivle.sellon.domain.channels.repository.comparison.ChannelComparisonRepository;
import com.aivle.sellon.domain.channels.repository.comparison.ChannelInquiryTypeStatRepository;
import com.aivle.sellon.domain.channels.repository.comparison.ChannelInsightRepository;
import com.aivle.sellon.domain.channels.repository.comparison.ChannelMonthlyInquiryRepository;
import com.aivle.sellon.domain.channels.repository.connection.UsersChannelRepository;
import com.aivle.sellon.domain.alert.entity.DetectionAlert;
import com.aivle.sellon.domain.alert.enums.AlertChannel;
import com.aivle.sellon.domain.alert.repository.DetectionAlertRepository;
import com.aivle.sellon.rawdb.entity.RawCs;
import com.aivle.sellon.rawdb.entity.RawOrder;
import com.aivle.sellon.rawdb.entity.RawReview;
import com.aivle.sellon.rawdb.repository.RawCsRepository;
import com.aivle.sellon.rawdb.repository.RawOrderRepository;
import com.aivle.sellon.rawdb.repository.RawReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * 「Raw DB 스키마 확정 (8/7)」 §1 소유권 기준 - main server(우리) 소유 cs/reviews 테이블을
 * 직접 읽어 문의/리뷰 통계를 계산하고, AI 노드(classification_worker) 소유
 * classified_item_aspect를 읽어 문의 유형(aspect) 분포를 계산한다.
 * 이 배달 방식은 REST(POST /api/v1/classify) 호출이 아니라 raw DB 폴링/write-back 구조다
 * (2026-08-11 확인 - classification_worker.py가 raw DB를 직접 읽고 쓴다).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChannelComparisonService {

    private final ChannelComparisonRepository channelComparisonRepository;
    private final ChannelInquiryTypeStatRepository channelInquiryTypeStatRepository;
    private final ChannelMonthlyInquiryRepository channelMonthlyInquiryRepository;
    private final ChannelInsightRepository channelInsightRepository;
    private final UsersChannelRepository usersChannelRepository;
    private final RawCsRepository rawCsInquiryRepository;
    private final RawReviewRepository rawReviewRepository;
    private final RawOrderRepository rawOrderRepository;
    private final DetectionAlertRepository detectionAlertRepository;

    /** 요약 지표(총 문의수/응답·해결 대체 지표/속성 분포/리뷰 감성) 산정 기준 - 화면 라벨 "최근 30일"과 일치시킨다. */
    private static final int RECENT_DAYS = 30;
    /** 월별 문의 추이 차트 산정 기준 - 화면 라벨 "최근 6개월"과 일치시킨다. */
    private static final int TREND_MONTHS = 6;

    @Transactional(readOnly = true)
    public List<ChannelComparisonResponse> getComparisons(Long companyId) {
        return channelComparisonRepository.findByUsersChannel_Company_Id(companyId).stream()
                .map(ChannelComparisonResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ChannelInquiryTypeResponse> getAspectDistribution(Long companyId, Long usersChannelKey, Integer limit) {
        verifyOwnership(usersChannelKey, companyId);
        return computeDistribution(usersChannelKey, limit);
    }

    @Transactional(readOnly = true)
    public List<ChannelInquiryTypeRadarResponse> getInquiryTypeRadar(Long companyId) {
        List<UsersChannel> channels = usersChannelRepository.findByCompany_Id(companyId);
        return channels.stream()
                .map(channel -> new ChannelInquiryTypeRadarResponse(
                        channel.getUsersChannelKey(),
                        channel.getChannelType(),
                        computeDistribution(channel.getUsersChannelKey(), null)
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ChannelMonthlyInquiryResponse> getMonthlyInquiries(Long companyId, Long usersChannelKey) {
        verifyOwnership(usersChannelKey, companyId);
        return channelMonthlyInquiryRepository.findByUsersChannel_UsersChannelKey(usersChannelKey).stream()
                .map(m -> new ChannelMonthlyInquiryResponse(m.getYearMonth(), m.getInquiryCount()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ChannelInsightResponse> getInsights(Long companyId, Long usersChannelKey) {
        verifyOwnership(usersChannelKey, companyId);
        return channelInsightRepository.findByUsersChannel_UsersChannelKeyOrderByDisplayOrderAsc(usersChannelKey).stream()
                .map(i -> new ChannelInsightResponse(i.getContent()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ChannelComparisonResponse> getAllForDetection() {
        return channelComparisonRepository.findAll().stream()
                .map(ChannelComparisonResponse::from)
                .toList();
    }

    @Transactional
    public void refreshComparisonData(Long companyId, Long usersChannelKey) {
        UsersChannel usersChannel = usersChannelRepository.findById(usersChannelKey)
                .orElseThrow(UsersChannelNotFoundException::new);
        if (!usersChannel.getCompany().getId().equals(companyId)) {
            throw new ChannelAccessDeniedException();
        }

        ChannelComparisonClient.ComparisonResult result = computeComparisonResult(usersChannel);

        upsertComparison(usersChannel, result);
        replaceInquiryTypeStats(usersChannel, result.inquiryTypeCounts());
        replaceMonthlyInquiries(usersChannel, result.monthlyCounts());
        replaceInsights(usersChannel, result.insights());
    }

    /**
     * raw DB(cs/reviews/orders/classified_item_aspect)만으로 전 필드를 자체 계산한다.
     * 원래 기획이던 응답시간/해결율/재문의율은 raw db에 원본 데이터가 없어(2026-08-12 원본
     * input_cs_inquiries.csv/input_reviews.csv로 확인) 대체 지표(문의 발생률/평균 평점/리뷰
     * 작성률, 전부 orders 대비 산출)로 교체했다 - 팀 컨펌 필요, PR에 별도 명시.
     */
    private ChannelComparisonClient.ComparisonResult computeComparisonResult(UsersChannel usersChannel) {
        Long companyId = usersChannel.getCompany().getId();
        String channelType = usersChannel.getChannelType();

        InquiryStats inquiryStats = computeInquiryStats(channelType);
        ReviewStats reviewStats = computeReviewStats(channelType);
        List<ChannelComparisonClient.InquiryTypeCount> inquiryTypeCounts = computeInquiryTypeCounts(companyId, channelType);
        int totalOrderQuantity = computeTotalOrderQuantity(channelType);

        List<Double> siblingPositiveRatios = siblingPositiveRatios(companyId, usersChannel.getUsersChannelKey());

        String totalInquiryComment = formatTotalInquiryComment(inquiryStats.changeRate());
        String aspectComment = formatAspectComment(channelType, inquiryTypeCounts);
        String sentimentComment = formatSentimentComment(reviewStats, siblingPositiveRatios);
        List<String> insights = computeInsights(inquiryStats, reviewStats, inquiryTypeCounts, siblingPositiveRatios);

        Double inquiryRatePerOrder = ratioPerOrder(inquiryStats.totalCount(), totalOrderQuantity);
        String inquiryRateComment = formatInquiryRateComment(inquiryRatePerOrder);
        String avgRatingComment = formatAvgRatingComment(reviewStats.avgRating());
        Double reviewRatePerOrder = ratioPerOrder(reviewStats.totalCount(), totalOrderQuantity);
        String reviewRateComment = formatReviewRateComment(reviewRatePerOrder);

        return new ChannelComparisonClient.ComparisonResult(
                inquiryStats.totalCount(), inquiryStats.changeRate(), totalInquiryComment,
                inquiryRatePerOrder, inquiryRateComment,
                reviewStats.avgRating(), avgRatingComment,
                reviewRatePerOrder, reviewRateComment,
                reviewStats.positiveRatio(), reviewStats.neutralRatio(), reviewStats.negativeRatio(),
                sentimentComment, aspectComment,
                inquiryTypeCounts,
                inquiryStats.monthlyCounts(),
                insights
        );
    }

    private Double ratioPerOrder(Integer count, int totalOrderQuantity) {
        if (count == null || totalOrderQuantity == 0) {
            return null;
        }
        return count * 100.0 / totalOrderQuantity;
    }

    /**
     * 판매량(orders) 대비 문의 발생률. 응답시간 데이터가 없어 대신 채택한 지표 -
     * "이 채널이 판매량 대비 CS 부담이 큰 채널인가"를 보여준다.
     */
    private static final double INQUIRY_RATE_HIGH_THRESHOLD = 10.0;

    private String formatInquiryRateComment(Double rate) {
        if (rate == null) {
            return "주문 데이터가 충분하지 않아요.";
        }
        if (rate >= INQUIRY_RATE_HIGH_THRESHOLD) {
            return "판매 대비 문의 발생률이 %.1f%%로 높은 편이에요 · 상품 이슈 점검 필요".formatted(rate);
        }
        return "판매 대비 문의 발생률 %.1f%%로 무난한 수준이에요.".formatted(rate);
    }

    private String formatAvgRatingComment(Double avgRating) {
        if (avgRating == null) {
            return "리뷰 데이터가 충분하지 않아요.";
        }
        if (avgRating >= 4.0) {
            return "평균 평점 %.1f점 · 우수한 수준이에요.".formatted(avgRating);
        }
        if (avgRating >= 3.0) {
            return "평균 평점 %.1f점으로 보통 수준이에요.".formatted(avgRating);
        }
        return "평균 평점 %.1f점 · 개선이 필요해요.".formatted(avgRating);
    }

    private String formatReviewRateComment(Double rate) {
        if (rate == null) {
            return "주문 데이터가 충분하지 않아요.";
        }
        return "판매 대비 리뷰 작성률 %.1f%%예요.".formatted(rate);
    }

    private int computeTotalOrderQuantity(String channelType) {
        return rawOrderRepository.findByChannelIdAndOrderDateGreaterThanEqual(
                        channelType, LocalDate.now().minusDays(RECENT_DAYS)).stream()
                .mapToInt(RawOrder::getQuantity)
                .sum();
    }

    private List<Double> siblingPositiveRatios(Long companyId, Long usersChannelKey) {
        return channelComparisonRepository.findByUsersChannel_Company_Id(companyId).stream()
                .filter(c -> !c.getUsersChannel().getUsersChannelKey().equals(usersChannelKey))
                .map(ChannelComparison::getPositiveRatio)
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    /**
     * "+12% vs 전월" 형식 - changeRate를 그대로 포맷팅. 비교할 전월 데이터가 없으면(채널 개설
     * 초기 등) "데이터 부족"으로 표시한다(프론트 목업의 지그재그 케이스와 동일한 처리).
     */
    private String formatTotalInquiryComment(Double changeRate) {
        if (changeRate == null) {
            return "데이터 부족";
        }
        return "%+.0f%% vs 전월".formatted(changeRate);
    }

    /**
     * 문의 유형(aspect) 1위 비중이 임계치를 넘으면 "편중형 패턴"으로, 아니면 "고른 분포"로 서술한다.
     * LLM 없이 우리가 이미 계산해둔 aspect 분포 숫자만으로 판단 가능한 규칙 기반 코멘트다.
     */
    private static final double ASPECT_CONCENTRATION_THRESHOLD = 0.5;

    private String formatAspectComment(String channelType, List<ChannelComparisonClient.InquiryTypeCount> counts) {
        int total = counts.stream().mapToInt(ChannelComparisonClient.InquiryTypeCount::count).sum();
        if (total == 0) {
            return "최근 문의 데이터가 충분하지 않아요.";
        }
        ChannelComparisonClient.InquiryTypeCount top = counts.stream()
                .max(Comparator.comparingInt(ChannelComparisonClient.InquiryTypeCount::count))
                .orElseThrow();
        double topRatio = top.count() * 1.0 / total;
        if (topRatio >= ASPECT_CONCENTRATION_THRESHOLD) {
            return "%s 채널에 %s 문의가 집중되어 있어요 · 편중형 패턴(채널 특성 요인 가능성)"
                    .formatted(channelType, top.inquireType().getLabel());
        }
        return "상대적으로 고른 분포 · 특정 유형에 쏠리지 않는 편이에요.";
    }

    /**
     * 같은 회사의 다른 채널들과 긍정 비율을 비교해 상대적 위치를 서술한다. 형제 채널의
     * positiveRatio는 그 채널이 마지막으로 refresh된 시점 기준 스냅샷이라 약간 stale할 수
     * 있지만, "어느 채널이 상대적으로 우위/열위인지" 판단에는 충분하다.
     */
    private String formatSentimentComment(ReviewStats reviewStats, List<Double> siblingPositiveRatios) {
        if (reviewStats.positiveRatio() == null) {
            return "리뷰 데이터가 충분하지 않아요.";
        }
        if (siblingPositiveRatios.isEmpty()) {
            return "긍정 비율 %.0f%%예요.".formatted(reviewStats.positiveRatio());
        }
        double maxSibling = siblingPositiveRatios.stream().mapToDouble(Double::doubleValue).max().orElseThrow();
        double minSibling = siblingPositiveRatios.stream().mapToDouble(Double::doubleValue).min().orElseThrow();
        if (reviewStats.positiveRatio() > maxSibling) {
            return "채널 중 긍정 비율이 가장 높아요 · 고객 경험 품질 우위";
        }
        if (reviewStats.positiveRatio() < minSibling) {
            return "채널 중 긍정 비율이 가장 낮아요 · 개선 필요";
        }
        return "긍정 비율 %.0f%%로 무난한 수준이에요.".formatted(reviewStats.positiveRatio());
    }

    /**
     * 프론트 목업의 "채널별 주요 인사이트" 카드용 규칙 기반 문장 리스트. 표본이 너무 적으면
     * 다른 판단 없이 "패턴 분석 유보" 한 줄만 내려주고, 충분하면 aspect 편중/문의량 급변/
     * 감성 비교 중 조건에 걸리는 것만 문장으로 추가한다(전부 우리가 이미 계산해둔 숫자 기반).
     */
    private static final int INSIGHT_MIN_SAMPLE = 30;
    private static final double INQUIRY_CHANGE_NOTABLE_THRESHOLD = 20.0;

    private List<String> computeInsights(
            InquiryStats inquiryStats, ReviewStats reviewStats,
            List<ChannelComparisonClient.InquiryTypeCount> inquiryTypeCounts,
            List<Double> siblingPositiveRatios
    ) {
        List<String> insights = new java.util.ArrayList<>();

        if (inquiryStats.totalCount() < INSIGHT_MIN_SAMPLE) {
            insights.add("최근 문의 데이터가 %d건으로 적어 패턴 분석은 유보돼요.".formatted(inquiryStats.totalCount()));
            return insights;
        }

        int aspectTotal = inquiryTypeCounts.stream().mapToInt(ChannelComparisonClient.InquiryTypeCount::count).sum();
        if (aspectTotal > 0) {
            ChannelComparisonClient.InquiryTypeCount top = inquiryTypeCounts.stream()
                    .max(Comparator.comparingInt(ChannelComparisonClient.InquiryTypeCount::count))
                    .orElseThrow();
            double topRatio = top.count() * 100.0 / aspectTotal;
            if (topRatio / 100.0 >= ASPECT_CONCENTRATION_THRESHOLD) {
                insights.add("%s 문의가 전체의 %.0f%%로 집중 — 편중형 패턴".formatted(top.inquireType().getLabel(), topRatio));
            }
        }

        if (inquiryStats.changeRate() != null) {
            if (inquiryStats.changeRate() >= INQUIRY_CHANGE_NOTABLE_THRESHOLD) {
                insights.add("문의량이 전월 대비 %.0f%% 증가 — 원인 모니터링 필요".formatted(inquiryStats.changeRate()));
            } else if (inquiryStats.changeRate() <= -INQUIRY_CHANGE_NOTABLE_THRESHOLD) {
                insights.add("문의량이 전월 대비 %.0f%% 감소".formatted(-inquiryStats.changeRate()));
            }
        }

        if (reviewStats.positiveRatio() != null && !siblingPositiveRatios.isEmpty()) {
            double maxSibling = siblingPositiveRatios.stream().mapToDouble(Double::doubleValue).max().orElseThrow();
            double minSibling = siblingPositiveRatios.stream().mapToDouble(Double::doubleValue).min().orElseThrow();
            if (reviewStats.positiveRatio() > maxSibling) {
                insights.add("긍정 감성 %.0f%%로 채널 중 가장 높음".formatted(reviewStats.positiveRatio()));
            } else if (reviewStats.positiveRatio() < minSibling) {
                insights.add("긍정 감성 %.0f%%로 채널 중 가장 낮음 — 개선 필요".formatted(reviewStats.positiveRatio()));
            }
        }

        return insights;
    }

    private InquiryStats computeInquiryStats(String channelType) {
        OffsetDateTime now = OffsetDateTime.now();
        // 월별 추이(최근 6개월)에 필요한 범위로 한 번만 조회하고, 총 문의수(최근 30일)는 그 안에서 다시 걸러낸다.
        List<RawCs> inquiries = rawCsInquiryRepository.findByChannelIdAndInquiredAtGreaterThanEqual(
                channelType, now.minusMonths(TREND_MONTHS));

        Map<YearMonth, Integer> countsByMonth = new TreeMap<>();
        int totalCount = 0;
        OffsetDateTime recentCutoff = now.minusDays(RECENT_DAYS);
        for (RawCs inquiry : inquiries) {
            if (inquiry.getInquiredAt() == null) {
                continue;
            }
            YearMonth yearMonth = YearMonth.from(inquiry.getInquiredAt());
            countsByMonth.merge(yearMonth, 1, Integer::sum);
            if (!inquiry.getInquiredAt().isBefore(recentCutoff)) {
                totalCount++;
            }
        }

        List<ChannelComparisonClient.MonthlyCount> monthlyCounts = countsByMonth.entrySet().stream()
                .map(e -> new ChannelComparisonClient.MonthlyCount(e.getKey().toString(), e.getValue()))
                .toList();

        Double changeRate = computeChangeRate(countsByMonth);

        return new InquiryStats(totalCount, changeRate, monthlyCounts);
    }

    private Double computeChangeRate(Map<YearMonth, Integer> countsByMonth) {
        if (countsByMonth.size() < 2) {
            return null;
        }
        List<YearMonth> months = countsByMonth.keySet().stream().sorted().toList();
        YearMonth latest = months.get(months.size() - 1);
        YearMonth previous = months.get(months.size() - 2);
        int latestCount = countsByMonth.get(latest);
        int previousCount = countsByMonth.get(previous);
        if (previousCount == 0) {
            return null;
        }
        return (latestCount - previousCount) * 100.0 / previousCount;
    }

    private ReviewStats computeReviewStats(String channelType) {
        List<RawReview> reviews = rawReviewRepository.findByChannelIdAndCreatedAtGreaterThanEqual(
                channelType, OffsetDateTime.now().minusDays(RECENT_DAYS));

        int positive = 0;
        int neutral = 0;
        int negative = 0;
        int total = 0;
        long ratingSum = 0;

        for (RawReview review : reviews) {
            Short rating = review.getRating();
            if (rating == null) {
                continue;
            }
            total++;
            ratingSum += rating;
            if (rating >= 4) {
                positive++;
            } else if (rating == 3) {
                neutral++;
            } else {
                negative++;
            }
        }

        if (total == 0) {
            return new ReviewStats(null, null, null, 0, null);
        }
        return new ReviewStats(
                positive * 100.0 / total, neutral * 100.0 / total, negative * 100.0 / total,
                total, ratingSum * 1.0 / total
        );
    }

    /**
     * 문의 유형(aspect) 분포 - 기존엔 raw db classified_item_aspect(문의 건별 AI 분류 라벨)를 직접
     * 집계했으나, develop에서 AI 분류 결과 전달 방식이 RabbitMQ 기반 DetectionAlert(이상탐지 이벤트
     * 단위)로 바뀌면서 원본 데이터(classified_item_aspect)가 사라졌다. 문의 건별 전체 분포는 더 이상
     * 복원할 수 없어, DetectionAlert.mainAspect(이상탐지로 플래그된 알림)의 stats.curTotal을
     * aspect별로 합산하는 근사치로 대체한다 - "탐지된 이상 패턴" 기준이라 실제 전체 문의 비중과는
     * 다를 수 있음(AI팀에 문의 건별 aspect 라벨 재노출 가능 여부 별도 확인 필요).
     */
    private List<ChannelComparisonClient.InquiryTypeCount> computeInquiryTypeCounts(Long companyId, String channelType) {
        AlertChannel alertChannel;
        try {
            alertChannel = AlertChannel.valueOf(channelType);
        } catch (IllegalArgumentException e) {
            return List.of();
        }

        List<DetectionAlert> alerts = detectionAlertRepository.findByCompany_IdAndChannel(companyId, alertChannel);

        Map<InquiryType, Integer> countsByType = new java.util.EnumMap<>(InquiryType.class);
        for (DetectionAlert alert : alerts) {
            InquiryType inquiryType;
            try {
                inquiryType = InquiryType.fromLabel(alert.getMainAspect().getJsonValue());
            } catch (IllegalArgumentException e) {
                continue;
            }
            countsByType.merge(inquiryType, alert.getStats().getCurTotal(), Integer::sum);
        }

        return countsByType.entrySet().stream()
                .map(e -> new ChannelComparisonClient.InquiryTypeCount(e.getKey(), e.getValue()))
                .toList();
    }

    private record InquiryStats(Integer totalCount, Double changeRate, List<ChannelComparisonClient.MonthlyCount> monthlyCounts) {
    }

    private record ReviewStats(Double positiveRatio, Double neutralRatio, Double negativeRatio,
                                Integer totalCount, Double avgRating) {
    }

    private void verifyOwnership(Long usersChannelKey, Long companyId) {
        UsersChannel usersChannel = usersChannelRepository.findById(usersChannelKey)
                .orElseThrow(UsersChannelNotFoundException::new);
        if (!usersChannel.getCompany().getId().equals(companyId)) {
            throw new ChannelAccessDeniedException();
        }
    }

    private void upsertComparison(UsersChannel usersChannel, ChannelComparisonClient.ComparisonResult result) {
        ChannelComparison comparison = channelComparisonRepository.findByUsersChannel_UsersChannelKey(usersChannel.getUsersChannelKey())
                .orElse(null);

        if (comparison == null) {
            channelComparisonRepository.save(ChannelComparison.of(
                    usersChannel,
                    result.totalInquiryCount(), result.totalInquiryChangeRate(), result.totalInquiryComment(),
                    result.inquiryRatePerOrder(), result.inquiryRateComment(),
                    result.avgRating(), result.avgRatingComment(),
                    result.reviewRatePerOrder(), result.reviewRateComment(),
                    result.positiveRatio(), result.neutralRatio(), result.negativeRatio(),
                    result.sentimentComment(), result.aspectComment()
            ));
            return;
        }

        comparison.update(
                result.totalInquiryCount(), result.totalInquiryChangeRate(), result.totalInquiryComment(),
                result.inquiryRatePerOrder(), result.inquiryRateComment(),
                result.avgRating(), result.avgRatingComment(),
                result.reviewRatePerOrder(), result.reviewRateComment(),
                result.positiveRatio(), result.neutralRatio(), result.negativeRatio(),
                result.sentimentComment(), result.aspectComment()
        );
    }

    private void replaceInquiryTypeStats(UsersChannel usersChannel, List<ChannelComparisonClient.InquiryTypeCount> counts) {
        channelInquiryTypeStatRepository.deleteByUsersChannel_UsersChannelKey(usersChannel.getUsersChannelKey());
        List<ChannelInquiryTypeStat> entities = counts.stream()
                .map(c -> ChannelInquiryTypeStat.of(usersChannel, c.inquireType(), c.count()))
                .toList();
        channelInquiryTypeStatRepository.saveAll(entities);
    }

    private void replaceMonthlyInquiries(UsersChannel usersChannel, List<ChannelComparisonClient.MonthlyCount> counts) {
        channelMonthlyInquiryRepository.deleteByUsersChannel_UsersChannelKey(usersChannel.getUsersChannelKey());
        List<ChannelMonthlyInquiry> entities = counts.stream()
                .map(c -> ChannelMonthlyInquiry.of(usersChannel, YearMonth.parse(c.yearMonth()), c.count()))
                .toList();
        channelMonthlyInquiryRepository.saveAll(entities);
    }

    private void replaceInsights(UsersChannel usersChannel, List<String> insights) {
        channelInsightRepository.deleteByUsersChannel_UsersChannelKey(usersChannel.getUsersChannelKey());
        List<ChannelInsight> entities = java.util.stream.IntStream.range(0, insights.size())
                .mapToObj(i -> ChannelInsight.of(usersChannel, insights.get(i), i))
                .toList();
        channelInsightRepository.saveAll(entities);
    }

    private List<ChannelInquiryTypeResponse> computeDistribution(Long usersChannelKey, Integer limit) {
        List<ChannelInquiryTypeStat> stats = channelInquiryTypeStatRepository.findByUsersChannel_UsersChannelKey(usersChannelKey);
        int total = stats.stream().mapToInt(ChannelInquiryTypeStat::getInquiryCount).sum();

        List<ChannelInquiryTypeResponse> distribution = stats.stream()
                .sorted(Comparator.comparing(ChannelInquiryTypeStat::getInquiryCount).reversed())
                .map(s -> new ChannelInquiryTypeResponse(
                        s.getInquireType(),
                        s.getInquiryCount(),
                        total == 0 ? 0.0 : s.getInquiryCount() * 100.0 / total
                ))
                .toList();

        return limit != null ? distribution.stream().limit(limit).toList() : distribution;
    }
}
