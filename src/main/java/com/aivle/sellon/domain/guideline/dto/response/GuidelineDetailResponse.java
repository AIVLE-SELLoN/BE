package com.aivle.sellon.domain.guideline.dto.response;

import com.aivle.sellon.domain.guideline.entity.Guideline;
import com.aivle.sellon.domain.guideline.entity.GuidelineApproval;
import com.aivle.sellon.domain.guideline.entity.GuidelineSummary;
import com.aivle.sellon.domain.guideline.enums.GuidelineAvailability;

import java.time.LocalDateTime;

public record GuidelineDetailResponse(
        String guidelineId,
        String title,
        String productName,
        LocalDateTime detectedAt,
        GuidelineAvailability status,
        String downloadUrl,
        boolean approved,
        String comment
) {
    public static GuidelineDetailResponse of(
            GuidelineSummary summary, GuidelineAvailability status, String downloadUrl, GuidelineApproval approval
    ) {
        Guideline guideline = summary.getGuideline();

        return new GuidelineDetailResponse(
                guideline.getGuidelineId(),
                summary.getTitle(),
                summary.getProductName(),
                summary.getDetectedAt(),
                status,
                downloadUrl,
                approval != null,
                approval != null ? approval.getComment() : null
        );
    }
}
