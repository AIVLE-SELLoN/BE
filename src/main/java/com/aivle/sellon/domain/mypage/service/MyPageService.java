package com.aivle.sellon.domain.mypage.service;

import com.aivle.sellon.domain.company.entity.Company;
import com.aivle.sellon.domain.company.exception.CompanyNotFoundException;
import com.aivle.sellon.domain.company.repository.CompanyRepository;
import com.aivle.sellon.domain.company.util.CompanyKeyGenerator;
import com.aivle.sellon.domain.mypage.dto.request.MyPageUpdateRequest;
import com.aivle.sellon.domain.mypage.dto.request.RecipientRequest;
import com.aivle.sellon.domain.mypage.dto.request.ReportSettingRequest;
import com.aivle.sellon.domain.mypage.dto.response.MyPageResponse;
import com.aivle.sellon.domain.mypage.dto.response.RecipientResponse;
import com.aivle.sellon.domain.mypage.dto.response.ReportSettingResponse;
import com.aivle.sellon.domain.mypage.entity.MonthlyReportRecipient;
import com.aivle.sellon.domain.mypage.entity.MonthlyReportSetting;
import com.aivle.sellon.domain.mypage.exception.CompanyKeyNotIssuedException;
import com.aivle.sellon.domain.mypage.exception.DuplicateRecipientEmailException;
import com.aivle.sellon.domain.mypage.exception.FieldNotEditableException;
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
import com.aivle.sellon.domain.user.exception.UserNotFoundException;
import com.aivle.sellon.domain.user.repository.UserRepository;
import com.aivle.sellon.domain.verification.service.EmailVerificationService;
import com.aivle.sellon.global.security.principal.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MyPageService {

    private static final int MAX_RECIPIENTS = 20;
    private static final int MIN_SEND_DAY = 1;
    private static final int MAX_SEND_DAY = 28;

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final CompanyKeyGenerator companyKeyGenerator;
    private final MonthlyReportSettingRepository monthlyReportSettingRepository;
    private final MonthlyReportRecipientRepository monthlyReportRecipientRepository;
    private final CompanyKeyMasker companyKeyMasker;
    private final EmailVerificationService emailVerificationService;

    @Transactional
    public String issueCompanyKey(UserPrincipal principal) {
        if (principal.getRole() != Role.ROOT)
            throw new NotCompanyOwnerException();

        Company company = companyRepository.findById(principal.getCompanyId())
                .orElseThrow(CompanyNotFoundException::new);

        String newKey = companyKeyGenerator.generate();
        company.issueJoinKey(newKey);

        return newKey;
    }

    public String getCompanyKey(UserPrincipal principal) {
        if (principal.getRole() != Role.ROOT)
            throw new NotCompanyOwnerException();

        Company company = companyRepository.findById(principal.getCompanyId())
                .orElseThrow(CompanyNotFoundException::new);

        String joinKey = company.getJoinKey();
        if (joinKey == null)
            throw new CompanyKeyNotIssuedException();

        return joinKey;
    }

    public MyPageResponse getMyPage(UserPrincipal principal) {
        User user = userRepository.findByIdAndDeletedAtIsNull(principal.getId())
                .orElseThrow(UserNotFoundException::new);
        Company company = user.getCompany();

        ReportSettingResponse reportSetting = findReportSetting(company.getId());

        return buildResponse(user, company, reportSetting);
    }

    @Transactional
    public MyPageResponse updateMyPage(UserPrincipal principal, MyPageUpdateRequest request) {
        User user = userRepository.findByIdAndDeletedAtIsNull(principal.getId())
                .orElseThrow(UserNotFoundException::new);
        Role role = user.getRole();
        Company company = user.getCompany();

        updateEmailIfChanged(request, user, role);
        updateBrandNameIfChanged(request, company, role);

        ReportSettingResponse reportSetting = request.reportSetting() != null
                ? applyReportSetting(company, request.reportSetting())
                : findReportSetting(company.getId());

        return buildResponse(user, company, reportSetting);
    }

    private void updateEmailIfChanged(MyPageUpdateRequest request, User user, Role role) {
        if (request.email() == null || request.email().equals(user.getEmail()))
            return;

        if (role == Role.ROOT)
            throw new FieldNotEditableException();

        if (userRepository.existsByEmailAndDeletedAtIsNull(request.email()))
            throw new DuplicateEmailException();

        emailVerificationService.validateVerificationToken(request.email(), request.verificationToken());

        user.changeEmail(request.email());
    }

    private void updateBrandNameIfChanged(MyPageUpdateRequest request, Company company, Role role) {
        if (request.brandName() == null || request.brandName().equals(company.getName()))
            return;

        if (role != Role.ROOT)
            throw new FieldNotEditableException();

        company.rename(request.brandName());
    }

    private ReportSettingResponse applyReportSetting(Company company, ReportSettingRequest request) {
        validateSendDay(request.sendDay());
        validateRecipients(request.recipients());

        MonthlyReportSetting setting;
        var existingSetting = monthlyReportSettingRepository.findByCompanyIdAndDeletedAtIsNull(company.getId());
        if (existingSetting.isPresent()) {
            setting = existingSetting.get();
            setting.update(request.enabled(), request.sendDay(), request.sendTime());
        } else {
            setting = monthlyReportSettingRepository.save(
                    MonthlyReportSetting.create(company, request.enabled(), request.sendDay(), request.sendTime())
            );
        }

        List<RecipientResponse> recipients = replaceRecipients(company, request.recipients());

        return ReportSettingResponse.of(setting, recipients);
    }

    private void validateSendDay(int sendDay) {
        if (sendDay < MIN_SEND_DAY || sendDay > MAX_SEND_DAY)
            throw new InvalidSendDayException();
    }

    private void validateRecipients(List<RecipientRequest> recipients) {
        if (recipients.size() > MAX_RECIPIENTS)
            throw new TooManyRecipientsException();

        long distinctEmailCount = recipients.stream()
                .map(RecipientRequest::email)
                .distinct()
                .count();
        if (distinctEmailCount != recipients.size())
            throw new DuplicateRecipientEmailException();
    }

    private List<RecipientResponse> replaceRecipients(Company company, List<RecipientRequest> requests) {
        List<MonthlyReportRecipient> existing =
                monthlyReportRecipientRepository.findAllByCompanyIdAndDeletedAtIsNullOrderByIdAsc(company.getId());

        Map<Long, MonthlyReportRecipient> existingById = new HashMap<>();
        for (MonthlyReportRecipient recipient : existing)
            existingById.put(recipient.getId(), recipient);

        List<MonthlyReportRecipient> toDelete = new ArrayList<>();
        for (MonthlyReportRecipient recipient : existing) {
            boolean stillRequested = requests.stream()
                    .anyMatch(r -> recipient.getId().equals(r.recipientId()));
            if (!stillRequested)
                toDelete.add(recipient);
        }
        if (!toDelete.isEmpty())
            monthlyReportRecipientRepository.deleteAll(toDelete);

        List<MonthlyReportRecipient> result = new ArrayList<>();
        for (RecipientRequest request : requests) {
            if (request.recipientId() == null) {
                result.add(monthlyReportRecipientRepository.save(
                        MonthlyReportRecipient.create(company, request.department(), request.email())
                ));
                continue;
            }

            MonthlyReportRecipient recipient = existingById.get(request.recipientId());
            if (recipient == null)
                throw new RecipientNotOwnedException();

            recipient.update(request.department(), request.email());
            result.add(recipient);
        }

        return result.stream()
                .sorted(Comparator.comparing(MonthlyReportRecipient::getId))
                .map(RecipientResponse::of)
                .toList();
    }

    private ReportSettingResponse findReportSetting(Long companyId) {
        return monthlyReportSettingRepository.findByCompanyIdAndDeletedAtIsNull(companyId)
                .map(setting -> ReportSettingResponse.of(setting, findRecipients(companyId)))
                .orElseGet(ReportSettingResponse::defaultValue);
    }

    private List<RecipientResponse> findRecipients(Long companyId) {
        return monthlyReportRecipientRepository.findAllByCompanyIdAndDeletedAtIsNullOrderByIdAsc(companyId).stream()
                .map(RecipientResponse::of)
                .toList();
    }

    private MyPageResponse buildResponse(User user, Company company, ReportSettingResponse reportSetting) {
        String companyKey = null;
        boolean companyKeyIssued = false;
        if (user.getRole() == Role.ROOT) {
            String joinKey = company.getJoinKey();
            companyKeyIssued = joinKey != null;
            companyKey = companyKeyMasker.mask(joinKey);
        }

        return MyPageResponse.of(user, companyKey, companyKeyIssued, reportSetting);
    }
}
