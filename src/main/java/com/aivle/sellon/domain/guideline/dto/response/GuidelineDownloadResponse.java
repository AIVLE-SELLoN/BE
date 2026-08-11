package com.aivle.sellon.domain.guideline.dto.response;

/**
 * @param regenerated 보관 기한이 지나 이번 요청에서 파일을 다시 만들었으면 true
 */
public record GuidelineDownloadResponse(
        String originalFileName,
        String downloadUrl,
        boolean regenerated
) {
}
