package com.aivle.sellon.domain.mypage.service;

import com.aivle.sellon.domain.company.entity.Company;
import com.aivle.sellon.domain.company.repository.CompanyRepository;
import com.aivle.sellon.domain.company.util.CompanyKeyGenerator;
import com.aivle.sellon.domain.mypage.dto.response.MyPageResponse;
import com.aivle.sellon.domain.mypage.exception.CompanyKeyNotIssuedException;
import com.aivle.sellon.domain.mypage.exception.NotCompanyOwnerException;
import com.aivle.sellon.domain.mypage.repository.MonthlyReportRecipientRepository;
import com.aivle.sellon.domain.mypage.repository.MonthlyReportSettingRepository;
import com.aivle.sellon.domain.mypage.util.CompanyKeyMasker;
import com.aivle.sellon.domain.user.entity.User;
import com.aivle.sellon.domain.user.enums.Role;
import com.aivle.sellon.domain.user.repository.UserRepository;
import com.aivle.sellon.global.security.principal.UserPrincipal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MyPageServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private CompanyKeyGenerator companyKeyGenerator;

    @Mock
    private MonthlyReportSettingRepository monthlyReportSettingRepository;

    @Mock
    private MonthlyReportRecipientRepository monthlyReportRecipientRepository;

    @Spy
    private CompanyKeyMasker companyKeyMasker = new CompanyKeyMasker();

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

    @Test
    @DisplayName("MEMBER 가 마이페이지를 조회하면 companyKey 와 companyKeyIssued 가 각각 null, false 다")
    void getMyPage_member_companyKeyIsNull() {
        UserPrincipal principal = UserPrincipal.ofClaims(1L, "member@example.com", Role.MEMBER, 10L);
        Company company = Company.create("마르디 메크르디");
        User user = User.createMember("member@example.com", "password", "김유진", company);
        when(userRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(user));

        MyPageResponse response = myPageService.getMyPage(principal);

        assertNull(response.companyKey());
        assertFalse(response.companyKeyIssued());
    }

    @Test
    @DisplayName("ROOT · 키 발급 상태에서 마이페이지를 조회하면 마스킹된 문자열을 반환한다")
    void getMyPage_root_issued_returnsMaskedKey() {
        UserPrincipal principal = UserPrincipal.ofClaims(1L, "root@example.com", Role.ROOT, 10L);
        Company company = Company.create("마르디 메크르디");
        company.issueJoinKey("SLN-A1B2C3D4E5F6AB12");
        User user = User.createRoot("root@example.com", "password", "김유진", company);
        when(userRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(user));

        MyPageResponse response = myPageService.getMyPage(principal);

        assertEquals("SLN-••••••••••••AB12", response.companyKey());
        assertTrue(response.companyKeyIssued());
    }

    @Test
    @DisplayName("ROOT · joinKey 가 null 이면 예외 없이 companyKeyIssued 가 false 다")
    void getMyPage_root_joinKeyNull_noExceptionAndNotIssued() {
        UserPrincipal principal = UserPrincipal.ofClaims(1L, "root@example.com", Role.ROOT, 10L);
        Company company = Company.create("마르디 메크르디");
        User user = User.createRoot("root@example.com", "password", "김유진", company);
        when(userRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(user));

        MyPageResponse response = myPageService.getMyPage(principal);

        assertNull(response.companyKey());
        assertFalse(response.companyKeyIssued());
    }

    @Test
    @DisplayName("설정 행이 없으면 기본값(false / 1 / 09:00 / [])을 반환한다")
    void getMyPage_noSettingRow_returnsDefaultReportSetting() {
        UserPrincipal principal = UserPrincipal.ofClaims(1L, "member@example.com", Role.MEMBER, 10L);
        Company company = Company.create("마르디 메크르디");
        User user = User.createMember("member@example.com", "password", "김유진", company);
        when(userRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(user));

        MyPageResponse response = myPageService.getMyPage(principal);

        assertFalse(response.reportSetting().enabled());
        assertEquals(1, response.reportSetting().sendDay());
        assertEquals("09:00", response.reportSetting().sendTime());
        assertTrue(response.reportSetting().recipients().isEmpty());
    }

    @Test
    @DisplayName("설정 행이 없으면 save 를 호출하지 않고 수신자 레포지토리와도 상호작용하지 않는다")
    void getMyPage_noSettingRow_doesNotSaveOrQueryRecipients() {
        UserPrincipal principal = UserPrincipal.ofClaims(1L, "member@example.com", Role.MEMBER, 10L);
        Company company = Company.create("마르디 메크르디");
        User user = User.createMember("member@example.com", "password", "김유진", company);
        when(userRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(user));

        myPageService.getMyPage(principal);

        verify(monthlyReportSettingRepository, never()).save(any());
        verifyNoInteractions(monthlyReportRecipientRepository);
    }

    @Test
    @DisplayName("editable 은 ROOT 와 MEMBER 에서 서로 반대값이다")
    void getMyPage_editable_differsByRole() {
        Company company = Company.create("마르디 메크르디");

        UserPrincipal rootPrincipal = UserPrincipal.ofClaims(1L, "root@example.com", Role.ROOT, 10L);
        User rootUser = User.createRoot("root@example.com", "password", "김유진", company);
        when(userRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(rootUser));
        MyPageResponse rootResponse = myPageService.getMyPage(rootPrincipal);
        assertFalse(rootResponse.editable().email());
        assertTrue(rootResponse.editable().brandName());

        UserPrincipal memberPrincipal = UserPrincipal.ofClaims(2L, "member@example.com", Role.MEMBER, 10L);
        User memberUser = User.createMember("member@example.com", "password", "김민수", company);
        when(userRepository.findByIdAndDeletedAtIsNull(2L)).thenReturn(Optional.of(memberUser));
        MyPageResponse memberResponse = myPageService.getMyPage(memberPrincipal);
        assertTrue(memberResponse.editable().email());
        assertFalse(memberResponse.editable().brandName());
    }
}
