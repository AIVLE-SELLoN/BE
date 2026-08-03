package com.aivle.sellon.domain.mypage.service;

import com.aivle.sellon.domain.company.entity.Company;
import com.aivle.sellon.domain.company.repository.CompanyRepository;
import com.aivle.sellon.domain.company.util.CompanyKeyGenerator;
import com.aivle.sellon.domain.mypage.exception.CompanyKeyNotIssuedException;
import com.aivle.sellon.domain.mypage.exception.NotCompanyOwnerException;
import com.aivle.sellon.domain.user.enums.Role;
import com.aivle.sellon.global.security.principal.UserPrincipal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MyPageServiceTest {

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private CompanyKeyGenerator companyKeyGenerator;

    @InjectMocks
    private MyPageService myPageService;

    @Test
    @DisplayName("MEMBER 가 식별자 키를 조회하면 NotCompanyOwnerException 이 발생한다")
    void getCompanyKey_member_throwsNotCompanyOwnerException() {
        UserPrincipal principal = UserPrincipal.ofClaims(1L, "member@example.com", Role.MEMBER, 10L);

        assertThrows(NotCompanyOwnerException.class, () -> myPageService.getCompanyKey(principal));
    }

    @Test
    @DisplayName("ROOT 가 식별자 키를 조회하면 마스킹 없이 원본을 반환한다")
    void getCompanyKey_root_returnsRawKey() {
        UserPrincipal principal = UserPrincipal.ofClaims(1L, "root@example.com", Role.ROOT, 10L);
        Company company = Company.create("마르디 메크르디");
        company.issueJoinKey("SLN-A1B2C3D4E5F6AB12");
        when(companyRepository.findById(10L)).thenReturn(Optional.of(company));

        String result = myPageService.getCompanyKey(principal);

        assertEquals("SLN-A1B2C3D4E5F6AB12", result);
    }

    @Test
    @DisplayName("식별자 키 조회는 companyKeyGenerator 를 호출하지 않는다")
    void getCompanyKey_doesNotInteractWithGenerator() {
        UserPrincipal principal = UserPrincipal.ofClaims(1L, "root@example.com", Role.ROOT, 10L);
        Company company = Company.create("마르디 메크르디");
        company.issueJoinKey("SLN-A1B2C3D4E5F6AB12");
        when(companyRepository.findById(10L)).thenReturn(Optional.of(company));

        myPageService.getCompanyKey(principal);

        verifyNoInteractions(companyKeyGenerator);
    }

    @Test
    @DisplayName("joinKey 가 발급되지 않은 상태면 CompanyKeyNotIssuedException 이 발생한다")
    void getCompanyKey_joinKeyNull_throwsCompanyKeyNotIssuedException() {
        UserPrincipal principal = UserPrincipal.ofClaims(1L, "root@example.com", Role.ROOT, 10L);
        Company company = Company.create("마르디 메크르디");
        when(companyRepository.findById(10L)).thenReturn(Optional.of(company));

        assertThrows(CompanyKeyNotIssuedException.class, () -> myPageService.getCompanyKey(principal));
    }
}
