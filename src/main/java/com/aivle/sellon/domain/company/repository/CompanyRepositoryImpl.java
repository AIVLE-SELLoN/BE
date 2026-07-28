package com.aivle.sellon.domain.company.repository;

import com.aivle.sellon.domain.company.entity.Company;
import com.aivle.sellon.domain.company.entity.QCompany;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

@RequiredArgsConstructor
public class CompanyRepositoryImpl implements CompanyRepositoryCustom {

    private static final QCompany company = QCompany.company;

    private final JPAQueryFactory queryFactory;

    @Override
    public boolean existsByJoinKey(String joinKey) {
        Integer result = queryFactory
                .selectOne()
                .from(company)
                .where(company.joinKey.eq(joinKey))
                .fetchFirst();
        return result != null;
    }

    @Override
    public Optional<Company> findByJoinKey(String joinKey) {
        return Optional.ofNullable(
                queryFactory
                        .selectFrom(company)
                        .where(company.joinKey.eq(joinKey))
                        .fetchOne()
        );
    }
}
