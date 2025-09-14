package com.focushive.identity.security.encryption.converters;

import com.focushive.identity.security.encryption.IEncryptionService;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * JPA converter for encrypting string fields that need to be searchable.
 * This converter only handles the encrypted value - the hash field must be
 * managed separately using @PostLoad, @PrePersist, @PreUpdate lifecycle methods.
 * 
 * Use this for fields like email where you need to search but also encrypt.
 */
@Converter
@RequiredArgsConstructor
@Slf4j
public class SearchableEncryptedStringConverter implements AttributeConverter<String, String> {
    
    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return attribute;
        }
        
        IEncryptionService encryptionService = SpringContextUtil.getEncryptionService();
        if (encryptionService == null) {
            log.warn("EncryptionService not available, storing plaintext (test mode)");
            return attribute;
        }
        
        try {
            return encryptionService.encrypt(attribute);
        } catch (Exception e) {
            log.error("Failed to encrypt searchable string field", e);
            throw new RuntimeException("Encryption failed for searchable string field", e);
        }
    }
    
    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return dbData;
        }
        
        IEncryptionService encryptionService = SpringContextUtil.getEncryptionService();
        if (encryptionService == null) {
            log.warn("EncryptionService not available, returning data as-is (test mode)");
            return dbData;
        }
        
        // Check if data is already encrypted (has version prefix)
        if (!dbData.contains(":")) {
            // Data is not encrypted (legacy data or test data)
            log.debug("Found unencrypted searchable data in database, returning as-is");
            return dbData;
        }
        
        try {
            return encryptionService.decrypt(dbData);
        } catch (Exception e) {
            log.error("Failed to decrypt searchable string field", e);
            throw new RuntimeException("Decryption failed for searchable string field", e);
        }
    }
}