package com.aivle.sellon.global.file.enums;

import com.aivle.sellon.global.file.exception.ExtensionEmptyException;
import com.aivle.sellon.global.file.exception.InvalidDirectoryException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.Duration;
import java.util.Arrays;
import java.util.Set;

@Getter
@RequiredArgsConstructor
public enum FileDirectory {
    PROFILE("profiles/user-images",
            Duration.ofMinutes(3),      // 업로드용 presigned PUT 만료
            Duration.ofMinutes(15),     // 조회용 presigned GET 만료
            5 * 1024 * 1024L,
            Set.of(AcceptableFileType.JPG,
                   AcceptableFileType.JPEG,
                   AcceptableFileType.PNG)),

    // downloadTime: 노션 S3 구조 문서 기준 24시간 — 보고서 담당자 확인 필요
    REPORT("report",
            Duration.ofMinutes(10),
            Duration.ofHours(24),
            10 * 1024 * 1024L,
            Set.of(AcceptableFileType.PDF)),

    // CS 문의 첨부파일 - 스크린샷/문서 첨부를 고려해 이미지+PDF 허용
    INQUIRY("inquiry",
            Duration.ofMinutes(10),
            Duration.ofHours(24),
            10 * 1024 * 1024L,
            Set.of(AcceptableFileType.JPG,
                   AcceptableFileType.JPEG,
                   AcceptableFileType.PNG,
                   AcceptableFileType.PDF));

    private final String prefix;
    private final Duration uploadTime;  //  presigned-url 만료시간 작성
    private final Duration downloadTime;
    private final long fileSize;
    private final Set<AcceptableFileType> allowedTypes;

    public boolean allows(AcceptableFileType type) {
        return allowedTypes.contains(type);
    }

    public static FileDirectory of(String inputPrefix) {
        if (inputPrefix == null || inputPrefix.isEmpty())
            throw new ExtensionEmptyException();


        return Arrays.stream(values())
                .filter(dir -> dir.prefix.equalsIgnoreCase(inputPrefix))
                .findFirst()
                .orElseThrow(InvalidDirectoryException::new);

    }
}
