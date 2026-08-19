package com.aivle.sellon.domain.alert.repository;

import com.aivle.sellon.domain.alert.dto.projection.RecommendedActionCount;
import com.aivle.sellon.domain.alert.entity.DetectionAlert;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DetectionAlertRepository extends JpaRepository<DetectionAlert, Long> {

    Optional<DetectionAlert> findByCompanyIdAndAlertCode(Long companyId, String alertCode);

    @Query("SELECT da.recommendedAction AS recommendedAction, COUNT(da) AS count " +
            "FROM DetectionAlert da " +
            "WHERE da.company.id = :companyId " +
            "GROUP BY da.recommendedAction")
    List<RecommendedActionCount> countByRecommendedAction(@Param("companyId") Long companyId);

    List<DetectionAlert> findByCompanyIdOrderByDetectedAtDesc(Long companyId, Pageable pageable);
}
