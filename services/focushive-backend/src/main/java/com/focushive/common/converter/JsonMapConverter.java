package com.focushive.common.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * JPA Attribute Converter for Map<String, Object> to handle PostgreSQL JSONB and H2 compatibility.
 * 
 * This converter handles the serialization and deserialization of Map objects for JSONB columns.
 * Works with both PostgreSQL JSONB columns and H2 TEXT columns.
 * 
 * Usage: Apply to Map<String, Object> fields with @Convert(converter = JsonMapConverter.class)
 */
@Converter
public class JsonMapConverter implements AttributeConverter<Map<String, Object>, String> {

    private static final Logger logger = LoggerFactory.getLogger(JsonMapConverter.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final TypeReference<Map<String, Object>> typeReference = 
            new TypeReference<Map<String, Object>>() {};

    @Override
    public String convertToDatabaseColumn(Map<String, Object> attribute) {
        if (attribute == null) {
            return null;
        }
        
        try {
            return objectMapper.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            logger.error("Error converting map to JSON: {}", e.getMessage(), e);
            throw new RuntimeException("Error converting map to JSON", e);
        }
    }

    @Override
    public Map<String, Object> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty()) {
            return new HashMap<>(); // Return empty map for null/empty JSON
        }
        
        try {
            return objectMapper.readValue(dbData, typeReference);
        } catch (JsonProcessingException e) {
            logger.error("Error converting JSON to map: {}", e.getMessage(), e);
            throw new RuntimeException("Error converting JSON to map", e);
        }
    }
}