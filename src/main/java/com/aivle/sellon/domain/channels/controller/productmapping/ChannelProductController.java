package com.aivle.sellon.domain.channels.controller.productmapping;

import com.aivle.sellon.domain.channels.dto.request.ConnectMappingRequest;
import com.aivle.sellon.domain.channels.dto.request.NewGroupRequest;
import com.aivle.sellon.domain.channels.dto.response.ChannelProductResponse;
import com.aivle.sellon.domain.channels.dto.response.MappingSummaryResponse;
import com.aivle.sellon.domain.channels.dto.response.MatchCandidateResponse;
import com.aivle.sellon.domain.channels.service.productmapping.ChannelProductBatchService;
import com.aivle.sellon.domain.channels.service.productmapping.ChannelProductService;
import com.aivle.sellon.global.security.principal.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/channels/{usersChannelKey}/product-mappings")
@RequiredArgsConstructor
public class ChannelProductController {

    private final ChannelProductService channelProductService;
    private final ChannelProductBatchService channelProductBatchService;

    @GetMapping
    public ResponseEntity<List<ChannelProductResponse>> getMappings(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long usersChannelKey,
            @RequestParam(required = false) Boolean matched,
            @RequestParam(required = false) String keyword
    ) {
        return ResponseEntity.ok(channelProductService.getMappings(principal.getCompanyId(), usersChannelKey, matched, keyword));
    }

    @GetMapping("/summary")
    public ResponseEntity<MappingSummaryResponse> getSummary(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long usersChannelKey
    ) {
        return ResponseEntity.ok(channelProductService.getSummary(principal.getCompanyId(), usersChannelKey));
    }

    @GetMapping("/{channelProductKey}/candidates")
    public ResponseEntity<List<MatchCandidateResponse>> getCandidates(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long usersChannelKey,
            @PathVariable Long channelProductKey,
            @RequestParam(required = false) String keyword
    ) {
        return ResponseEntity.ok(channelProductService.getCandidates(principal.getCompanyId(), usersChannelKey, channelProductKey, keyword));
    }

    @PatchMapping("/{channelProductKey}/connect")
    public ResponseEntity<ChannelProductResponse> connect(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long usersChannelKey,
            @PathVariable Long channelProductKey,
            @RequestBody ConnectMappingRequest request
    ) {
        return ResponseEntity.ok(channelProductService.connect(principal.getCompanyId(), usersChannelKey, channelProductKey, request));
    }

    @PostMapping("/{channelProductKey}/new-group")
    public ResponseEntity<ChannelProductResponse> createNewGroup(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long usersChannelKey,
            @PathVariable Long channelProductKey,
            @RequestBody NewGroupRequest request
    ) {
        return ResponseEntity.ok(channelProductService.createNewGroup(principal.getCompanyId(), usersChannelKey, channelProductKey, request));
    }

    /**
     * 매칭 툴(docker_mapping_tool)의 input_channel_products.csv 포맷으로 미매칭 상품을 내려받는다.
     * 팀원이 이 파일로 도커 컨테이너를 수동 실행 -> mapping_result.csv/review_queue.csv를 아래 import API로 업로드.
     */
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportUnmatched(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long usersChannelKey
    ) {
        byte[] csv = channelProductBatchService.exportUnmatchedCsv(principal.getCompanyId(), usersChannelKey);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=input_channel_products.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }

    @PostMapping("/import/mapping-result")
    public ResponseEntity<Map<String, Integer>> importMappingResult(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long usersChannelKey,
            @RequestParam("file") MultipartFile file
    ) {
        int matchedCount = channelProductBatchService.importMappingResult(principal.getCompanyId(), usersChannelKey, file);
        return ResponseEntity.ok(Map.of("matchedCount", matchedCount));
    }

    @PostMapping("/import/review-queue")
    public ResponseEntity<Map<String, Integer>> importReviewQueue(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long usersChannelKey,
            @RequestParam("file") MultipartFile file
    ) {
        int rowCount = channelProductBatchService.importReviewQueue(principal.getCompanyId(), usersChannelKey, file);
        return ResponseEntity.ok(Map.of("rowCount", rowCount));
    }
}
