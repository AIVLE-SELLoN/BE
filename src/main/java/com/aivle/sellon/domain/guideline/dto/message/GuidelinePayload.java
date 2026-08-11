package com.aivle.sellon.domain.guideline.dto.message;

import com.aivle.sellon.domain.report.enums.ReportStatus;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

// status는 월간 리포트와 같은 5종 enum을 그대로 쓴다(§4.2·§4.6 공용, 노션 "CallbackStatus").
// 다만 HOLD_INSUFFICIENT_DATA는 가이드라인 이벤트에서는 실제로 오지 않는다.
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record GuidelinePayload(
        String guidelineId,
        String alertId,
        ReportStatus status,
        PdfS3MetaPayload pdfS3Meta,
        JsonNode sourcePayload,
        String noticeMessage,
        JsonNode validationReport
) {
}
