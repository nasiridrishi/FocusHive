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
 * JPA Attribute Converter for Map<Long, Integer> to handle PostgreSQL JSONB and H2 compatibility.
 * 
 * This converter handles the serialization and deserialization of Map<Long, Integer> objects
 * for JSONB columns, commonly used for goal progress tracking in buddy system.
 * 
 * Usage: Apply to Map<Long, Integer> fields with @Convert(converter = JsonIntegerMapConverter.class)
 */
@Converter
public class JsonIntegerMapConverter implements AttributeConverter<Map<Long, Integer>, String> {

    private static final Logger logger = LoggerFactory.getLogger(JsonIntegerMapConverter.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final TypeReference<Map<Long, Integer>> typeReference = 
            new TypeReference<Map<Long, Integer>>() {};

    @Override
    public String convertToDatabaseColumn(Map<Long, Integer> attribute) {
        if (attribute == null) {
            return null;
        }
        
        try {
            return objectMapper.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            logger.error("Error converting Long-Integer map to JSON: {}", e.getMessage(), e);
            throw new RuntimeException("Error converting Long-Integer map to JSON", e);
        }
    }

    @Override
    public Map<Long, Integer> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty()) {
            return new HashMap<>(); // Return empty map for null/empty JSON
        }
        
        try {
            return objectMapper.readValue(dbData, typeReference);
        } catch (JsonProcessingException e) {
            logger.error("Error converting JSON to Long-Integer map: {}", e.getMessage(), e);
            throw new RuntimeException("Error converting JSON to Long-Integer map", e);
        }
    }
}