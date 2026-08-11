package com.aivle.sellon.domain.guideline.dto.response;

import com.aivle.sellon.domain.guideline.entity.Guideline;
import com.aivle.sellon.domain.guideline.entity.PdfS3Meta;
import com.aivle.sellon.domain.guideline.enums.GuidelineAvailability;

import java.time.LocalDateTime;

/**
 * 가이드라인 파일 히스토리의 한 줄.
 * status가 EXPIRED면 보관 기한이 지나 S3에 파일이 없다는 뜻이고, 다운로드 시 재생성된다.
 */
public record GuidelineFileResponse(
        String guidelineId,
        String originalFileName,
        LocalDateTime createdAt,
        Long fileSizeBytes,
        GuidelineAvailability status
) {
    public static GuidelineFileResponse of(Guideline guideline, GuidelineAvailability status) {
        PdfS3Meta meta = guideline.getPdfS3Meta();

        return new GuidelineFileResponse(
                guideline.getGuidelineId(),
                meta.getOriginalFileName(),
                meta.getCreatedAt(),
                meta.getFileSizeBytes(),
                status
        );
    }
}
