package com.aivle.sellon.domain.guideline.service;

import com.aivle.sellon.domain.guideline.exception.GuidelinePdfGenerationException;
import com.openhtmltopdf.extend.FSSupplier;
import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder.FontStyle;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Map;

/**
 * 보관 기한이 지나 S3에서 삭제된 가이드라인 PDF를, 저장해 둔 source_payload로 다시 만든다.
 * AI 팀이 실제 렌더링에 쓰는 HTML/CSS 템플릿(resources/templates/guideline/cs_guideline.html,
 * 원래는 WeasyPrint용 Jinja2)을 Thymeleaf로 옮겨 그대로 쓴다 — 레이아웃을 별도로 다시 설계하지 않는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GuidelinePdfGenerator {

    private static final String TEMPLATE_NAME = "guideline/cs_guideline";

    // resources/fonts에 있는 실제 TTF. OpenPDF의 내장 CID 폰트와 달리 HTML 렌더러는 폰트를 직접 등록해야 한다
    private static final String FONT_FAMILY = "Noto Sans KR";
    private static final String FONT_REGULAR_PATH = "/fonts/NotoSansKR-Regular.ttf";
    private static final String FONT_BOLD_PATH = "/fonts/NotoSansKR-Bold.ttf";

    private final JsonMapper jsonMapper;
    private final TemplateEngine templateEngine = buildTemplateEngine();

    private static TemplateEngine buildTemplateEngine() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");

        TemplateEngine engine = new TemplateEngine();
        engine.setTemplateResolver(resolver);
        return engine;
    }

    /**
     * @param input  source_payload.input — 템플릿의 {@code input.*}
     * @param output source_payload.output — 템플릿의 {@code guideline.*}
     * @return PDF 바이트
     */
    public byte[] generate(String guidelineId, JsonNode input, JsonNode output) {
        try {
            String html = render(input, output);
            return toPdf(html);
        } catch (Exception e) {
            log.error("가이드라인 PDF 재생성 실패. guidelineId={}", guidelineId, e);
            throw new GuidelinePdfGenerationException();
        }
    }

    private String render(JsonNode input, JsonNode output) {
        Context context = new Context();
        context.setVariable("input", toMap(input));
        context.setVariable("guideline", toMap(output));
        return templateEngine.process(TEMPLATE_NAME, context);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> toMap(JsonNode node) {
        return jsonMapper.convertValue(node, Map.class);
    }

    private byte[] toPdf(String html) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        PdfRendererBuilder builder = new PdfRendererBuilder();
        builder.useFont(fontSupplier(FONT_REGULAR_PATH), FONT_FAMILY, 400, FontStyle.NORMAL, true);
        builder.useFont(fontSupplier(FONT_BOLD_PATH), FONT_FAMILY, 700, FontStyle.NORMAL, true);
        builder.withHtmlContent(html, null);
        builder.toStream(out);
        builder.run();

        return out.toByteArray();
    }

    private FSSupplier<InputStream> fontSupplier(String classpathLocation) {
        return () -> {
            try {
                return new ClassPathResource(classpathLocation).getInputStream();
            } catch (IOException e) {
                throw new UncheckedIOException("폰트 리소스를 열 수 없음: " + classpathLocation, e);
            }
        };
    }
}
