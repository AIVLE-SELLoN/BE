package com.aivle.sellon.global.mq.config;

import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.module.SimpleModule;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;

/**
 * AI 서비스가 payload 안 시각 필드에 오프셋을 붙이거나 안 붙이거나 일관되지 않게 보낸다
 * (원본 파이썬 datetime이 그 시점에 timezone-aware였는지 naive였는지에 따라 갈림 - 계약 문서가
 * envelope의 occurredAt 표기만 규정하고 payload 내부 시각 표기는 규정하지 않아서 생긴 공백).
 * <p>
 * 필드마다 타입을 맞춰가며 두더지잡기 하는 대신, 오프셋이 없으면 UTC로 간주해서 파싱하는
 * 관대한 디시리얼라이저를 OffsetDateTime/Instant 전체에 전역으로 적용한다.
 */
public class LenientDateTimeModule extends SimpleModule {

    public LenientDateTimeModule() {
        addDeserializer(OffsetDateTime.class, new LenientOffsetDateTimeDeserializer());
        addDeserializer(Instant.class, new LenientInstantDeserializer());
    }

    private static class LenientOffsetDateTimeDeserializer extends ValueDeserializer<OffsetDateTime> {
        @Override
        public OffsetDateTime deserialize(JsonParser p, DeserializationContext ctxt) {
            String text = p.getString();
            if (text == null || text.isBlank())
                return null;

            try {
                return OffsetDateTime.parse(text);
            } catch (DateTimeParseException e) {
                return LocalDateTime.parse(text).atOffset(ZoneOffset.UTC);
            }
        }
    }

    private static class LenientInstantDeserializer extends ValueDeserializer<Instant> {
        @Override
        public Instant deserialize(JsonParser p, DeserializationContext ctxt) {
            String text = p.getString();
            if (text == null || text.isBlank())
                return null;

            try {
                return Instant.parse(text);
            } catch (DateTimeParseException e) {
                return LocalDateTime.parse(text).toInstant(ZoneOffset.UTC);
            }
        }
    }
}
