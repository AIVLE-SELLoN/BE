package com.aivle.sellon.domain.company.util;

import com.aivle.sellon.domain.company.exception.CompanyKeyGenerationException;
import com.aivle.sellon.domain.company.repository.CompanyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
@RequiredArgsConstructor
public class CompanyKeyGenerator {

    private static final String PREFIX = "SLN-";
    private static final String CHARSET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int RANDOM_LENGTH = 16;
    private static final int MAX_ATTEMPTS = 5;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final CompanyRepository companyRepository;

    public String generate() {
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            String candidate = PREFIX + randomString(RANDOM_LENGTH);

            if (!companyRepository.existsByJoinKey(candidate))
                return candidate;
        }

        throw new CompanyKeyGenerationException();
    }

    private String randomString(int length) {
        StringBuilder builder = new StringBuilder(length);
        for (int i = 0; i < length; i++)
            builder.append(CHARSET.charAt(RANDOM.nextInt(CHARSET.length())));

        return builder.toString();
    }
}
