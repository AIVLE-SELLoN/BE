package com.aivle.sellon.global.file;

import com.aivle.sellon.global.file.exception.InvalidFileException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/**
 * S3 기반 파일 저장 구현체 (뼈대 단계).
 *
 * 활성화하려면:
 * 1. build.gradle에 AWS SDK v2 의존성 추가
 *      implementation platform('software.amazon.awssdk:bom:{version}')
 *      implementation 'software.amazon.awssdk:s3'
 * 2. S3Client, S3Presigner를 스프링 빈으로 등록하는 설정 클래스 추가
 * 3. application.yaml에 아래 값 추가 후 file.storage-type: s3 로 전환
 *      file:
 *        storage-type: s3
 *        s3:
 *          bucket: {버킷명}
 *          presigned-url-expire-minutes: 10
 *
 * 이 클래스는 file.storage-type=s3 일 때만 빈으로 등록되고,
 * 기본값(local)일 때는 LocalFileStorageService가 대신 사용된다.
 */
@Service
@ConditionalOnProperty(name = "file.storage-type", havingValue = "s3")
public class S3FileStorageService implements FileStorageService {

    private static final String KEY_PREFIX = "inquiries/";

    // TODO: S3Client, S3Presigner 주입 (AWS SDK 의존성 추가 후)
    // private final S3Client s3Client;
    // private final S3Presigner s3Presigner;

    @Value("${file.s3.bucket:}")
    private String bucketName;

    @Value("${file.s3.presigned-url-expire-minutes:10}")
    private long presignedUrlExpireMinutes;

    @Override
    public String store(MultipartFile file) {
        if (file == null || file.isEmpty())
            throw new InvalidFileException();

        String key = generateKey(file.getOriginalFilename());

        // TODO: s3Client.putObject(...) 로 실제 업로드 구현
        // PutObjectRequest request = PutObjectRequest.builder()
        //         .bucket(bucketName)
        //         .key(key)
        //         .contentType(file.getContentType())
        //         .build();
        // s3Client.putObject(request, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

        return key;
    }

    @Override
    public String getAccessUrl(String storedKey) {
        // TODO: s3Presigner로 만료시간(presignedUrlExpireMinutes)이 있는 GET presigned URL 발급
        // GetObjectRequest getRequest = GetObjectRequest.builder()
        //         .bucket(bucketName)
        //         .key(storedKey)
        //         .build();
        // GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
        //         .signatureDuration(Duration.ofMinutes(presignedUrlExpireMinutes))
        //         .getObjectRequest(getRequest)
        //         .build();
        // return s3Presigner.presignGetObject(presignRequest).url().toString();

        throw new UnsupportedOperationException("S3 연동 구현 전입니다. AWS SDK 연동 후 완성해주세요.");
    }

    private String generateKey(String originalFilename) {
        return KEY_PREFIX + UUID.randomUUID() + extractExtension(originalFilename);
    }

    private String extractExtension(String originalFilename) {
        if (originalFilename == null || !originalFilename.contains("."))
            return "";
        return originalFilename.substring(originalFilename.lastIndexOf("."));
    }
}
