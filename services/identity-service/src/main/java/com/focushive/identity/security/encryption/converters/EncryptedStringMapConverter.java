package com.focushive.identity.security.encryption.converters;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.focushive.identity.security.encryption.EncryptionService;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

/**
 * JPA converter for encrypting Map<String, String> fields that may contain PII.
 * Used for custom attributes and metadata that might contain sensitive information.
 */
@Converter
@RequiredArgsConstructor
@Slf4j
public class EncryptedStringMapConverter implements AttributeConverter<Map<String, String>, String> {
    
    @Autowired
    private EncryptionService encryptionService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    private static final TypeReference<Map<String, String>> STRING_MAP_TYPE = new TypeReference<Map<String, String>>() {};
    
    @Override
    public String convertToDatabaseColumn(Map<String, String> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return null;
        }
        
        try {
            // Serialize to JSON
            String json = objectMapper.writeValueAsString(attribute);
            
            // Encrypt the JSON
            return encryptionService.encrypt(json);
            
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize string map to JSON", e);
            throw new RuntimeException("JSON serialization failed for string map", e);
        } catch (Exception e) {
            log.error("Failed to encrypt string map field", e);
            throw new RuntimeException("Encryption failed for string map field", e);
        }
    }
    
    @Override
    public Map<String, String> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return null;
        }
        
        try {
            String json;
            
            // Check if data is encrypted (has version prefix)
            if (dbData.contains(":")) {
                // Decrypt first
                json = encryptionService.decrypt(dbData);
            } else {
                // Data is not encrypted (legacy data)
                log.debug("Found unencrypted string map data in database, parsing directly");
                json = dbData;
            }
            
            // Deserialize from JSON
            return objectMapper.readValue(json, STRING_MAP_TYPE);
            
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize JSON to string map", e);
            throw new RuntimeException("JSON deserialization failed for string map", e);
        } catch (Exception e) {
            log.error("Failed to decrypt string map field", e);
            throw new RuntimeException("Decryption failed for string map field", e);
        }
    }
}