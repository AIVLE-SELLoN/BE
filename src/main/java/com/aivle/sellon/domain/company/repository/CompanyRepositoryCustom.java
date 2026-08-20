package com.aivle.sellon.domain.company.repository;

import com.aivle.sellon.domain.company.entity.Company;

import java.util.Optional;

public interface CompanyRepositoryCustom {
    boolean existsByJoinKey(String joinKey);

    Optional<Company> findByJoinKey(String joinKey);
}
