package com.aivle.sellon.domain.guideline.service;

import com.aivle.sellon.domain.guideline.exception.GuidelinePdfGenerationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GuidelinePdfGeneratorTest {

    private static final String FULL_INPUT = """
            {
              "product_group_id": "P001",
              "product_name": "미디 원피스",
              "channel": "COUPANG",
              "main_aspect": "색상"
            }
            """;

    private static final String FULL_OUTPUT = """
            {
              "guideline_id": "GD-20260528-P001-COUPANG",
              "alert_id": "ALT-20260528-P001-COUPANG",
              "summary": {
                "risk_level": "WARNING",
                "issue_title": "쿠팡 색상 불만 급증 대응 가이드",
                "key_metric_text": "색상 부정 비율이 5%에서 13%로 8%p 상승했습니다."
              },
              "root_cause_summary": "사진_색감_오차 18건 / 전체 26건 (69%)",
              "ops_action_guide": "쿠팡 대표 이미지의 색보정 상태를 점검하세요.",
              "standard_guideline": {
                "core_message": "촬영 조명 차이로 실물 색상이 다르게 보일 수 있음을 안내합니다.",
                "key_talking_points": ["조명 차이 정중히 안내", "고객 과실 암시 표현 금지"],
                "draft_reply": "안녕하세요 고객님, 색상 차이로 불편을 드려 죄송합니다."
              },
              "inquiry_specific_guides": [
                {"item_id": "INQ-000412", "recommended_point": "사과 후 무상 회수 접수를 우선 안내하세요."}
              ]
            }
            """;

    private final JsonMapper jsonMapper = JsonMapper.builder().build();
    private final GuidelinePdfGenerator generator = new GuidelinePdfGenerator(jsonMapper);

    @Test
    @DisplayName("AI 팀 템플릿(input+output)으로 유효한 PDF를 생성한다")
    void generatesValidPdf() {
        byte[] pdf = generator.generate("GD-20260528-P001-COUPANG", node(FULL_INPUT), node(FULL_OUTPUT));

        assertTrue(new String(pdf, 0, 5, StandardCharsets.US_ASCII).startsWith("%PDF"), "PDF 시그니처가 아니다");
        assertTrue(pdf.length > 1000, "본문이 비어 있는 것으로 보인다. length=" + pdf.length);
    }

    @Test
    @DisplayName("한글 렌더링을 위해 임베드한 Noto Sans KR 폰트를 사용한다")
    void embedsKoreanFont() {
        byte[] pdf = generator.generate("GD-1", node(FULL_INPUT), node(FULL_OUTPUT));

        // 서브셋 임베딩되면 "ABCDEF+NotoSansKR" 형태로 이름이 남는다. 폰트가 아예 안 실리면 이 substring이 없다
        assertTrue(new String(pdf, StandardCharsets.ISO_8859_1).contains("NotoSansKR"),
                "Noto Sans KR 폰트가 문서에 임베드되지 않았다");
    }

    @Test
    @DisplayName("product_name이 없으면(선택 필드) 괄호 없이 렌더링될 뿐 실패하지 않는다")
    void toleratesMissingOptionalProductName() {
        String inputWithoutProductName = """
                {"product_group_id": "P001", "channel": "COUPANG", "main_aspect": "색상"}
                """;

        assertDoesNotThrow(() -> generator.generate("GD-1", node(inputWithoutProductName), node(FULL_OUTPUT)));
    }

    @Test
    @DisplayName("output의 필수 필드(예: standard_guideline)가 통째로 없으면 재생성 실패로 처리한다")
    void failsWhenRequiredSectionMissing() {
        String outputWithoutStandardGuideline = """
                {"guideline_id": "GD-1", "alert_id": "ALT-1",
                 "summary": {"issue_title": "t", "risk_level": "WARNING", "key_metric_text": "m"},
                 "root_cause_summary": "r", "ops_action_guide": "o", "inquiry_specific_guides": []}
                """;

        assertThrows(GuidelinePdfGenerationException.class,
                () -> generator.generate("GD-1", node(FULL_INPUT), node(outputWithoutStandardGuideline)));
    }

    @Test
    @DisplayName("output이 null이면 재생성 실패로 처리한다")
    void failsOnNullOutput() {
        assertThrows(GuidelinePdfGenerationException.class,
                () -> generator.generate("GD-1", node(FULL_INPUT), null));
    }

    private JsonNode node(String json) {
        return jsonMapper.readTree(json);
    }
}
