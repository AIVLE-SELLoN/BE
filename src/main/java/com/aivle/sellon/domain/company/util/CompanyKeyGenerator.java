package com.aivle.sellon.domain.company.util;

import com.aivle.sellon.domain.company.exception.CompanyKeyGenerationException;
import com.aivle.sellon.domain.company.repository.CompanyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CompanyKeyGenerator {

    private static final String PREFIX = "sLN-";
    private static final int MAX_ATTEMPTS = 5;

    private final CompanyRepository companyRepository;

    public String generate() {
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            String candidate = PREFIX + UUID.randomUUID().toString()
                    .replace("-", "")
                    .substring(0, 8)
                    .toUpperCase();

            if (!companyRepository.existsByJoinKey(candidate))
                return candidate;
        }

        throw new CompanyKeyGenerationException();
    }
}
