package com.aivle.sellon.domain.channels.controller.productmapping;

import com.aivle.sellon.domain.channels.service.productmapping.ChannelProductBatchService;
import com.aivle.sellon.global.common.ApiResponse;
import com.aivle.sellon.global.security.principal.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * Mock Producer의 채널 상품 카탈로그(input_channel_products.csv) 적재와,
 * 매칭 툴(docker_mapping_tool)로 보낼 미매칭 상품 export.
 * 크로스채널 매칭이 목적이라 특정 usersChannel이 아닌 회사 단위로 다룬다.
 */
@RestController
@RequestMapping("/channels/product-mappings/catalog")
@RequiredArgsConstructor
public class ChannelProductCatalogController {

    private final ChannelProductBatchService channelProductBatchService;

    @PostMapping("/import")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> importChannelProducts(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam("file") MultipartFile file
    ) {
        int count = channelProductBatchService.importChannelProducts(principal.getCompanyId(), file);
        return ApiResponse.ok(Map.of("importedCount", count));
    }

    /**
     * 회사가 연동한 모든 채널의 미매칭 상품을 매칭 툴 input_channel_products.csv 포맷으로 내보낸다.
     * 파일 다운로드라 공통 ApiResponse 포맷 대상이 아니라 그대로 byte[]로 응답한다.
     */
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportUnmatched(@AuthenticationPrincipal UserPrincipal principal) {
        byte[] csv = channelProductBatchService.exportUnmatchedCsv(principal.getCompanyId());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=input_channel_products.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }
}
