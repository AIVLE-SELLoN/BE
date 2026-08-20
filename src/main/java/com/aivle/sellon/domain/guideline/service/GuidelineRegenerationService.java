package com.aivle.sellon.domain.guideline.service;

import com.aivle.sellon.domain.guideline.entity.Guideline;
import com.aivle.sellon.domain.guideline.entity.PdfS3Meta;
import com.aivle.sellon.domain.guideline.exception.GuidelineDownloadUnavailableException;
import com.aivle.sellon.domain.guideline.exception.GuidelineNotFoundException;
import com.aivle.sellon.domain.guideline.exception.GuidelinePdfGenerationException;
import com.aivle.sellon.domain.guideline.repository.GuidelineRepository;
import com.aivle.sellon.global.file.service.ReportS3UploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.UUID;

/**
 * 보관 기한이 지나 S3에서 삭제된 가이드라인 PDF를 저장된 source_payload로 다시 만들어 올린다.
 * 새로 올린 파일도 같은 lifecycle을 따르므로 기한이 지나면 다시 재생성 대상이 된다.
 * <p>
 * PDF 렌더링(CPU)과 S3 업로드(네트워크)는 트랜잭션·행 락 없이 처리하고, DB에 반영하는 마지막 단계만
 * {@link GuidelineRegenerationWriter}에게 맡겨 짧은 트랜잭션으로 끝낸다. 그렇지 않으면 이 느린 작업 내내
 * PESSIMISTIC_WRITE 락과 DB 커넥션이 묶여서, 같은 배치로 함께 만료된 가이드라인 여러 개가 동시에
 * 다운로드될 때 커넥션 풀이 고갈될 수 있다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GuidelineRegenerationService {

    private static final String FILE_NAME_FALLBACK_PREFIX = "cs-guideline";
    private static final String PDF_EXTENSION = ".pdf";

    private final GuidelineRepository guidelineRepository;
    private final GuidelinePdfGenerator pdfGenerator;
    private final GuidelineDownloadUrlService guidelineDownloadUrlService;
    private final ReportS3UploadService reportS3UploadService;
    private final GuidelineRegenerationWriter writer;
    private final JsonMapper jsonMapper;

    @Value("${guideline.file.retention}")
    private long fileRetentionMs;

    /**
     * @return 재생성 후의 파일 메타. 대기 중 다른 요청이 먼저 재생성했다면 그 결과를 그대로 돌려준다.
     */
    public PdfS3Meta regenerate(Long id) {
        Guideline guideline = guidelineRepository.findById(id)
                .orElseThrow(GuidelineNotFoundException::new);

        PdfS3Meta meta = guideline.getPdfS3Meta();

        // 파일이 올라간 적 없으면 어느 폴더에 어떤 이름으로 되살릴지 알 수 없다
        if (meta == null || meta.getS3FullKey() == null)
            throw new GuidelineDownloadUnavailableException();

        if (guidelineDownloadUrlService.isAvailable(meta))
            return meta;

        JsonNode sourcePayload = readSourcePayload(guideline);
        byte[] pdf = pdfGenerator.generate(guideline.getGuidelineId(),
                requireNode(guideline, sourcePayload, "input"), requireNode(guideline, sourcePayload, "output"));

        String newFileName = buildNewFileName(meta);
        String s3FullKey = folderOf(meta) + newFileName;
        uploadPdf(guideline, s3FullKey, pdf);

        return writer.persist(id, newFileName, s3FullKey, pdf.length, fileRetentionMs);
    }

    private void uploadPdf(Guideline guideline, String s3FullKey, byte[] pdf) {
        try {
            reportS3UploadService.uploadPdf(s3FullKey, pdf);
        } catch (Exception e) {
            log.error("가이드라인 재생성 실패 - S3 업로드 실패. guidelineId={}, key={}",
                    guideline.getGuidelineId(), s3FullKey, e);
            throw new GuidelinePdfGenerationException();
        }
    }

    private JsonNode readSourcePayload(Guideline guideline) {
        try {
            return jsonMapper.readTree(guideline.getSourcePayload());
        } catch (Exception e) {
            log.error("가이드라인 재생성 실패 - source_payload를 읽을 수 없음. guidelineId={}",
                    guideline.getGuidelineId(), e);
            throw new GuidelinePdfGenerationException();
        }
    }

    /** 템플릿이 input/output을 모두 쓰므로, 둘 중 하나라도 없으면 되살릴 내용 자체가 없다. */
    private JsonNode requireNode(Guideline guideline, JsonNode sourcePayload, String field) {
        JsonNode node = sourcePayload.path(field);
        if (node.isMissingNode() || node.isNull()) {
            log.error("가이드라인 재생성 실패 - source_payload.{} 없음. guidelineId={}", field, guideline.getGuidelineId());
            throw new GuidelinePdfGenerationException();
        }

        return node;
    }

    /** 원본이 올라갔던 폴더에 그대로 올린다. 폴더에는 생성 연월이 들어 있어 시점이 섞이지 않는다. */
    private String folderOf(PdfS3Meta meta) {
        String filePath = meta.getS3FilePath();
        if (filePath != null && !filePath.isBlank())
            return filePath.endsWith("/") ? filePath : filePath + "/";

        String key = meta.getS3FullKey();
        return key.substring(0, key.lastIndexOf('/') + 1);
    }

    /** 이전 객체명의 접두어(cs-guideline_{yyyyMM})는 유지하고 뒤의 UUID만 새로 붙인다. */
    private String buildNewFileName(PdfS3Meta meta) {
        return "%s_%s%s".formatted(prefixOf(meta.getNewFileName()), UUID.randomUUID(), PDF_EXTENSION);
    }

    private String prefixOf(String previousFileName) {
        if (previousFileName == null)
            return FILE_NAME_FALLBACK_PREFIX;

        String base = previousFileName.endsWith(PDF_EXTENSION)
                ? previousFileName.substring(0, previousFileName.length() - PDF_EXTENSION.length())
                : previousFileName;

        int lastSeparator = base.lastIndexOf('_');
        return lastSeparator > 0 ? base.substring(0, lastSeparator) : FILE_NAME_FALLBACK_PREFIX;
    }
}
