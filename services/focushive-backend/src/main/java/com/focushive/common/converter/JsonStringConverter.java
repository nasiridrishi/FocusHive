package com.focushive.common.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JPA Attribute Converter for handling JSON string serialization/deserialization.
 * 
 * This converter provides a generic way to convert Java objects to/from JSON strings
 * for database storage, supporting both PostgreSQL JSONB and H2 TEXT columns.
 * 
 * Usage: Apply to any field that needs JSON serialization with @Convert(converter = JsonStringConverter.class)
 */
@Converter
public class JsonStringConverter implements AttributeConverter<Object, String> {

    private static final Logger logger = LoggerFactory.getLogger(JsonStringConverter.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(Object attribute) {
        if (attribute == null) {
            return null;
        }
        
        try {
            return objectMapper.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            logger.error("Error converting object to JSON: {}", e.getMessage(), e);
            throw new RuntimeException("Error converting object to JSON", e);
        }
    }

    @Override
    public Object convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty()) {
            return null;
        }
        
        try {
            return objectMapper.readValue(dbData, Object.class);
        } catch (JsonProcessingException e) {
            logger.error("Error converting JSON to object: {}", e.getMessage(), e);
            throw new RuntimeException("Error converting JSON to object", e);
        }
    }
}