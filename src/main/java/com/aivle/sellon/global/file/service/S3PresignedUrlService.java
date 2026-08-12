package com.aivle.sellon.global.file.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Service
@RequiredArgsConstructor
public class S3PresignedUrlService {

    private final S3Presigner reportS3Presigner;

    @Value("${cloud.aws.s3.report.bucket}")
    private String bucket;

    /**
     * 업로드 시점 메타데이터와 무관하게 항상 인라인 PDF로 열리도록 강제한다. (월간 리포트 뷰어 전용)
     * 다운로드로 받게 하려면 파일명을 지정하는 {@link #generateDownloadUrl(String, String, Duration)}을 쓴다.
     */
    public String generateDownloadUrl(String key, Duration expiration) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .responseContentType("application/pdf")
                .responseContentDisposition("inline")
                .build();

        return presign(getObjectRequest, expiration);
    }

    /**
     * S3에 저장된 객체명은 UUID라 그대로 받으면 사용자가 알아볼 수 없다.
     * 내려받을 때 원본 파일명이 그대로 붙도록 응답 헤더를 지정해 발급한다.
     */
    public String generateDownloadUrl(String key, String fileName, Duration expiration) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .responseContentDisposition(contentDisposition(fileName))
                .build();

        return presign(getObjectRequest, expiration);
    }

    private String presign(GetObjectRequest getObjectRequest, Duration expiration) {
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(expiration)
                .getObjectRequest(getObjectRequest)
                .build();

        return reportS3Presigner.presignGetObject(presignRequest).url().toString();
    }

    /**
     * 한글 파일명도 깨지지 않도록 RFC 5987(filename*)로 인코딩하되, filename*을 모르는 오래된
     * 클라이언트를 위해 RFC 6266이 권장하는 ASCII filename 폴백도 함께 둔다.
     */
    private String contentDisposition(String fileName) {
        String encoded = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
        String asciiFallback = fileName.replaceAll("[^\\x20-\\x7E]", "_");
        return "attachment; filename=\"%s\"; filename*=UTF-8''%s".formatted(asciiFallback, encoded);
    }
}
