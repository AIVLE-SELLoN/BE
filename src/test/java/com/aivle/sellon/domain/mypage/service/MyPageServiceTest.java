package com.aivle.sellon.domain.mypage.service;

import com.aivle.sellon.domain.company.entity.Company;
import com.aivle.sellon.domain.company.repository.CompanyRepository;
import com.aivle.sellon.domain.company.util.CompanyKeyGenerator;
import com.aivle.sellon.domain.mypage.dto.request.MyPageUpdateRequest;
import com.aivle.sellon.domain.mypage.dto.request.ProfileImageCompleteRequest;
import com.aivle.sellon.domain.mypage.dto.request.ProfileImagePresignedUrlRequest;
import com.aivle.sellon.domain.mypage.dto.request.RecipientRequest;
import com.aivle.sellon.domain.mypage.dto.request.ReportSettingRequest;
import com.aivle.sellon.domain.mypage.dto.response.MyPageResponse;
import com.aivle.sellon.domain.mypage.dto.response.ProfileImagePresignedUrlResponse;
import com.aivle.sellon.domain.mypage.entity.MonthlyReportRecipient;
import com.aivle.sellon.domain.mypage.entity.MonthlyReportSetting;
import com.aivle.sellon.domain.mypage.enums.RecipientDepartment;
import com.aivle.sellon.domain.mypage.exception.CompanyKeyNotIssuedException;
import com.aivle.sellon.domain.mypage.exception.DuplicateRecipientEmailException;
import com.aivle.sellon.domain.mypage.exception.FieldNotEditableException;
import com.aivle.sellon.domain.mypage.exception.FileSizeExceededException;
import com.aivle.sellon.domain.mypage.exception.InvalidObjectKeyException;
import com.aivle.sellon.domain.mypage.exception.InvalidSendDayException;
import com.aivle.sellon.domain.mypage.exception.NotCompanyOwnerException;
import com.aivle.sellon.domain.mypage.exception.RecipientNotOwnedException;
import com.aivle.sellon.domain.mypage.exception.TooManyRecipientsException;
import com.aivle.sellon.domain.mypage.repository.MonthlyReportRecipientRepository;
import com.aivle.sellon.domain.mypage.repository.MonthlyReportSettingRepository;
import com.aivle.sellon.domain.mypage.util.CompanyKeyMasker;
import com.aivle.sellon.domain.user.entity.User;
import com.aivle.sellon.domain.user.enums.Role;
import com.aivle.sellon.domain.user.exception.DuplicateEmailException;
import com.aivle.sellon.domain.user.repository.UserRepository;
import com.aivle.sellon.domain.verification.exception.EmailNotVerifiedException;
import com.aivle.sellon.domain.verification.service.EmailVerificationService;
import com.aivle.sellon.global.file.exception.InvalidExtensionException;
import com.aivle.sellon.global.file.utils.GlobalFileValidator;
import com.aivle.sellon.global.security.principal.UserPrincipal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.net.URI;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
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

    @Mock
    private EmailVerificationService emailVerificationService;

    @Spy
    private GlobalFileValidator globalFileValidator = new GlobalFileValidator();

    @Mock
    private S3Client s3Client;

    @Mock
    private S3Presigner s3Presigner;

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
    @DisplayName("설정 행이 없으면 기본값(false / 1 / 10:00 / [])을 반환한다")
    void getMyPage_noSettingRow_returnsDefaultReportSetting() {
        UserPrincipal principal = UserPrincipal.ofClaims(1L, "member@example.com", Role.MEMBER, 10L);
        Company company = Company.create("마르디 메크르디");
        User user = User.createMember("member@example.com", "password", "김유진", company);
        when(userRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(user));

        MyPageResponse response = myPageService.getMyPage(principal);

        assertFalse(response.reportSetting().enabled());
        assertEquals(1, response.reportSetting().sendDay());
        assertEquals("10:00", response.reportSetting().sendTime());
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

    @Test
    @DisplayName("ROOT 가 email 을 현재와 동일한 값으로 보내면 예외 없이 정상 응답한다")
    void updateMyPage_root_sameEmail_noException() {
        UserPrincipal principal = UserPrincipal.ofClaims(1L, "root@example.com", Role.ROOT, 10L);
        Company company = Company.create("마르디 메크르디");
        User user = User.createRoot("root@example.com", "password", "김유진", company);
        when(userRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(user));
        when(monthlyReportSettingRepository.findByCompanyIdAndDeletedAtIsNull(any())).thenReturn(Optional.empty());

        MyPageUpdateRequest request = new MyPageUpdateRequest("root@example.com", null, null, null);

        MyPageResponse response = myPageService.updateMyPage(principal, request);

        assertEquals("root@example.com", response.email());
        verify(userRepository, never()).existsByEmailAndDeletedAtIsNull(any());
    }

    @Test
    @DisplayName("ROOT 가 email 을 다른 값으로 보내면 FieldNotEditableException 이 발생한다")
    void updateMyPage_root_differentEmail_throwsFieldNotEditableException() {
        UserPrincipal principal = UserPrincipal.ofClaims(1L, "root@example.com", Role.ROOT, 10L);
        Company company = Company.create("마르디 메크르디");
        User user = User.createRoot("root@example.com", "password", "김유진", company);
        when(userRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(user));

        MyPageUpdateRequest request = new MyPageUpdateRequest("new@example.com", null, null, null);

        assertThrows(FieldNotEditableException.class, () -> myPageService.updateMyPage(principal, request));
    }

    @Test
    @DisplayName("MEMBER 가 brandName 을 다른 값으로 보내면 FieldNotEditableException 이 발생한다")
    void updateMyPage_member_differentBrandName_throwsFieldNotEditableException() {
        UserPrincipal principal = UserPrincipal.ofClaims(1L, "member@example.com", Role.MEMBER, 10L);
        Company company = Company.create("마르디 메크르디");
        User user = User.createMember("member@example.com", "password", "김유진", company);
        when(userRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(user));

        MyPageUpdateRequest request = new MyPageUpdateRequest(null, null, "새브랜드명", null);

        assertThrows(FieldNotEditableException.class, () -> myPageService.updateMyPage(principal, request));
    }

    @Test
    @DisplayName("MEMBER 가 brandName 을 현재와 동일한 값으로 보내면 예외 없이 정상 응답한다")
    void updateMyPage_member_sameBrandName_noException() {
        UserPrincipal principal = UserPrincipal.ofClaims(1L, "member@example.com", Role.MEMBER, 10L);
        Company company = Company.create("마르디 메크르디");
        User user = User.createMember("member@example.com", "password", "김유진", company);
        when(userRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(user));
        when(monthlyReportSettingRepository.findByCompanyIdAndDeletedAtIsNull(any())).thenReturn(Optional.empty());

        MyPageUpdateRequest request = new MyPageUpdateRequest(null, null, "마르디 메크르디", null);

        MyPageResponse response = myPageService.updateMyPage(principal, request);

        assertEquals("마르디 메크르디", response.brandName());
    }

    @Test
    @DisplayName("sendDay 가 29 이면 InvalidSendDayException 이 발생한다")
    void updateMyPage_sendDay29_throwsInvalidSendDayException() {
        UserPrincipal principal = UserPrincipal.ofClaims(1L, "member@example.com", Role.MEMBER, 10L);
        Company company = Company.create("마르디 메크르디");
        User user = User.createMember("member@example.com", "password", "김유진", company);
        when(userRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(user));

        ReportSettingRequest reportSettingRequest = new ReportSettingRequest(true, 29, LocalTime.of(9, 0), List.of());
        MyPageUpdateRequest request = new MyPageUpdateRequest(null, null, null, reportSettingRequest);

        assertThrows(InvalidSendDayException.class, () -> myPageService.updateMyPage(principal, request));
    }

    @Test
    @DisplayName("수신자가 21 명이면 TooManyRecipientsException 이 발생한다")
    void updateMyPage_21Recipients_throwsTooManyRecipientsException() {
        UserPrincipal principal = UserPrincipal.ofClaims(1L, "member@example.com", Role.MEMBER, 10L);
        Company company = Company.create("마르디 메크르디");
        User user = User.createMember("member@example.com", "password", "김유진", company);
        when(userRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(user));

        List<RecipientRequest> recipients = IntStream.range(0, 21)
                .mapToObj(i -> new RecipientRequest(null, RecipientDepartment.CS, "user" + i + "@example.com"))
                .toList();
        ReportSettingRequest reportSettingRequest = new ReportSettingRequest(true, 1, LocalTime.of(9, 0), recipients);
        MyPageUpdateRequest request = new MyPageUpdateRequest(null, null, null, reportSettingRequest);

        assertThrows(TooManyRecipientsException.class, () -> myPageService.updateMyPage(principal, request));
    }

    @Test
    @DisplayName("수신자 이메일이 중복되면 DuplicateRecipientEmailException 이 발생한다")
    void updateMyPage_duplicateRecipientEmail_throwsDuplicateRecipientEmailException() {
        UserPrincipal principal = UserPrincipal.ofClaims(1L, "member@example.com", Role.MEMBER, 10L);
        Company company = Company.create("마르디 메크르디");
        User user = User.createMember("member@example.com", "password", "김유진", company);
        when(userRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(user));

        List<RecipientRequest> recipients = List.of(
                new RecipientRequest(null, RecipientDepartment.OPERATIONS, "dup@example.com"),
                new RecipientRequest(null, RecipientDepartment.CS, "dup@example.com")
        );
        ReportSettingRequest reportSettingRequest = new ReportSettingRequest(true, 1, LocalTime.of(9, 0), recipients);
        MyPageUpdateRequest request = new MyPageUpdateRequest(null, null, null, reportSettingRequest);

        assertThrows(DuplicateRecipientEmailException.class, () -> myPageService.updateMyPage(principal, request));
    }

    @Test
    @DisplayName("존재하지 않거나 타 회사 소유인 recipientId 가 전달되면 RecipientNotOwnedException 이 발생한다")
    void updateMyPage_recipientNotOwned_throwsRecipientNotOwnedException() {
        UserPrincipal principal = UserPrincipal.ofClaims(1L, "member@example.com", Role.MEMBER, 10L);
        Company company = Company.create("마르디 메크르디");
        User user = User.createMember("member@example.com", "password", "김유진", company);
        when(userRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(user));
        when(monthlyReportSettingRepository.findByCompanyIdAndDeletedAtIsNull(any())).thenReturn(Optional.empty());
        when(monthlyReportRecipientRepository.findAllByCompanyIdAndDeletedAtIsNullOrderByIdAsc(any()))
                .thenReturn(List.of());

        List<RecipientRequest> recipients = List.of(new RecipientRequest(999L, RecipientDepartment.OPERATIONS, "mkt@example.com"));
        ReportSettingRequest reportSettingRequest = new ReportSettingRequest(true, 1, LocalTime.of(9, 0), recipients);
        MyPageUpdateRequest request = new MyPageUpdateRequest(null, null, null, reportSettingRequest);

        assertThrows(RecipientNotOwnedException.class, () -> myPageService.updateMyPage(principal, request));
    }

    @Test
    @DisplayName("요청에서 빠진 기존 수신자는 hard delete 된다")
    void updateMyPage_missingRecipient_isHardDeleted() {
        UserPrincipal principal = UserPrincipal.ofClaims(1L, "member@example.com", Role.MEMBER, 10L);
        Company company = Company.create("마르디 메크르디");
        User user = User.createMember("member@example.com", "password", "김유진", company);
        when(userRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(user));

        MonthlyReportSetting setting = MonthlyReportSetting.create(company, true, 1, LocalTime.of(9, 0));
        when(monthlyReportSettingRepository.findByCompanyIdAndDeletedAtIsNull(any())).thenReturn(Optional.of(setting));

        MonthlyReportRecipient existingRecipient = MonthlyReportRecipient.create(company, RecipientDepartment.OPERATIONS, "mkt@example.com");
        ReflectionTestUtils.setField(existingRecipient, "id", 1L);
        when(monthlyReportRecipientRepository.findAllByCompanyIdAndDeletedAtIsNullOrderByIdAsc(any()))
                .thenReturn(List.of(existingRecipient));

        ReportSettingRequest reportSettingRequest = new ReportSettingRequest(true, 1, LocalTime.of(9, 0), List.of());
        MyPageUpdateRequest request = new MyPageUpdateRequest(null, null, null, reportSettingRequest);

        myPageService.updateMyPage(principal, request);

        verify(monthlyReportRecipientRepository).deleteAll(List.of(existingRecipient));
    }

    @Test
    @DisplayName("recipientId 가 null 인 항목은 신규 추가된다")
    void updateMyPage_nullRecipientId_createsNewRecipient() {
        UserPrincipal principal = UserPrincipal.ofClaims(1L, "member@example.com", Role.MEMBER, 10L);
        Company company = Company.create("마르디 메크르디");
        User user = User.createMember("member@example.com", "password", "김유진", company);
        when(userRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(user));

        MonthlyReportSetting setting = MonthlyReportSetting.create(company, true, 1, LocalTime.of(9, 0));
        when(monthlyReportSettingRepository.findByCompanyIdAndDeletedAtIsNull(any())).thenReturn(Optional.of(setting));
        when(monthlyReportRecipientRepository.findAllByCompanyIdAndDeletedAtIsNullOrderByIdAsc(any()))
                .thenReturn(List.of());
        when(monthlyReportRecipientRepository.save(any(MonthlyReportRecipient.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        List<RecipientRequest> recipients = List.of(new RecipientRequest(null, RecipientDepartment.CS, "cs@example.com"));
        ReportSettingRequest reportSettingRequest = new ReportSettingRequest(true, 1, LocalTime.of(9, 0), recipients);
        MyPageUpdateRequest request = new MyPageUpdateRequest(null, null, null, reportSettingRequest);

        MyPageResponse response = myPageService.updateMyPage(principal, request);

        verify(monthlyReportRecipientRepository).save(any(MonthlyReportRecipient.class));
        assertEquals(1, response.reportSetting().recipients().size());
        assertEquals("cs@example.com", response.reportSetting().recipients().get(0).email());
    }

    @Test
    @DisplayName("설정 행이 없던 회사에 PATCH 하면 새 설정 행이 생성된다")
    void updateMyPage_noExistingSetting_createsNewSetting() {
        UserPrincipal principal = UserPrincipal.ofClaims(1L, "member@example.com", Role.MEMBER, 10L);
        Company company = Company.create("마르디 메크르디");
        User user = User.createMember("member@example.com", "password", "김유진", company);
        when(userRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(user));
        when(monthlyReportSettingRepository.findByCompanyIdAndDeletedAtIsNull(any())).thenReturn(Optional.empty());
        when(monthlyReportSettingRepository.save(any(MonthlyReportSetting.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(monthlyReportRecipientRepository.findAllByCompanyIdAndDeletedAtIsNullOrderByIdAsc(any()))
                .thenReturn(List.of());

        ReportSettingRequest reportSettingRequest = new ReportSettingRequest(true, 5, LocalTime.of(10, 30), List.of());
        MyPageUpdateRequest request = new MyPageUpdateRequest(null, null, null, reportSettingRequest);

        MyPageResponse response = myPageService.updateMyPage(principal, request);

        verify(monthlyReportSettingRepository).save(any(MonthlyReportSetting.class));
        assertTrue(response.reportSetting().enabled());
        assertEquals(5, response.reportSetting().sendDay());
        assertEquals("10:30", response.reportSetting().sendTime());
    }

    @Test
    @DisplayName("이메일 변경 시 validateVerificationToken 이 호출된다")
    void updateMyPage_emailChange_callsValidateVerificationToken() {
        UserPrincipal principal = UserPrincipal.ofClaims(1L, "member@example.com", Role.MEMBER, 10L);
        Company company = Company.create("마르디 메크르디");
        User user = User.createMember("member@example.com", "password", "김유진", company);
        when(userRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmailAndDeletedAtIsNull("new@example.com")).thenReturn(false);
        when(monthlyReportSettingRepository.findByCompanyIdAndDeletedAtIsNull(any())).thenReturn(Optional.empty());

        MyPageUpdateRequest request = new MyPageUpdateRequest("new@example.com", "token-abc", null, null);

        myPageService.updateMyPage(principal, request);

        verify(emailVerificationService).validateVerificationToken("new@example.com", "token-abc");
    }

    @Test
    @DisplayName("verificationToken 이 유효하지 않으면 이메일이 저장되지 않는다")
    void updateMyPage_invalidVerificationToken_emailNotSaved() {
        UserPrincipal principal = UserPrincipal.ofClaims(1L, "member@example.com", Role.MEMBER, 10L);
        Company company = Company.create("마르디 메크르디");
        User user = User.createMember("member@example.com", "password", "김유진", company);
        when(userRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmailAndDeletedAtIsNull("new@example.com")).thenReturn(false);
        doThrow(new EmailNotVerifiedException())
                .when(emailVerificationService).validateVerificationToken("new@example.com", null);

        MyPageUpdateRequest request = new MyPageUpdateRequest("new@example.com", null, null, null);

        assertThrows(EmailNotVerifiedException.class, () -> myPageService.updateMyPage(principal, request));
        assertEquals("member@example.com", user.getEmail());
    }

    @Test
    @DisplayName("PATCH 응답은 실제로 저장된 값과 일치하며 GET 과 동일한 스키마다")
    void updateMyPage_responseMatchesSavedState() {
        UserPrincipal principal = UserPrincipal.ofClaims(1L, "member@example.com", Role.MEMBER, 10L);
        Company company = Company.create("마르디 메크르디");
        User user = User.createMember("member@example.com", "password", "김유진", company);
        when(userRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmailAndDeletedAtIsNull("new@example.com")).thenReturn(false);

        MonthlyReportSetting setting = MonthlyReportSetting.create(company, false, 1, LocalTime.of(9, 0));
        when(monthlyReportSettingRepository.findByCompanyIdAndDeletedAtIsNull(any())).thenReturn(Optional.of(setting));

        MonthlyReportRecipient existingRecipient = MonthlyReportRecipient.create(company, RecipientDepartment.OPERATIONS, "old@example.com");
        ReflectionTestUtils.setField(existingRecipient, "id", 1L);
        when(monthlyReportRecipientRepository.findAllByCompanyIdAndDeletedAtIsNullOrderByIdAsc(any()))
                .thenReturn(List.of(existingRecipient));
        when(monthlyReportRecipientRepository.save(any(MonthlyReportRecipient.class)))
                .thenAnswer(invocation -> {
                    MonthlyReportRecipient saved = invocation.getArgument(0);
                    ReflectionTestUtils.setField(saved, "id", 2L);
                    return saved;
                });

        List<RecipientRequest> recipients = List.of(
                new RecipientRequest(1L, RecipientDepartment.OPERATIONS, "mkt@example.com"),
                new RecipientRequest(null, RecipientDepartment.CS, "cs@example.com")
        );
        ReportSettingRequest reportSettingRequest = new ReportSettingRequest(true, 15, LocalTime.of(14, 30), recipients);
        MyPageUpdateRequest request = new MyPageUpdateRequest("new@example.com", "token-abc", null, reportSettingRequest);

        MyPageResponse response = myPageService.updateMyPage(principal, request);

        assertEquals("new@example.com", response.email());
        assertEquals("new@example.com", user.getEmail());
        assertTrue(response.reportSetting().enabled());
        assertEquals(15, response.reportSetting().sendDay());
        assertEquals("14:30", response.reportSetting().sendTime());
        assertEquals(2, response.reportSetting().recipients().size());
        assertEquals("mkt@example.com", response.reportSetting().recipients().get(0).email());
        assertEquals("cs@example.com", response.reportSetting().recipients().get(1).email());
    }

    @Test
    @DisplayName("확장자가 .pdf 인 파일명으로 presigned 요청을 하면 InvalidExtensionException 이 발생한다")
    void issueProfileImagePresignedUrl_pdfExtension_throwsInvalidExtensionException() {
        UserPrincipal principal = UserPrincipal.ofClaims(1L, "member@example.com", Role.MEMBER, 10L);
        Company company = Company.create("마르디 메크르디");
        User user = User.createMember("member@example.com", "password", "김유진", company);
        when(userRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(user));

        ProfileImagePresignedUrlRequest request = new ProfileImagePresignedUrlRequest("photo.pdf", 1_000_000L);

        assertThrows(InvalidExtensionException.class,
                () -> myPageService.issueProfileImagePresignedUrl(principal, request));
    }

    @Test
    @DisplayName("확장자가 없는 파일명으로 presigned 요청을 하면 InvalidExtensionException 이 발생한다")
    void issueProfileImagePresignedUrl_noExtension_throwsInvalidExtensionException() {
        UserPrincipal principal = UserPrincipal.ofClaims(1L, "member@example.com", Role.MEMBER, 10L);
        Company company = Company.create("마르디 메크르디");
        User user = User.createMember("member@example.com", "password", "김유진", company);
        when(userRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(user));

        ProfileImagePresignedUrlRequest request = new ProfileImagePresignedUrlRequest("photo", 1_000_000L);

        assertThrows(InvalidExtensionException.class,
                () -> myPageService.issueProfileImagePresignedUrl(principal, request));
    }

    @Test
    @DisplayName("fileSize 가 5MB 를 초과하면 FileSizeExceededException 이 발생한다")
    void issueProfileImagePresignedUrl_fileSizeExceeded_throwsFileSizeExceededException() {
        UserPrincipal principal = UserPrincipal.ofClaims(1L, "member@example.com", Role.MEMBER, 10L);
        Company company = Company.create("마르디 메크르디");
        User user = User.createMember("member@example.com", "password", "김유진", company);
        when(userRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(user));

        ProfileImagePresignedUrlRequest request = new ProfileImagePresignedUrlRequest("photo.jpg", 6 * 1024 * 1024L);

        assertThrows(FileSizeExceededException.class,
                () -> myPageService.issueProfileImagePresignedUrl(principal, request));
    }

    @Test
    @DisplayName("정상 요청이면 객체 키가 profiles/user-images/{userId}/original/ 로 시작한다")
    void issueProfileImagePresignedUrl_valid_objectKeyStartsWithExpectedPrefix() throws Exception {
        UserPrincipal principal = UserPrincipal.ofClaims(1L, "member@example.com", Role.MEMBER, 10L);
        Company company = Company.create("마르디 메크르디");
        User user = User.createMember("member@example.com", "password", "김유진", company);
        ReflectionTestUtils.setField(user, "id", 1L);
        when(userRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(user));

        PresignedPutObjectRequest presignedPutObjectRequest = mock(PresignedPutObjectRequest.class);
        when(presignedPutObjectRequest.url()).thenReturn(URI.create("https://bucket.s3.amazonaws.com/signed").toURL());
        when(s3Presigner.presignPutObject(any(PutObjectPresignRequest.class))).thenReturn(presignedPutObjectRequest);

        ProfileImagePresignedUrlRequest request = new ProfileImagePresignedUrlRequest("photo.jpg", 1_000_000L);

        ProfileImagePresignedUrlResponse response = myPageService.issueProfileImagePresignedUrl(principal, request);

        assertTrue(response.objectKey().startsWith("profiles/user-images/1/original/"));
    }

    @Test
    @DisplayName("타인 경로의 objectKey 로 완료 통보하면 InvalidObjectKeyException 이 발생한다")
    void completeProfileImageUpload_foreignObjectKey_throwsInvalidObjectKeyException() {
        UserPrincipal principal = UserPrincipal.ofClaims(1L, "member@example.com", Role.MEMBER, 10L);
        Company company = Company.create("마르디 메크르디");
        User user = User.createMember("member@example.com", "password", "김유진", company);
        ReflectionTestUtils.setField(user, "id", 1L);
        when(userRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(user));

        ProfileImageCompleteRequest request =
                new ProfileImageCompleteRequest("profiles/user-images/999/original/1721897234-uuid.jpg");

        assertThrows(InvalidObjectKeyException.class,
                () -> myPageService.completeProfileImageUpload(principal, request));
    }

    @Test
    @DisplayName("경로 순회가 포함된 objectKey 로 완료 통보하면 InvalidObjectKeyException 이 발생한다")
    void completeProfileImageUpload_pathTraversalObjectKey_throwsInvalidObjectKeyException() {
        UserPrincipal principal = UserPrincipal.ofClaims(1L, "member@example.com", Role.MEMBER, 10L);
        Company company = Company.create("마르디 메크르디");
        User user = User.createMember("member@example.com", "password", "김유진", company);
        ReflectionTestUtils.setField(user, "id", 1L);
        when(userRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(user));

        // 접두사 검사만으로는 통과하지만 파일명 형식 검사에서 걸려야 한다
        ProfileImageCompleteRequest request = new ProfileImageCompleteRequest(
                "profiles/user-images/1/original/../../3/original/1721890000-11111111-2222-3333-4444-555555555555.jpg");

        assertThrows(InvalidObjectKeyException.class,
                () -> myPageService.completeProfileImageUpload(principal, request));
        verifyNoInteractions(s3Client);
    }

    @Test
    @DisplayName("HeadObject 결과가 5MB 를 초과하면 FileSizeExceededException 이 발생하고 S3 객체를 삭제한다")
    void completeProfileImageUpload_headObjectExceedsSize_deletesObjectAndThrows() {
        UserPrincipal principal = UserPrincipal.ofClaims(1L, "member@example.com", Role.MEMBER, 10L);
        Company company = Company.create("마르디 메크르디");
        User user = User.createMember("member@example.com", "password", "김유진", company);
        ReflectionTestUtils.setField(user, "id", 1L);
        when(userRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(user));

        String objectKey = "profiles/user-images/1/original/1721897234-a3f4c9e2-b7d1-4f2a-9e6c-2a5f8b3d1c7a.jpg";
        HeadObjectResponse headObjectResponse = HeadObjectResponse.builder()
                .contentLength(6 * 1024 * 1024L)
                .build();
        when(s3Client.headObject(any(HeadObjectRequest.class))).thenReturn(headObjectResponse);

        ProfileImageCompleteRequest request = new ProfileImageCompleteRequest(objectKey);

        assertThrows(FileSizeExceededException.class,
                () -> myPageService.completeProfileImageUpload(principal, request));
        verify(s3Client).deleteObject(any(DeleteObjectRequest.class));
    }

    @Test
    @DisplayName("이전 이미지가 있는 상태에서 교체하면 이전 키로 deleteObject 를 호출한다")
    void completeProfileImageUpload_replacesExistingImage_deletesPreviousObject() throws Exception {
        UserPrincipal principal = UserPrincipal.ofClaims(1L, "member@example.com", Role.MEMBER, 10L);
        Company company = Company.create("마르디 메크르디");
        User user = User.createMember("member@example.com", "password", "김유진", company);
        ReflectionTestUtils.setField(user, "id", 1L);
        String previousObjectKey = "profiles/user-images/1/original/1721890000-11111111-2222-3333-4444-555555555555.jpg";
        user.changeProfileImage(previousObjectKey);
        when(userRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(user));

        String newObjectKey = "profiles/user-images/1/original/1721897234-66666666-7777-8888-9999-aaaaaaaaaaaa.jpg";
        HeadObjectResponse headObjectResponse = HeadObjectResponse.builder()
                .contentLength(1_000_000L)
                .build();
        when(s3Client.headObject(any(HeadObjectRequest.class))).thenReturn(headObjectResponse);

        PresignedGetObjectRequest presignedGetObjectRequest = mock(PresignedGetObjectRequest.class);
        when(presignedGetObjectRequest.url()).thenReturn(URI.create("https://bucket.s3.amazonaws.com/get").toURL());
        when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class))).thenReturn(presignedGetObjectRequest);

        ProfileImageCompleteRequest request = new ProfileImageCompleteRequest(newObjectKey);

        myPageService.completeProfileImageUpload(principal, request);

        ArgumentCaptor<DeleteObjectRequest> captor = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3Client).deleteObject(captor.capture());
        assertEquals(previousObjectKey, captor.getValue().key());
    }

    @Test
    @DisplayName("profileImageKey 가 null 이면 profileImageUrl 도 null 이고 presigner 를 호출하지 않는다")
    void getMyPage_noProfileImage_profileImageUrlNullAndPresignerNotCalled() {
        UserPrincipal principal = UserPrincipal.ofClaims(1L, "member@example.com", Role.MEMBER, 10L);
        Company company = Company.create("마르디 메크르디");
        User user = User.createMember("member@example.com", "password", "김유진", company);
        when(userRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(user));

        MyPageResponse response = myPageService.getMyPage(principal);

        assertNull(response.profileImageUrl());
        verifyNoInteractions(s3Presigner);
    }

    @Test
    @DisplayName("이미 프로필 이미지가 없는 상태에서 삭제를 요청하면 예외 없이 정상 응답한다")
    void removeProfileImage_alreadyNull_returnsNormallyWithoutException() {
        UserPrincipal principal = UserPrincipal.ofClaims(1L, "member@example.com", Role.MEMBER, 10L);
        Company company = Company.create("마르디 메크르디");
        User user = User.createMember("member@example.com", "password", "김유진", company);
        when(userRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(user));

        MyPageResponse response = myPageService.removeProfileImage(principal);

        assertNull(response.profileImageUrl());
        verifyNoInteractions(s3Client);
    }
}
