package com.aivle.sellon.domain.guideline.service;

import com.aivle.sellon.domain.company.entity.Company;
import com.aivle.sellon.domain.company.exception.CompanyNotFoundException;
import com.aivle.sellon.domain.company.repository.CompanyRepository;
import com.aivle.sellon.domain.guideline.dto.message.GuidelinePayload;
import com.aivle.sellon.domain.guideline.entity.Guideline;
import com.aivle.sellon.domain.guideline.entity.GuidelineSummary;
import com.aivle.sellon.domain.guideline.repository.GuidelineRepository;
import com.aivle.sellon.domain.guideline.repository.GuidelineSummaryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;

/**
 * 큐로 들어오는 CS 가이드라인을 저장만 한다. 메일 발송은 이 흐름의 트리거가 아니라
 * 화면에서 별도 버튼을 눌러야 시작되는 후속 기능이라 여기서 다루지 않는다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GuidelineService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final GuidelineRepository guidelineRepository;
    private final GuidelineSummaryRepository guidelineSummaryRepository;
    private final CompanyRepository companyRepository;

    @Transactional
    public void saveGeneratedGuideline(String companyKey, GuidelinePayload payload) {
        Company company = findCompany(companyKey);

        Guideline guideline = guidelineRepository.findByCompanyIdAndGuidelineId(company.getId(), payload.guidelineId())
                .map(existing -> {
                    existing.update(payload);
                    return existing;
                })
                .orElseGet(() -> guidelineRepository.save(Guideline.create(payload, company)));

        saveSummary(guideline, payload.sourcePayload());
    }

    // company_id는 DB PK가 아니라 회원가입 시 발급되는 회사 식별키(join_key)다.
    private Company findCompany(String companyKey) {
        if (companyKey == null || companyKey.isBlank())
            throw new CompanyNotFoundException();

        return companyRepository.findByJoinKey(companyKey)
                .orElseThrow(CompanyNotFoundException::new);
    }

    /**
     * source_payload.input에서 화면에 자주 뿌릴 값만 뽑아 별도 테이블에 색인처럼 보관한다.
     * input의 정식 스키마가 문서로 확정돼 있지 않아, 필드가 비거나 형식이 달라도
     * 본체(Guideline) 저장은 막지 않고 요약만 생략한다.
     */
    private void saveSummary(Guideline guideline, JsonNode sourcePayload) {
        JsonNode input = sourcePayload != null ? sourcePayload.get("input") : null;
        if (input == null || input.isNull()) {
            log.warn("가이드라인 요약 생략 - source_payload.input 없음. guidelineId={}", guideline.getGuidelineId());
            return;
        }

        LocalDateTime detectedAt = parseDetectedAt(input.path("detected_at").asString(null));
        String productGroupId = input.path("product_group_id").asString(null);
        String productName = input.path("product_name").asString(null);
        String channel = input.path("channel").asString(null);
        String rootCauseLabel = input.path("root_cause").path("label").asString(null);
        String title = "[%s] | %s - %s".formatted(guideline.getAlertId(), channel, rootCauseLabel);
        JsonNode linkedInquiriesNode = input.get("linked_inquiries");
        String linkedInquiries = linkedInquiriesNode != null ? linkedInquiriesNode.toString() : null;

        guidelineSummaryRepository.findByGuidelineId(guideline.getId())
                .ifPresentOrElse(
                        existing -> existing.update(detectedAt, productGroupId, productName, title, linkedInquiries),
                        () -> guidelineSummaryRepository.save(GuidelineSummary.create(
                                guideline, detectedAt, productGroupId, productName, title, linkedInquiries))
                );
    }

    private LocalDateTime parseDetectedAt(String raw) {
        if (raw == null)
            return null;

        try {
            return LocalDateTime.ofInstant(Instant.parse(raw), KST);
        } catch (DateTimeParseException e) {
            log.warn("가이드라인 요약 - detected_at 파싱 실패. value={}", raw, e);
            return null;
        }
    }
}
