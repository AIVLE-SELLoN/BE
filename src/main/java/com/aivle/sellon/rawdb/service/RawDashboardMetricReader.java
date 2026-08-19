package com.aivle.sellon.rawdb.service;

import com.aivle.sellon.rawdb.dto.ChannelMetricRow;
import com.aivle.sellon.rawdb.repository.RawCsRepository;
import com.aivle.sellon.rawdb.repository.RawOrderRepository;
import com.aivle.sellon.rawdb.repository.RawReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

// 대시보드 지표 3종을 raw DB 단일 트랜잭션에서 읽는다.
// 리포지토리 메서드의 @Transactional은 기본 전파(REQUIRED)라 이 트랜잭션에 합류하므로,
// 세 쿼리가 동일한 스냅샷을 공유한다.
@Component
@RequiredArgsConstructor
public class RawDashboardMetricReader {

    private final RawCsRepository rawCsRepository;
    private final RawOrderRepository rawOrderRepository;
    private final RawReviewRepository rawReviewRepository;

    @Transactional(transactionManager = "rawDbTransactionManager", readOnly = true)
    public RawDashboardMetrics read(OffsetDateTime timestampFromInclusive,
                                    OffsetDateTime timestampToExclusive,
                                    LocalDate dateFromInclusive,
                                    LocalDate dateToExclusive) {
        return new RawDashboardMetrics(
                rawCsRepository.findCountsByChannel(timestampFromInclusive, timestampToExclusive),
                rawOrderRepository.findTotalQuantitiesByChannel(dateFromInclusive, dateToExclusive),
                rawReviewRepository.findAverageRatingsByChannel(timestampFromInclusive, timestampToExclusive)
        );
    }

    public record RawDashboardMetrics(
            List<ChannelMetricRow.CsCount> csCounts,
            List<ChannelMetricRow.OrderQuantity> orderQuantities,
            List<ChannelMetricRow.ReviewRating> reviewRatings
    ) {
    }
}
