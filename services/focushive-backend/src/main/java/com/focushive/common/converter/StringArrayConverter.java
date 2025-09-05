package com.focushive.common.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

/**
 * JPA Attribute Converter for String arrays to handle PostgreSQL text[] and H2 compatibility.
 * 
 * This converter serializes String arrays as JSON for database storage, making them compatible
 * with both PostgreSQL (where they could be stored as JSONB/TEXT) and H2 (stored as TEXT).
 * 
 * Usage: Apply to String[] fields with @Convert(converter = StringArrayConverter.class)
 */
@Converter
public class StringArrayConverter implements AttributeConverter<String[], String> {

    private static final Logger logger = LoggerFactory.getLogger(StringArrayConverter.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final TypeReference<List<String>> typeReference = new TypeReference<List<String>>() {};

    @Override
    public String convertToDatabaseColumn(String[] attribute) {
        if (attribute == null) {
            return null;
        }
        
        try {
            // Convert array to list and then to JSON string
            List<String> list = Arrays.asList(attribute);
            return objectMapper.writeValueAsString(list);
        } catch (JsonProcessingException e) {
            logger.error("Error converting string array to JSON: {}", e.getMessage(), e);
            throw new RuntimeException("Error converting string array to JSON", e);
        }
    }

    @Override
    public String[] convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty()) {
            return null;
        }
        
        try {
            // Convert JSON string back to list and then to array
            List<String> list = objectMapper.readValue(dbData, typeReference);
            return list.toArray(new String[0]);
        } catch (JsonProcessingException e) {
            logger.error("Error converting JSON to string array: {}", e.getMessage(), e);
            throw new RuntimeException("Error converting JSON to string array", e);
        }
    }
}