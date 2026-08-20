package com.aivle.sellon.domain.user.repository;

import com.aivle.sellon.domain.company.entity.Company;
import com.aivle.sellon.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long>, UserRepositoryCustom {

    // 아이디 찾기 - 회사명 + 사용자 이름으로 조회 (동명이인 있으면 가장 먼저 가입한 계정 기준)
    Optional<User> findFirstByNameAndCompanyAndDeletedAtIsNullOrderByIdAsc(String name, Company company);
}
