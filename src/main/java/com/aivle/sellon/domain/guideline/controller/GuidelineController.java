package com.aivle.sellon.domain.guideline.controller;

import com.aivle.sellon.domain.guideline.dto.request.GuidelineApprovalRequest;
import com.aivle.sellon.domain.guideline.dto.response.GuidelineDetailResponse;
import com.aivle.sellon.domain.guideline.dto.response.GuidelineDownloadResponse;
import com.aivle.sellon.domain.guideline.dto.response.GuidelineFileResponse;
import com.aivle.sellon.domain.guideline.dto.response.GuidelineListItemResponse;
import com.aivle.sellon.domain.guideline.service.GuidelineApprovalService;
import com.aivle.sellon.domain.guideline.service.GuidelineFileService;
import com.aivle.sellon.domain.guideline.service.GuidelineMailService;
import com.aivle.sellon.domain.guideline.service.GuidelineService;
import com.aivle.sellon.global.common.ApiResponse;
import com.aivle.sellon.global.common.dto.CursorPageResponse;
import com.aivle.sellon.global.security.principal.UserPrincipal;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/sellon/guidelines")
@RequiredArgsConstructor
@Validated
public class GuidelineController {

    private final GuidelineService guidelineService;
    private final GuidelineMailService guidelineMailService;
    private final GuidelineFileService guidelineFileService;
    private final GuidelineApprovalService guidelineApprovalService;

    @GetMapping
    public ResponseEntity<ApiResponse<CursorPageResponse<GuidelineListItemResponse>>> getGuidelines(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") @Positive int size,
            @RequestParam(required = false) String q
    ) {
        return ApiResponse.ok(guidelineService.getGuidelines(principal, cursor, size, q));
    }

    @GetMapping("/{guidelineId}")
    public ResponseEntity<ApiResponse<GuidelineDetailResponse>> getGuideline(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String guidelineId
    ) {
        return ApiResponse.ok(guidelineService.getGuidelineDetail(principal, guidelineId));
    }

    @GetMapping("/files")
    public ResponseEntity<ApiResponse<CursorPageResponse<GuidelineFileResponse>>> getFiles(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") @Positive int size,
            @RequestParam(required = false) String q
    ) {
        return ApiResponse.ok(guidelineFileService.getFiles(principal, cursor, size, q));
    }

    /**
     * 다운로드 버튼을 누른 시점에 파일이 아직 살아 있는지 확인하고, 만료됐으면 다시 만들어 링크를 준다.
     * 재생성이라는 쓰기가 일어나므로 조회(GET)가 아닌 POST로 둔다.
     */
    @PostMapping("/{guidelineId}/download")
    public ResponseEntity<ApiResponse<GuidelineDownloadResponse>> download(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String guidelineId
    ) {
        return ApiResponse.ok(guidelineFileService.download(principal, guidelineId));
    }

    @PostMapping("/{guidelineId}/mail")
    public ResponseEntity<ApiResponse<Void>> sendMail(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String guidelineId
    ) {
        guidelineMailService.sendMail(principal, guidelineId);
        return ApiResponse.ok();
    }

    /**
     * 운영 MD가 상세 페이지에서 승인하며 코멘트를 남긴다. 이미 승인된 건이면 코멘트만 갱신한다.
     */
    @PostMapping("/{guidelineId}/approval")
    public ResponseEntity<ApiResponse<Void>> approve(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String guidelineId,
            @RequestBody GuidelineApprovalRequest request
    ) {
        guidelineApprovalService.approve(principal, guidelineId, request.comment());
        return ApiResponse.ok();
    }
}
