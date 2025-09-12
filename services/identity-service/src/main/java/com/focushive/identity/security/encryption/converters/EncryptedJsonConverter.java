package com.focushive.identity.security.encryption.converters;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.focushive.identity.security.encryption.EncryptionService;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

/**
 * JPA converter for encrypting JSON/Map fields that may contain PII.
 * Serializes the map to JSON, encrypts it, and reverses the process on retrieval.
 */
@Converter
@RequiredArgsConstructor
@Slf4j
public class EncryptedJsonConverter implements AttributeConverter<Map<String, Object>, String> {
    
    @Autowired
    private EncryptionService encryptionService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Override
    public String convertToDatabaseColumn(Map<String, Object> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return null;
        }
        
        try {
            // Serialize to JSON
            String json = objectMapper.writeValueAsString(attribute);
            
            // Encrypt the JSON
            return encryptionService.encrypt(json);
            
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize map to JSON", e);
            throw new RuntimeException("JSON serialization failed", e);
        } catch (Exception e) {
            log.error("Failed to encrypt JSON field", e);
            throw new RuntimeException("Encryption failed for JSON field", e);
        }
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> convertToEntityAttribute(String dbData) {
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
                log.debug("Found unencrypted JSON data in database, parsing directly");
                json = dbData;
            }
            
            // Deserialize from JSON
            return objectMapper.readValue(json, Map.class);
            
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize JSON to map", e);
            throw new RuntimeException("JSON deserialization failed", e);
        } catch (Exception e) {
            log.error("Failed to decrypt JSON field", e);
            throw new RuntimeException("Decryption failed for JSON field", e);
        }
    }
}