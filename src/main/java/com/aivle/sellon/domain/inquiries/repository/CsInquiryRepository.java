package com.aivle.sellon.domain.inquiries.repository;

import com.aivle.sellon.domain.inquiries.entity.CsInquiry;
import com.aivle.sellon.domain.inquiries.enums.InquiryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CsInquiryRepository extends JpaRepository<CsInquiry, Long> {
    List<CsInquiry> findByUser_IdAndDeletedAtIsNull(Long userId);
    List<CsInquiry> findByInquiryStatusAndDeletedAtIsNull(InquiryStatus status);
    List<CsInquiry> findByDeletedAtIsNull();
}
