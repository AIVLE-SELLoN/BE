package com.aivle.sellon.global.file;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {

    /**
     * 파일을 저장하고 식별자를 반환한다.
     * - Local 구현체: 바로 접근 가능한 경로("/files/{filename}")를 반환
     * - S3 구현체: 접근 URL이 아닌 S3 object key를 반환 (DB에는 이 key가 저장됨)
     */
    String store(MultipartFile file);

    /**
     * store()가 반환한 식별자를 실제 접근 가능한 URL로 변환한다.
     * - Local 구현체: 입력값을 그대로 반환 (이미 접근 가능한 경로이므로)
     * - S3 구현체: key를 기반으로 presigned URL을 발급해서 반환 (만료시간 있음)
     */
    String getAccessUrl(String storedKey);
}
