package com.aivle.sellon.domain.mypage.repository;

import com.aivle.sellon.domain.mypage.entity.MonthlyReportRecipient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MonthlyReportRecipientRepository extends JpaRepository<MonthlyReportRecipient, Long> {
    List<MonthlyReportRecipient> findAllByCompanyId(Long companyId);
}
