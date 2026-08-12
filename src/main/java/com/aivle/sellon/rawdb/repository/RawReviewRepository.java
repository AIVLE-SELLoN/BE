package com.aivle.sellon.rawdb.repository;

import com.aivle.sellon.rawdb.dto.ChannelMetricRow;
import com.aivle.sellon.rawdb.entity.RawReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

public interface RawReviewRepository extends JpaRepository<RawReview, String> {

    @Transactional(transactionManager = "rawDbTransactionManager", readOnly = true)
    @Query("""
            SELECT reviews.channelId AS channelId,
                   AVG(reviews.rating) AS avgRating
            FROM RawReview reviews
            WHERE reviews.createdAt >= :fromInclusive
              AND reviews.createdAt < :toExclusive
            GROUP BY reviews.channelId
            ORDER BY reviews.channelId
            """)
    List<ChannelMetricRow.ReviewRating> findAverageRatingsByChannel(
            @Param("fromInclusive") OffsetDateTime fromInclusive,
            @Param("toExclusive") OffsetDateTime toExclusive
    );
}
