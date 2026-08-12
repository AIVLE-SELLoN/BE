package com.aivle.sellon.rawdb.repository;

import com.aivle.sellon.rawdb.entity.RawReview;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RawReviewRepository extends JpaRepository<RawReview, String> {
    List<RawReview> findByChannelId(String channelId);
}
