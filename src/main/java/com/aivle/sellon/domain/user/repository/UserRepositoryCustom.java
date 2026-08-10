package com.aivle.sellon.domain.user.repository;

import com.aivle.sellon.domain.user.entity.User;

import java.util.Optional;

public interface UserRepositoryCustom {
    Optional<User> findByEmailAndDeletedAtIsNull(String email);

    boolean existsByEmailAndDeletedAtIsNull(String email);

    Optional<User> findByIdAndDeletedAtIsNull(Long id);

    Optional<User> findRootByCompanyIdAndDeletedAtIsNull(Long companyId);
}
