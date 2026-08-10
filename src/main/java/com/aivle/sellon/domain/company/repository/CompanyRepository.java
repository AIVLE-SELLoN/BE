package com.aivle.sellon.domain.company.repository;

import com.aivle.sellon.domain.company.entity.Company;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CompanyRepository extends JpaRepository<Company, Long>, CompanyRepositoryCustom {
    Optional<Company> findByName(String name);
}
