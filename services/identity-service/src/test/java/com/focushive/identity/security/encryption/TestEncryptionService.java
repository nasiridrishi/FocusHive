package com.focushive.identity.security.encryption;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * Test implementation of EncryptionService for repository tests.
 * 
 * This is a no-op encryption service that passes data through unchanged
 * for testing purposes. In repository tests, we're testing data persistence
 * and retrieval, not encryption functionality.
 */
@Service
@Profile("test")
public class TestEncryptionService implements IEncryptionService {
    
    @Override
    public String encrypt(String plaintext) {
        // For test purposes, return plaintext with a test prefix
        if (plaintext == null || plaintext.isEmpty()) {
            return plaintext;
        }
        return "test:" + plaintext;
    }
    
    @Override
    public String decrypt(String encryptedText) {
        // For test purposes, remove the test prefix
        if (encryptedText == null || encryptedText.isEmpty()) {
            return encryptedText;
        }
        
        // Handle simple test format: "test:data"
        if (encryptedText.startsWith("test:")) {
            return encryptedText.substring(5);
        }
        
        // Return as-is for unencrypted data
        return encryptedText;
    }
    
    @Override
    public String createSearchableHash(String value) {
        return hash(value);
    }
    
    @Override
    public String hash(String data) {
        // For test purposes, return a simple hash
        if (data == null || data.isEmpty()) {
            return data;
        }
        return "hash_" + data.hashCode();
    }
    
    @Override
    public boolean verifyHash(String value, String hash) {
        if (value == null || hash == null) {
            return value == null && hash == null;
        }
        String computedHash = hash(value);
        return hash.equals(computedHash);
    }
    
    @Override
    public String[] encryptBatch(String[] plaintextArray) {
        if (plaintextArray == null) {
            return null;
        }
        
        String[] result = new String[plaintextArray.length];
        for (int i = 0; i < plaintextArray.length; i++) {
            result[i] = encrypt(plaintextArray[i]);
        }
        return result;
    }
    
    @Override
    public String[] decryptBatch(String[] encryptedArray) {
        if (encryptedArray == null) {
            return null;
        }
        
        String[] result = new String[encryptedArray.length];
        for (int i = 0; i < encryptedArray.length; i++) {
            result[i] = decrypt(encryptedArray[i]);
        }
        return result;
    }
    
    @Override
    public boolean validateEncryption() {
        // For test purposes, always return true
        return true;
    }
    
    @Override
    public EncryptionMetrics getMetrics() {
        return EncryptionMetrics.builder()
                .algorithm("TEST")
                .transformation("TEST/TEST/TEST")
                .keyLength(256)
                .ivLength(12)
                .tagLength(16)
                .cacheEnabled(false)
                .auditEnabled(false)
                .currentKeyVersion("test-v1")
                .build();
    }
}