package com.aivle.sellon.rawdb.entity.converter;

import com.aivle.sellon.domain.channels.enums.InquiryType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

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
