package com.aivle.sellon.domain.notification.repository;

import com.aivle.sellon.domain.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long>, NotificationRepositoryCustom {
    Optional<Notification> findByIdAndCompanyId(Long id, Long companyId);
}
