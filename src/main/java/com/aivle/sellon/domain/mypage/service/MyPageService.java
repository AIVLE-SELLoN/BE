package com.aivle.sellon.domain.mypage.service;

import com.aivle.sellon.domain.company.entity.Company;
import com.aivle.sellon.domain.company.exception.CompanyNotFoundException;
import com.aivle.sellon.domain.company.repository.CompanyRepository;
import com.aivle.sellon.domain.company.util.CompanyKeyGenerator;
import com.aivle.sellon.domain.mypage.dto.response.MyPageResponse;
import com.aivle.sellon.domain.mypage.dto.response.RecipientResponse;
import com.aivle.sellon.domain.mypage.dto.response.ReportSettingResponse;
import com.aivle.sellon.domain.mypage.exception.CompanyKeyNotIssuedException;
import com.aivle.sellon.domain.mypage.exception.NotCompanyOwnerException;
import com.aivle.sellon.domain.mypage.repository.MonthlyReportRecipientRepository;
import com.aivle.sellon.domain.mypage.repository.MonthlyReportSettingRepository;
import com.aivle.sellon.domain.mypage.util.CompanyKeyMasker;
import com.aivle.sellon.domain.user.entity.User;
import com.aivle.sellon.domain.user.enums.Role;
import com.aivle.sellon.domain.user.exception.UserNotFoundException;
import com.aivle.sellon.domain.user.repository.UserRepository;
import com.aivle.sellon.global.security.principal.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MyPageService {

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final CompanyKeyGenerator companyKeyGenerator;
    private final MonthlyReportSettingRepository monthlyReportSettingRepository;
    private final MonthlyReportRecipientRepository monthlyReportRecipientRepository;
    private final CompanyKeyMasker companyKeyMasker;

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
        Role role = user.getRole();

        String companyKey = null;
        boolean companyKeyIssued = false;
        if (role == Role.ROOT) {
            String joinKey = company.getJoinKey();
            companyKeyIssued = joinKey != null;
            companyKey = companyKeyMasker.mask(joinKey);
        }

        ReportSettingResponse reportSetting = monthlyReportSettingRepository.findByCompanyIdAndDeletedAtIsNull(company.getId())
                .map(setting -> ReportSettingResponse.of(setting, findRecipients(company.getId())))
                .orElseGet(ReportSettingResponse::defaultValue);

        return MyPageResponse.of(user, companyKey, companyKeyIssued, reportSetting);
    }

    private List<RecipientResponse> findRecipients(Long companyId) {
        return monthlyReportRecipientRepository.findAllByCompanyIdAndDeletedAtIsNullOrderByIdAsc(companyId).stream()
                .map(RecipientResponse::of)
                .toList();
    }
}
