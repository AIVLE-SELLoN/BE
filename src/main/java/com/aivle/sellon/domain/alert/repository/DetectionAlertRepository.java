package com.aivle.sellon.domain.alert.repository;

import com.aivle.sellon.domain.alert.entity.DetectionAlert;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DetectionAlertRepository extends JpaRepository<DetectionAlert, Long> {

    Optional<DetectionAlert> findByAlertCode(String alertCode);
}
