package com.aivle.sellon.domain.mypage.repository;

import com.aivle.sellon.domain.mypage.entity.MonthlyReportRecipient;
import com.aivle.sellon.domain.mypage.enums.RecipientDepartment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MonthlyReportRecipientRepository extends JpaRepository<MonthlyReportRecipient, Long> {
    List<MonthlyReportRecipient> findAllByCompanyIdAndDeletedAtIsNullOrderByIdAsc(Long companyId);

    List<MonthlyReportRecipient> findAllByCompanyIdAndDepartmentAndDeletedAtIsNullOrderByIdAsc(
            Long companyId, RecipientDepartment department);
}
