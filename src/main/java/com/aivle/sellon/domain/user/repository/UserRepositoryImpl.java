package com.aivle.sellon.domain.user.repository;

import com.aivle.sellon.domain.user.entity.QUser;
import com.aivle.sellon.domain.user.entity.User;
import com.aivle.sellon.domain.user.enums.Role;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepositoryCustom {

    private static final QUser user = QUser.user;

    private final JPAQueryFactory queryFactory;

    @Override
    public Optional<User> findByEmailAndDeletedAtIsNull(String email) {
        return Optional.ofNullable(
                queryFactory
                        .selectFrom(user)
                        .where(user.email.eq(email), user.deletedAt.isNull())
                        .fetchOne()
        );
    }

    @Override
    public boolean existsByEmailAndDeletedAtIsNull(String email) {
        Integer result = queryFactory
                .selectOne()
                .from(user)
                .where(user.email.eq(email), user.deletedAt.isNull())
                .fetchFirst();
        return result != null;
    }

    @Override
    public Optional<User> findByIdAndDeletedAtIsNull(Long id) {
        return Optional.ofNullable(
                queryFactory
                        .selectFrom(user)
                        .where(user.id.eq(id), user.deletedAt.isNull())
                        .fetchOne()
        );
    }

    @Override
    public Optional<User> findRootByCompanyIdAndDeletedAtIsNull(Long companyId) {
        return Optional.ofNullable(
                queryFactory
                        .selectFrom(user)
                        .where(
                                user.company.id.eq(companyId),
                                user.role.eq(Role.ROOT),
                                user.deletedAt.isNull()
                        )
                        .fetchOne()
        );
    }
}
