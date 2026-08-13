package com.aivle.sellon.domain.auth.config;

import com.aivle.sellon.domain.company.entity.Company;
import com.aivle.sellon.domain.company.repository.CompanyRepository;
import com.aivle.sellon.domain.user.entity.User;
import com.aivle.sellon.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminAccountInitializer implements ApplicationRunner {

    private static final String ADMIN_COMPANY_NAME = "SELLON_INTERNAL";

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${sellon.admin.seed.email:}")
    private String seedEmail;

    @Value("${sellon.admin.seed.password:}")
    private String seedPassword;

    @Value("${sellon.admin.seed.name:관리자}")
    private String seedName;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (seedEmail.isBlank() || seedPassword.isBlank()) {
            log.info("sellon.admin.seed.email/password 미설정 - 관리자 계정 시딩을 건너뜁니다.");
            return;
        }

        if (userRepository.existsByEmailAndDeletedAtIsNull(seedEmail)) {
            log.info("관리자 계정({})이 이미 존재합니다 - 시딩을 건너뜁니다.", seedEmail);
            return;
        }

        Company company = companyRepository.findByName(ADMIN_COMPANY_NAME)
                .orElseGet(() -> companyRepository.save(Company.create(ADMIN_COMPANY_NAME)));

        userRepository.save(User.createAdmin(
                seedEmail,
                passwordEncoder.encode(seedPassword),
                seedName,
                company
        ));

        log.info("관리자 계정({})을 새로 생성했습니다.", seedEmail);
    }
}
