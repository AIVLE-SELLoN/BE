package com.aivle.sellon.domain.guideline.service;

import com.aivle.sellon.domain.guideline.entity.PdfS3Meta;
import com.aivle.sellon.global.file.service.S3PresignedUrlService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * CS 가이드라인 PDF도 report와 같은 버킷(다른 폴더)에 저장되므로 report와 동일한 presigner를 쓴다.
 * 저장된 presigned URL은 발급 +7일이면 만료되므로 조회·발송 시점마다 s3_full_key로 새로 발급한다.
 */
@Service
@RequiredArgsConstructor
public class GuidelineDownloadUrlService {

    private final S3PresignedUrlService s3PresignedUrlService;

    @Value("${guideline.download-url.expire}")
    private long downloadUrlExpireMs;

    /**
     * @return 다운로드 URL. PDF가 없거나 S3 Lifecycle로 이미 삭제됐으면 null
     */
    public String generate(PdfS3Meta meta) {
        if (!isAvailable(meta))
            return null;

        return s3PresignedUrlService.generateDownloadUrl(meta.getS3FullKey(), Duration.ofMillis(downloadUrlExpireMs));
    }

    /** @return 파일이 아직 S3에 남아있어 다운로드/메일 전송이 가능하면 true */
    public boolean isAvailable(PdfS3Meta meta) {
        return meta != null && meta.getS3FullKey() != null && !isObjectExpired(meta);
    }

    private boolean isObjectExpired(PdfS3Meta meta) {
        LocalDateTime objectExpiresAt = meta.getObjectExpiresAt();
        return objectExpiresAt != null && objectExpiresAt.isBefore(LocalDateTime.now());
    }
}
