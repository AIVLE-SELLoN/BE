package com.aivle.sellon.domain.mypage.service;

import com.aivle.sellon.domain.company.entity.Company;
import com.aivle.sellon.domain.company.exception.CompanyNotFoundException;
import com.aivle.sellon.domain.company.repository.CompanyRepository;
import com.aivle.sellon.domain.company.util.CompanyKeyGenerator;
import com.aivle.sellon.domain.mypage.exception.CompanyKeyNotIssuedException;
import com.aivle.sellon.domain.mypage.exception.NotCompanyOwnerException;
import com.aivle.sellon.domain.user.enums.Role;
import com.aivle.sellon.global.security.principal.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MyPageService {

    private final CompanyRepository companyRepository;
    private final CompanyKeyGenerator companyKeyGenerator;

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
}
