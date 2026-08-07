package com.aivle.sellon.domain.report.service;

import com.aivle.sellon.domain.report.entity.PdfS3Meta;
import com.aivle.sellon.global.file.service.S3PresignedUrlService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 저장된 presigned URL은 발급 +7일이면 만료되는데 리포트는 6개월 살아있다.
 * 그래서 조회·발송 시점마다 s3_full_key로 새로 발급한다 (큐 컨벤션 §5 "링크 재발급").
 */
@Service
@RequiredArgsConstructor
public class ReportDownloadUrlService {

    private final S3PresignedUrlService s3PresignedUrlService;

    @Value("${report.download-url.expire}")
    private long downloadUrlExpireMs;

    /**
     * @return 다운로드 URL. PDF가 없거나 S3 Lifecycle로 이미 삭제됐으면 null
     */
    public String generate(PdfS3Meta meta) {
        if (meta == null || meta.getS3FullKey() == null)
            return null;

        if (isObjectExpired(meta))
            return null;

        return s3PresignedUrlService.generateDownloadUrl(meta.getS3FullKey(), Duration.ofMillis(downloadUrlExpireMs));
    }

    private boolean isObjectExpired(PdfS3Meta meta) {
        LocalDateTime objectExpiresAt = meta.getObjectExpiresAt();
        return objectExpiresAt != null && objectExpiresAt.isBefore(LocalDateTime.now());
    }
}
