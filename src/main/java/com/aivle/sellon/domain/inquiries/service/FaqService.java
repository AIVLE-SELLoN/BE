package com.aivle.sellon.domain.inquiries.service;

import com.aivle.sellon.domain.inquiries.dto.FaqResponse;
import com.aivle.sellon.domain.inquiries.repository.FaqRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FaqService {

    private final FaqRepository faqRepository;

    @Transactional(readOnly = true)
    public List<FaqResponse> getAllFaqs() {
        return faqRepository.findAll().stream()
            .map(f -> new FaqResponse(f.getFaqKey(), f.getFaqTitle(), f.getFaqQuestion(), f.getFaqAnswer(), f.getFaqCategory()))
            .toList();
    }
}
