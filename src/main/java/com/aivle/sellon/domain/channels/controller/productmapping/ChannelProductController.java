package com.aivle.sellon.domain.channels.controller.productmapping;

import com.aivle.sellon.domain.channels.service.productmapping.ChannelProductBatchService;
import com.aivle.sellon.global.common.ApiResponse;
import com.aivle.sellon.global.security.principal.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

import static org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;

@RestController
@RequestMapping("/channels/{usersChannelKey}/product-mappings")
@RequiredArgsConstructor
public class ChannelProductController {

    private final ChannelProductBatchService channelProductBatchService;

    @PostMapping(value = "/import/mapping-result", consumes = MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Map<String, Integer>>> importMappingResult(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long usersChannelKey,
            @RequestParam("file") MultipartFile file
    ) {
        int matchedCount = channelProductBatchService.importMappingResult(principal.getCompanyId(), usersChannelKey, file);
        return ApiResponse.ok(Map.of("matchedCount", matchedCount));
    }

    @PostMapping(value = "/import/review-queue", consumes = MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Map<String, Integer>>> importReviewQueue(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long usersChannelKey,
            @RequestParam("file") MultipartFile file
    ) {
        int rowCount = channelProductBatchService.importReviewQueue(principal.getCompanyId(), usersChannelKey, file);
        return ApiResponse.ok(Map.of("rowCount", rowCount));
    }
}
