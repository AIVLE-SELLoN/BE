package com.aivle.sellon.domain.alert.repository;

import com.aivle.sellon.domain.alert.entity.DetectionAlert;
import com.aivle.sellon.domain.alert.enums.AlertChannel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DetectionAlertRepository extends JpaRepository<DetectionAlert, Long> {

    Optional<DetectionAlert> findByCompanyIdAndAlertCode(Long companyId, String alertCode);

    // 채널 비교 - aspect(문의 유형) 분포 대체 지표: 문의 건별 원본 라벨(classified_item_aspect)이
    // develop에서 사라져, 이상탐지로 플래그된 알림(mainAspect 단위) 기준으로 근사 집계한다.
    List<DetectionAlert> findByCompany_IdAndChannel(Long companyId, AlertChannel channel);
}
