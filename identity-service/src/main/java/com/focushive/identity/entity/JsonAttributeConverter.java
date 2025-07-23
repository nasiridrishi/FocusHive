package com.focushive.identity.entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * JPA converter for storing Map as JSON in PostgreSQL jsonb columns.
 */
@Slf4j
@Component
@Converter
@RequiredArgsConstructor
public class JsonAttributeConverter implements AttributeConverter<Map<String, Boolean>, String> {
    
    private final ObjectMapper objectMapper;
    
    @Override
    public String convertToDatabaseColumn(Map<String, Boolean> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return "{}";
        }
        
        try {
            return objectMapper.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            log.error("Error converting map to JSON", e);
            return "{}";
        }
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Boolean> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return new HashMap<>();
        }
        
        try {
            return objectMapper.readValue(dbData, Map.class);
        } catch (JsonProcessingException e) {
            log.error("Error converting JSON to map", e);
            return new HashMap<>();
        }
    }
}