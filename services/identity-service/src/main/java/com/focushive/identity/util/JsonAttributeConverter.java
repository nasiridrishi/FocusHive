package com.focushive.identity.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.postgresql.util.PGobject;
import org.springframework.stereotype.Component;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * JPA converter for storing Map<String, Object> as JSONB in PostgreSQL.
 * Handles conversion between Java Map and PostgreSQL JSONB type.
 */
@Slf4j
@Component
@Converter(autoApply = false)
@RequiredArgsConstructor
public class JsonAttributeConverter implements AttributeConverter<Map<String, Object>, PGobject> {

    private final ObjectMapper objectMapper;

    @Override
    public PGobject convertToDatabaseColumn(Map<String, Object> attribute) {
        if (attribute == null) {
            return null;
        }

        try {
            PGobject pgObject = new PGobject();
            pgObject.setType("jsonb");
            pgObject.setValue(objectMapper.writeValueAsString(attribute));
            return pgObject;
        } catch (JsonProcessingException | SQLException e) {
            log.error("Failed to convert Map to JSONB: {}", e.getMessage());
            throw new IllegalArgumentException("Error converting Map to JSONB", e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> convertToEntityAttribute(PGobject dbData) {
        if (dbData == null || dbData.getValue() == null) {
            return new HashMap<>();
        }

        try {
            return objectMapper.readValue(dbData.getValue(), Map.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to convert JSONB to Map: {}", e.getMessage());
            return new HashMap<>();
        }
    }
}