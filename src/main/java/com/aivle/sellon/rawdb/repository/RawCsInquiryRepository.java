package com.aivle.sellon.rawdb.repository;

import com.aivle.sellon.rawdb.entity.RawCsInquiry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RawCsInquiryRepository extends JpaRepository<RawCsInquiry, String> {
    List<RawCsInquiry> findByChannelId(String channelId);
}
