package com.aivle.sellon.domain.mypage.repository;

import com.aivle.sellon.domain.mypage.entity.MonthlyReportSetting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MonthlyReportSettingRepository extends JpaRepository<MonthlyReportSetting, Long> {
    Optional<MonthlyReportSetting> findByCompanyIdAndDeletedAtIsNull(Long companyId);
}
