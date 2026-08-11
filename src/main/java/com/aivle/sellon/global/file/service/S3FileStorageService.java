package com.aivle.sellon.global.file.service;

import com.aivle.sellon.global.file.enums.AcceptableFileType;
import com.aivle.sellon.global.file.enums.FileDirectory;
import com.aivle.sellon.global.file.exception.InvalidExtensionException;
import com.aivle.sellon.global.file.exception.InvalidFileException;
import com.aivle.sellon.global.file.utils.GlobalFileValidator;
import lombok.RequiredArgsConstructor;
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
 * 2. S3Client, S3Presigner를 스프링 빈으로 등록하는 설정 클래스 추가 (global/file/config)
 * 3. application.yaml에 아래 값 추가 후 file.storage-type: s3 로 전환
 *      file:
 *        storage-type: s3
 *        s3:
 *          bucket: {버킷명}
 *
 * directory별 최대 용량(FileDirectory.getFileSize())과 presigned URL 만료시간
 * (FileDirectory.getUploadTime())은 FileDirectory enum에서 관리한다.
 */
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "file.storage-type", havingValue = "s3")
public class S3FileStorageService implements FileStorageService {

    private final GlobalFileValidator globalFileValidator;

    // TODO: S3Client, S3Presigner 주입 (AWS SDK 의존성 추가 후)
    // private final S3Client s3Client;
    // private final S3Presigner s3Presigner;

    @Value("${file.s3.bucket:}")
    private String bucketName;

    @Override
    public String store(MultipartFile file, FileDirectory directory) {
        if (file == null || file.isEmpty())
            throw new InvalidFileException();

        if (file.getSize() > directory.getFileSize())
            throw new InvalidFileException();

        AcceptableFileType fileType = globalFileValidator.validateAndGetExtension(file.getOriginalFilename());

        if (!directory.allows(fileType))
            throw new InvalidExtensionException();

        String key = directory.getPrefix() + "/" + UUID.randomUUID() + "." + fileType.getExtension();

        // TODO: s3Client.putObject(...) 로 실제 업로드 구현
        // PutObjectRequest request = PutObjectRequest.builder()
        //         .bucket(bucketName)
        //         .key(key)
        //         .contentType(fileType.getMimeType())
        //         .build();
        // s3Client.putObject(request, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

        return key;
    }

    @Override
    public String getAccessUrl(String storedKey, FileDirectory directory) {
        // TODO: s3Presigner로 directory.getUploadTime() 만료시간의 GET presigned URL 발급
        // GetObjectRequest getRequest = GetObjectRequest.builder()
        //         .bucket(bucketName)
        //         .key(storedKey)
        //         .build();
        // GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
        //         .signatureDuration(directory.getUploadTime())
        //         .getObjectRequest(getRequest)
        //         .build();
        // return s3Presigner.presignGetObject(presignRequest).url().toString();

        throw new UnsupportedOperationException("S3 연동 구현 전입니다. AWS SDK 연동 후 완성해주세요.");
    }
}
