package com.aivle.sellon.domain.guideline.dto.response;

import com.aivle.sellon.domain.guideline.entity.Guideline;
import com.aivle.sellon.domain.guideline.entity.GuidelineSummary;
import com.aivle.sellon.domain.guideline.enums.GuidelineAvailability;

import java.time.LocalDateTime;

public record GuidelineListItemResponse(
        String guidelineId,
        String title,
        String productName,
        LocalDateTime detectedAt,
        GuidelineAvailability status
) {
    public static GuidelineListItemResponse of(GuidelineSummary summary, GuidelineAvailability status) {
        Guideline guideline = summary.getGuideline();

        return new GuidelineListItemResponse(
                guideline.getGuidelineId(),
                summary.getTitle(),
                summary.getProductName(),
                summary.getDetectedAt(),
                status
        );
    }
}
