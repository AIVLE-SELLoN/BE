package com.aivle.sellon.domain.guideline.service;

import com.aivle.sellon.domain.company.entity.Company;
import com.aivle.sellon.domain.guideline.dto.message.GuidelinePayload;
import com.aivle.sellon.domain.guideline.dto.response.GuidelineDownloadResponse;
import com.aivle.sellon.domain.guideline.dto.response.GuidelineFileResponse;
import com.aivle.sellon.domain.guideline.entity.Guideline;
import com.aivle.sellon.domain.guideline.entity.PdfS3Meta;
import com.aivle.sellon.domain.guideline.enums.GuidelineAvailability;
import com.aivle.sellon.domain.guideline.exception.GuidelineDownloadUnavailableException;
import com.aivle.sellon.domain.guideline.exception.GuidelineNotFoundException;
import com.aivle.sellon.domain.guideline.repository.GuidelineRepository;
import com.aivle.sellon.domain.report.enums.ReportStatus;
import com.aivle.sellon.global.common.dto.CursorPageResponse;
import com.aivle.sellon.global.common.utils.CursorUtils;
import com.aivle.sellon.global.security.principal.UserPrincipal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GuidelineFileServiceTest {

    private static final Long COMPANY_ID = 1L;
    private static final String GUIDELINE_ID = "GD-20260528-P001-COUPANG";

    @Mock
    private GuidelineRepository guidelineRepository;

    @Mock
    private GuidelineDownloadUrlService guidelineDownloadUrlService;

    @Mock
    private GuidelineRegenerationService guidelineRegenerationService;

    @Mock
    private CursorUtils cursorUtils;

    @InjectMocks
    private GuidelineFileService guidelineFileService;

    private final UserPrincipal principal = UserPrincipal.ofClaims(1L, "cs@example.com", null, COMPANY_ID);

    @Test
    @DisplayName("파일이 아직 유효하면 재생성 없이 다운로드 링크만 발급한다")
    void downloadsWithoutRegeneration() {
        Guideline guideline = guideline(meta("cs-guideline_202605_uuid-old.pdf"));
        when(guidelineRepository.findByCompanyIdAndGuidelineId(COMPANY_ID, GUIDELINE_ID))
                .thenReturn(Optional.of(guideline));
        when(guidelineDownloadUrlService.isAvailable(any())).thenReturn(true);
        when(guidelineDownloadUrlService.generate(any())).thenReturn("https://s3/download");

        GuidelineDownloadResponse response = guidelineFileService.download(principal, GUIDELINE_ID);

        assertFalse(response.regenerated());
        assertEquals("https://s3/download", response.downloadUrl());
        assertEquals("cs-guideline_202605_ALT-20260528-P001-COUPANG.pdf", response.originalFileName());
        verify(guidelineRegenerationService, never()).regenerate(anyLong());
    }

    @Test
    @DisplayName("보관 기한이 지난 파일은 다운로드 시 재생성한 뒤 링크를 발급한다")
    void regeneratesExpiredFileOnDownload() {
        Guideline guideline = guideline(meta("cs-guideline_202605_uuid-old.pdf"));
        ReflectionTestUtils.setField(guideline, "id", 7L);
        PdfS3Meta regenerated = meta("cs-guideline_202605_uuid-new.pdf");

        when(guidelineRepository.findByCompanyIdAndGuidelineId(COMPANY_ID, GUIDELINE_ID))
                .thenReturn(Optional.of(guideline));
        when(guidelineDownloadUrlService.isAvailable(any())).thenReturn(false);
        when(guidelineRegenerationService.regenerate(7L)).thenReturn(regenerated);
        when(guidelineDownloadUrlService.generate(regenerated)).thenReturn("https://s3/regenerated");

        GuidelineDownloadResponse response = guidelineFileService.download(principal, GUIDELINE_ID);

        assertTrue(response.regenerated());
        assertEquals("https://s3/regenerated", response.downloadUrl());
        verify(guidelineRegenerationService).regenerate(7L);
    }

    @Test
    @DisplayName("한 번도 파일이 올라온 적 없는 가이드라인은 되살릴 원본이 없어 다운로드할 수 없다")
    void rejectsDownloadWhenFileNeverExisted() {
        when(guidelineRepository.findByCompanyIdAndGuidelineId(COMPANY_ID, GUIDELINE_ID))
                .thenReturn(Optional.of(guideline(null)));

        assertThrows(GuidelineDownloadUnavailableException.class,
                () -> guidelineFileService.download(principal, GUIDELINE_ID));
        verify(guidelineRegenerationService, never()).regenerate(anyLong());
    }

    @Test
    @DisplayName("다른 회사의 가이드라인은 조회되지 않는다")
    void rejectsDownloadOfOtherCompanyGuideline() {
        when(guidelineRepository.findByCompanyIdAndGuidelineId(COMPANY_ID, GUIDELINE_ID))
                .thenReturn(Optional.empty());

        assertThrows(GuidelineNotFoundException.class,
                () -> guidelineFileService.download(principal, GUIDELINE_ID));
    }

    @Test
    @DisplayName("요청한 size보다 많이 조회되면 초과분을 잘라내고 다음 커서를 준다")
    void paginatesWithCursor() {
        // id 내림차순이라 페이지에 남는 first가 더 최신이고, 다음 커서는 잘라낸 페이지의 마지막 항목에서 나온다
        Guideline first = guideline(meta("a.pdf"));
        Guideline second = guideline(meta("b.pdf"));
        ReflectionTestUtils.setField(first, "id", 9L);
        ReflectionTestUtils.setField(second, "id", 5L);
        when(cursorUtils.toId(null)).thenReturn(null);
        when(guidelineRepository.findAllWithFileByCompanyId(COMPANY_ID, null, 2))
                .thenReturn(List.of(first, second));
        when(guidelineDownloadUrlService.isAvailable(any())).thenReturn(true);
        when(cursorUtils.toCursor(9L)).thenReturn("next-cursor");

        CursorPageResponse<GuidelineFileResponse> response = guidelineFileService.getFiles(principal, null, 1);

        assertEquals(1, response.content().size());
        assertTrue(response.hasNext());
        assertEquals("next-cursor", response.nextCursor());
    }

    @Test
    @DisplayName("마지막 페이지에서는 다음 커서를 주지 않는다")
    void marksLastPage() {
        when(cursorUtils.toId(null)).thenReturn(null);
        when(guidelineRepository.findAllWithFileByCompanyId(COMPANY_ID, null, 21))
                .thenReturn(List.of(guideline(meta("a.pdf"))));
        when(guidelineDownloadUrlService.isAvailable(any())).thenReturn(true);

        CursorPageResponse<GuidelineFileResponse> response = guidelineFileService.getFiles(principal, null, 20);

        assertEquals(1, response.content().size());
        assertFalse(response.hasNext());
        assertNull(response.nextCursor());
        verify(cursorUtils, never()).toCursor(anyLong());
    }

    @Test
    @DisplayName("보관 기한이 지난 파일은 목록에서 EXPIRED로 보여 재생성이 필요함을 알린다")
    void marksExpiredFileInList() {
        when(cursorUtils.toId(null)).thenReturn(null);
        when(guidelineRepository.findAllWithFileByCompanyId(COMPANY_ID, null, 21))
                .thenReturn(List.of(guideline(meta("a.pdf"))));
        when(guidelineDownloadUrlService.isAvailable(any())).thenReturn(false);

        CursorPageResponse<GuidelineFileResponse> response = guidelineFileService.getFiles(principal, null, 20);

        assertEquals(GuidelineAvailability.EXPIRED, response.content().get(0).status());
    }

    private Guideline guideline(PdfS3Meta meta) {
        GuidelinePayload payload = new GuidelinePayload(
                GUIDELINE_ID, "ALT-20260528-P001-COUPANG", ReportStatus.SUCCESS,
                null, JsonMapper.builder().build().readTree("{\"output\":{}}"), null, null);

        Guideline guideline = Guideline.create(payload, Company.create("마르디 메크르디"));
        guideline.replacePdfS3Meta(meta);
        return guideline;
    }

    private PdfS3Meta meta(String newFileName) {
        return PdfS3Meta.of(
                "SLN-1943576218438216", "마르디 메크르디", "sellon-reports",
                "reports/cs-guideline/SLN-1943576218438216/2026/05/",
                "cs-guideline_202605_ALT-20260528-P001-COUPANG.pdf", newFileName,
                "reports/cs-guideline/SLN-1943576218438216/2026/05/" + newFileName,
                24034L, null, LocalDateTime.of(2026, 5, 28, 14, 21), null,
                LocalDateTime.of(2026, 6, 4, 14, 21));
    }
}
