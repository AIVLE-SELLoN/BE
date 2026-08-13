package com.aivle.sellon.domain.guideline.service;

import com.aivle.sellon.domain.guideline.entity.Guideline;
import com.aivle.sellon.domain.guideline.entity.PdfS3Meta;
import com.aivle.sellon.domain.guideline.exception.GuidelineNotFoundException;
import com.aivle.sellon.domain.guideline.repository.GuidelineRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * {@link GuidelineRegenerationService}가 새로 만든 PDF를 DB에 반영하는 부분만 맡는다.
 * PDF 렌더링·S3 업로드처럼 느린 작업은 이 트랜잭션 밖(호출부)에서 끝내고 결과만 여기로 들고 와야
 * 행 락(PESSIMISTIC_WRITE)이 DB 커넥션을 오래 붙잡지 않는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
class GuidelineRegenerationWriter {

    private final GuidelineRepository guidelineRepository;
    private final GuidelineDownloadUrlService guidelineDownloadUrlService;

    @Transactional
    PdfS3Meta persist(Long id, String newFileName, String s3FullKey, long fileSizeBytes, long fileRetentionMs) {
        Guideline guideline = guidelineRepository.findByIdForUpdate(id)
                .orElseThrow(GuidelineNotFoundException::new);

        PdfS3Meta meta = guideline.getPdfS3Meta();

        // 우리가 렌더링·업로드하는 동안 다른 요청이 먼저 끝냈으면 그 결과를 쓴다.
        // 우리가 올린 S3 객체는 이제 아무도 참조하지 않는 채로 남지만, 원본과 같은 보관 기한(7일)이 지나면
        // lifecycle 규칙이 알아서 지워주므로 별도 정리는 하지 않는다.
        if (guidelineDownloadUrlService.isAvailable(meta)) {
            log.info("가이드라인 파일 재생성 결과 폐기 - 다른 요청이 먼저 처리함. guidelineId={}", guideline.getGuidelineId());
            return meta;
        }

        LocalDateTime now = LocalDateTime.now();
        PdfS3Meta regenerated = meta.withRegeneratedFile(
                newFileName, s3FullKey, fileSizeBytes, now, now.plus(Duration.ofMillis(fileRetentionMs)));
        guideline.replacePdfS3Meta(regenerated);

        log.info("가이드라인 파일 재생성 완료. guidelineId={}, key={}, size={}",
                guideline.getGuidelineId(), s3FullKey, fileSizeBytes);

        return regenerated;
    }
}
