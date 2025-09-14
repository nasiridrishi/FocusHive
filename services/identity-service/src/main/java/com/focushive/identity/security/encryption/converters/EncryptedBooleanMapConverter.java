package com.focushive.identity.security.encryption.converters;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.focushive.identity.security.encryption.IEncryptionService;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * JPA converter for encrypting Map<String, Boolean> fields like notification preferences
 * that may reveal personal information about user habits and preferences.
 */
@Converter
@RequiredArgsConstructor
@Slf4j
public class EncryptedBooleanMapConverter implements AttributeConverter<Map<String, Boolean>, String> {
    
    private ObjectMapper objectMapper = new ObjectMapper();
    
    private static final TypeReference<Map<String, Boolean>> BOOLEAN_MAP_TYPE = new TypeReference<Map<String, Boolean>>() {};
    
    @Override
    public String convertToDatabaseColumn(Map<String, Boolean> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return null;
        }
        
        try {
            // Serialize to JSON
            String json = objectMapper.writeValueAsString(attribute);
            
            IEncryptionService encryptionService = SpringContextUtil.getEncryptionService();
            if (encryptionService == null) {
                log.warn("EncryptionService not available, storing plaintext JSON (test mode)");
                return json;
            }
            
            // Encrypt the JSON
            return encryptionService.encrypt(json);
            
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize boolean map to JSON", e);
            throw new RuntimeException("JSON serialization failed for boolean map", e);
        } catch (Exception e) {
            log.error("Failed to encrypt boolean map field", e);
            throw new RuntimeException("Encryption failed for boolean map field", e);
        }
    }
    
    @Override
    public Map<String, Boolean> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return null;
        }
        
        try {
            String json;
            
            IEncryptionService encryptionService = SpringContextUtil.getEncryptionService();
            if (encryptionService == null) {
                log.warn("EncryptionService not available, parsing data as-is (test mode)");
                json = dbData;
            } else {
                // Check if data is encrypted (has version prefix)
                if (dbData.contains(":")) {
                    // Decrypt first
                    json = encryptionService.decrypt(dbData);
                } else {
                    // Data is not encrypted (legacy data)
                    log.debug("Found unencrypted boolean map data in database, parsing directly");
                    json = dbData;
                }
            }
            
            // Deserialize from JSON
            return objectMapper.readValue(json, BOOLEAN_MAP_TYPE);
            
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize JSON to boolean map", e);
            throw new RuntimeException("JSON deserialization failed for boolean map", e);
        } catch (Exception e) {
            log.error("Failed to decrypt boolean map field", e);
            throw new RuntimeException("Decryption failed for boolean map field", e);
        }
    }
}