package com.aivle.sellon.rawdb.entity.converter;

import com.aivle.sellon.domain.channels.enums.InquiryType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * classified_item_aspect.aspect 컬럼은 AI 노드가 한글 라벨("색상"/"사이즈"/...)을 그대로
 * 저장한다(app.core.schemas.Aspect가 str Enum이라 값 자체가 한글). @Enumerated(STRING)을
 * 쓰면 Java enum 이름(COLOR 등)이 저장되어 어긋나므로 라벨 기준으로 직접 변환한다.
 */
@Converter
public class InquiryTypeConverter implements AttributeConverter<InquiryType, String> {

    @Override
    public String convertToDatabaseColumn(InquiryType attribute) {
        return attribute == null ? null : attribute.getLabel();
    }

    @Override
    public InquiryType convertToEntityAttribute(String dbData) {
        return dbData == null ? null : InquiryType.fromLabel(dbData);
    }
}
