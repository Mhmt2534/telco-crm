package com.telcocrm.common.persistence.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class PiiEncryptionConverter implements AttributeConverter<String, String> {

    @Override
    public String convertToDatabaseColumn(String attribute) {
        throw new UnsupportedOperationException("PII encryption not yet implemented");
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        throw new UnsupportedOperationException("PII decryption not yet implemented");
    }
}
