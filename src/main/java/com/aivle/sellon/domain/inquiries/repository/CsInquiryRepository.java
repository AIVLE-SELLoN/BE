package com.aivle.sellon.domain.inquiries.repository;

import com.aivle.sellon.domain.inquiries.entity.CsInquiry;
import com.aivle.sellon.domain.inquiries.enums.InquiryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CsInquiryRepository extends JpaRepository<CsInquiry, Long> {
    List<CsInquiry> findByUser_Id(Long userId);
    List<CsInquiry> findByInquiryStatus(InquiryStatus status);
}
