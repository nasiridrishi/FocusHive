package com.focushive.buddy.entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * JPA Attribute Converter for mapping Map<String, WorkHours> to/from JSON in PostgreSQL JSONB column.
 * 
 * This converter handles the serialization and deserialization of work hours maps for buddy preferences.
 * The JSON structure is: {"MONDAY": {"startHour": 9, "endHour": 17}, ...}
 */
@Converter
public class WorkHoursMapConverter implements AttributeConverter<Map<String, BuddyPreferences.WorkHours>, String> {

    private static final Logger logger = LoggerFactory.getLogger(WorkHoursMapConverter.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final TypeReference<Map<String, BuddyPreferences.WorkHours>> typeReference = 
            new TypeReference<Map<String, BuddyPreferences.WorkHours>>() {};

    @Override
    public String convertToDatabaseColumn(Map<String, BuddyPreferences.WorkHours> attribute) {
        if (attribute == null) {
            return null;
        }
        
        try {
            return objectMapper.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            logger.error("Error converting work hours map to JSON: {}", e.getMessage(), e);
            throw new RuntimeException("Error converting work hours map to JSON", e);
        }
    }

    @Override
    public Map<String, BuddyPreferences.WorkHours> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty()) {
            return null;
        }
        
        try {
            return objectMapper.readValue(dbData, typeReference);
        } catch (JsonProcessingException e) {
            logger.error("Error converting JSON to work hours map: {}", e.getMessage(), e);
            throw new RuntimeException("Error converting JSON to work hours map", e);
        }
    }
}