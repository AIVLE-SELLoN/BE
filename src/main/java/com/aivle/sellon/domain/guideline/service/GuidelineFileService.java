package com.aivle.sellon.domain.guideline.service;

import com.aivle.sellon.domain.guideline.dto.response.GuidelineDownloadResponse;
import com.aivle.sellon.domain.guideline.dto.response.GuidelineFileResponse;
import com.aivle.sellon.domain.guideline.entity.Guideline;
import com.aivle.sellon.domain.guideline.entity.PdfS3Meta;
import com.aivle.sellon.domain.guideline.enums.GuidelineAvailability;
import com.aivle.sellon.domain.guideline.exception.GuidelineDownloadUnavailableException;
import com.aivle.sellon.domain.guideline.exception.GuidelineNotFoundException;
import com.aivle.sellon.domain.guideline.repository.GuidelineRepository;
import com.aivle.sellon.global.common.dto.CursorPageResponse;
import com.aivle.sellon.global.common.utils.CursorUtils;
import com.aivle.sellon.global.security.principal.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 가이드라인 파일 히스토리 조회와 다운로드.
 * 파일은 7일이면 S3에서 사라지지만 source_payload는 남아 있어, 만료된 건은 내려받는 시점에 다시 만들어 준다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GuidelineFileService {

    private final GuidelineRepository guidelineRepository;
    private final GuidelineDownloadUrlService guidelineDownloadUrlService;
    private final GuidelineRegenerationService guidelineRegenerationService;
    private final CursorUtils cursorUtils;

    public CursorPageResponse<GuidelineFileResponse> getFiles(UserPrincipal principal, String cursor, int size) {
        Long cursorId = cursorUtils.toId(cursor);
        List<Guideline> guidelines = guidelineRepository
                .findAllWithFileByCompanyId(principal.getCompanyId(), cursorId, size + 1);

        boolean hasNext = guidelines.size() > size;
        List<Guideline> content = hasNext ? guidelines.subList(0, size) : guidelines;

        List<GuidelineFileResponse> files = content.stream()
                .map(guideline -> GuidelineFileResponse.of(guideline, resolveAvailability(guideline)))
                .toList();

        String nextCursor = hasNext ? cursorUtils.toCursor(content.get(content.size() - 1).getId()) : null;

        return new CursorPageResponse<>(files, nextCursor, hasNext);
    }

    /**
     * 보관 기한이 지났으면 파일을 다시 만든 뒤 링크를 준다.
     * 프론트는 목록의 status로 버튼을 "재생성"/"다운로드"로 나눠 보여주지만, 어느 쪽을 눌러도 이 API 하나로 처리된다.
     */
    public GuidelineDownloadResponse download(UserPrincipal principal, String guidelineId) {
        Guideline guideline = guidelineRepository
                .findByCompanyIdAndGuidelineId(principal.getCompanyId(), guidelineId)
                .orElseThrow(GuidelineNotFoundException::new);

        PdfS3Meta meta = guideline.getPdfS3Meta();

        // 애초에 파일이 올라온 적 없는 건(생성 실패 등)은 되살릴 원본 자체가 없다
        if (meta == null || meta.getS3FullKey() == null)
            throw new GuidelineDownloadUnavailableException();

        boolean regenerated = !guidelineDownloadUrlService.isAvailable(meta);
        if (regenerated)
            meta = guidelineRegenerationService.regenerate(guideline.getId());

        return new GuidelineDownloadResponse(
                meta.getOriginalFileName(),
                guidelineDownloadUrlService.generate(meta),
                regenerated
        );
    }

    private GuidelineAvailability resolveAvailability(Guideline guideline) {
        return guidelineDownloadUrlService.isAvailable(guideline.getPdfS3Meta())
                ? GuidelineAvailability.COMPLETED
                : GuidelineAvailability.EXPIRED;
    }
}
