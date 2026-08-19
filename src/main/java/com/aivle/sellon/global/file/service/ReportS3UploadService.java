package com.aivle.sellon.global.file.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * report 버킷에 백엔드가 직접 만든 파일을 올린다.
 * 다운로드 링크를 {@link S3PresignedUrlService}가 같은 버킷 기준으로 발급하므로 버킷 설정을 공유한다.
 */
@Service
@RequiredArgsConstructor
public class ReportS3UploadService {

    private static final String PDF_CONTENT_TYPE = "application/pdf";

    private final S3Client reportS3Client;

    @Value("${cloud.aws.s3.report.bucket}")
    private String bucket;

    public void uploadPdf(String key, byte[] content) {
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(PDF_CONTENT_TYPE)
                .build();

        reportS3Client.putObject(request, RequestBody.fromBytes(content));
    }
}
