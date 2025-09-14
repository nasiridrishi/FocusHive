package com.focushive.identity.security.encryption;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Core encryption service implementing AES-256-GCM encryption for PII fields.
 * Provides field-level encryption with GDPR Article 32 compliance.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EncryptionService implements IEncryptionService {
    
    private static final String ENCRYPTION_ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12; // 96 bits
    private static final int GCM_TAG_LENGTH = 16; // 128 bits
    private static final int AES_KEY_LENGTH = 256; // 256 bits
    private static final String HASH_ALGORITHM = "SHA-256";
    private static final String KEY_VERSION_SEPARATOR = ":";
    
    private final EncryptionKeyService keyService;
    private final SecureRandom secureRandom = new SecureRandom();
    
    @Value("${app.encryption.performance.cache-enabled:true}")
    private boolean cacheEnabled;
    
    @Value("${app.encryption.audit.enabled:true}")
    private boolean auditEnabled;
    
    /**
     * Encrypts the given plaintext using AES-256-GCM.
     * 
     * @param plaintext the text to encrypt
     * @return encrypted text in format: keyVersion:base64(iv+encrypted+tag)
     * @throws EncryptionException if encryption fails
     */
    public String encrypt(String plaintext) {
        if (plaintext == null || plaintext.isEmpty()) {
            return plaintext;
        }
        
        try {
            long startTime = System.nanoTime();
            
            // Get current encryption key
            EncryptionKey encryptionKey = keyService.getCurrentEncryptionKey();
            SecretKey secretKey = new SecretKeySpec(encryptionKey.getKeyBytes(), ENCRYPTION_ALGORITHM);
            
            // Generate random IV
            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);
            
            // Initialize cipher
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec);
            
            // Encrypt
            byte[] encryptedBytes = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            
            // Combine IV + encrypted data + auth tag
            ByteBuffer buffer = ByteBuffer.allocate(GCM_IV_LENGTH + encryptedBytes.length);
            buffer.put(iv);
            buffer.put(encryptedBytes);
            
            // Format: keyVersion:base64(iv+encrypted+tag)
            String result = encryptionKey.getVersion() + KEY_VERSION_SEPARATOR + 
                          Base64.getEncoder().encodeToString(buffer.array());
            
            if (auditEnabled) {
                long duration = System.nanoTime() - startTime;
                log.trace("Encrypted field in {} μs, key version: {}", 
                         duration / 1000, encryptionKey.getVersion());
            }
            
            return result;
            
        } catch (Exception e) {
            log.error("Encryption failed", e);
            throw new EncryptionException("Failed to encrypt data", e);
        }
    }
    
    /**
     * Decrypts the given encrypted text.
     * 
     * @param encryptedText encrypted text in format: keyVersion:base64(iv+encrypted+tag)
     * @return decrypted plaintext
     * @throws EncryptionException if decryption fails
     */
    public String decrypt(String encryptedText) {
        if (encryptedText == null || encryptedText.isEmpty()) {
            return encryptedText;
        }
        
        try {
            long startTime = System.nanoTime();
            
            // Parse key version and encrypted data
            String[] parts = encryptedText.split(KEY_VERSION_SEPARATOR, 2);
            if (parts.length != 2) {
                throw new EncryptionException("Invalid encrypted data format");
            }
            
            String keyVersion = parts[0];
            byte[] encryptedData = Base64.getDecoder().decode(parts[1]);
            
            if (encryptedData.length < GCM_IV_LENGTH + GCM_TAG_LENGTH) {
                throw new EncryptionException("Encrypted data too short");
            }
            
            // Get decryption key for this version
            EncryptionKey encryptionKey = keyService.getEncryptionKey(keyVersion);
            SecretKey secretKey = new SecretKeySpec(encryptionKey.getKeyBytes(), ENCRYPTION_ALGORITHM);
            
            // Extract IV and encrypted bytes
            ByteBuffer buffer = ByteBuffer.wrap(encryptedData);
            byte[] iv = new byte[GCM_IV_LENGTH];
            buffer.get(iv);
            
            byte[] encrypted = new byte[buffer.remaining()];
            buffer.get(encrypted);
            
            // Initialize cipher for decryption
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec);
            
            // Decrypt
            byte[] decryptedBytes = cipher.doFinal(encrypted);
            String result = new String(decryptedBytes, StandardCharsets.UTF_8);
            
            if (auditEnabled) {
                long duration = System.nanoTime() - startTime;
                log.trace("Decrypted field in {} μs, key version: {}", 
                         duration / 1000, keyVersion);
            }
            
            return result;
            
        } catch (Exception e) {
            log.error("Decryption failed", e);
            throw new EncryptionException("Failed to decrypt data", e);
        }
    }
    
    /**
     * Creates a searchable hash of the given value using SHA-256.
     * This allows searching on encrypted fields without decryption.
     * 
     * @param value the value to hash
     * @return hexadecimal hash string
     */
    public String createSearchableHash(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        
        try {
            // Use salt from current key to prevent rainbow table attacks
            EncryptionKey currentKey = keyService.getCurrentEncryptionKey();
            String saltedValue = value + currentKey.getSalt();
            
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
            byte[] hashBytes = digest.digest(saltedValue.getBytes(StandardCharsets.UTF_8));
            
            return HexFormat.of().formatHex(hashBytes);
            
        } catch (Exception e) {
            log.error("Hash creation failed", e);
            throw new EncryptionException("Failed to create searchable hash", e);
        }
    }
    
    /**
     * Creates a one-way hash of data for searching encrypted fields.
     * Uses SHA-256 with salt for security.
     * 
     * @param data the data to hash
     * @return the hex-encoded hash
     */
    public String hash(String data) {
        return createSearchableHash(data);
    }
    
    /**
     * Verifies if a plaintext value matches the given hash.
     * 
     * @param value plaintext value to verify
     * @param hash the hash to verify against
     * @return true if the value matches the hash
     */
    public boolean verifyHash(String value, String hash) {
        if (value == null || hash == null) {
            return value == null && hash == null;
        }
        
        String computedHash = createSearchableHash(value);
        return MessageDigest.isEqual(hash.getBytes(), computedHash.getBytes());
    }
    
    /**
     * Generates a new AES-256 key for encryption.
     * Used internally by the key service.
     * 
     * @return new secret key
     */
    SecretKey generateNewKey() {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(ENCRYPTION_ALGORITHM);
            keyGenerator.init(AES_KEY_LENGTH);
            return keyGenerator.generateKey();
        } catch (Exception e) {
            log.error("Key generation failed", e);
            throw new EncryptionException("Failed to generate encryption key", e);
        }
    }
    
    /**
     * Encrypts data for batch operations with performance optimization.
     * 
     * @param plaintextArray array of plaintext strings
     * @return array of encrypted strings
     */
    public String[] encryptBatch(String[] plaintextArray) {
        if (plaintextArray == null) {
            return null;
        }
        
        long startTime = System.nanoTime();
        String[] result = new String[plaintextArray.length];
        
        // Get key once for entire batch
        EncryptionKey encryptionKey = keyService.getCurrentEncryptionKey();
        
        for (int i = 0; i < plaintextArray.length; i++) {
            result[i] = encryptWithKey(plaintextArray[i], encryptionKey);
        }
        
        if (auditEnabled) {
            long duration = System.nanoTime() - startTime;
            log.debug("Encrypted batch of {} items in {} ms", 
                     plaintextArray.length, duration / 1_000_000);
        }
        
        return result;
    }
    
    /**
     * Decrypts data for batch operations with performance optimization.
     * 
     * @param encryptedArray array of encrypted strings
     * @return array of decrypted strings
     */
    public String[] decryptBatch(String[] encryptedArray) {
        if (encryptedArray == null) {
            return null;
        }
        
        long startTime = System.nanoTime();
        String[] result = new String[encryptedArray.length];
        
        for (int i = 0; i < encryptedArray.length; i++) {
            result[i] = decrypt(encryptedArray[i]);
        }
        
        if (auditEnabled) {
            long duration = System.nanoTime() - startTime;
            log.debug("Decrypted batch of {} items in {} ms", 
                     encryptedArray.length, duration / 1_000_000);
        }
        
        return result;
    }
    
    /**
     * Internal method to encrypt with a specific key (for batch operations).
     */
    private String encryptWithKey(String plaintext, EncryptionKey encryptionKey) {
        if (plaintext == null || plaintext.isEmpty()) {
            return plaintext;
        }
        
        try {
            SecretKey secretKey = new SecretKeySpec(encryptionKey.getKeyBytes(), ENCRYPTION_ALGORITHM);
            
            // Generate random IV
            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);
            
            // Initialize cipher
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec);
            
            // Encrypt
            byte[] encryptedBytes = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            
            // Combine IV + encrypted data + auth tag
            ByteBuffer buffer = ByteBuffer.allocate(GCM_IV_LENGTH + encryptedBytes.length);
            buffer.put(iv);
            buffer.put(encryptedBytes);
            
            return encryptionKey.getVersion() + KEY_VERSION_SEPARATOR + 
                   Base64.getEncoder().encodeToString(buffer.array());
            
        } catch (Exception e) {
            log.error("Encryption with specific key failed", e);
            throw new EncryptionException("Failed to encrypt with specific key", e);
        }
    }
    
    /**
     * Validates that encryption is working correctly.
     * Used for health checks and initialization.
     * 
     * @return true if encryption is working
     */
    public boolean validateEncryption() {
        try {
            String testData = "test-encryption-validation";
            String encrypted = encrypt(testData);
            String decrypted = decrypt(encrypted);
            
            boolean isValid = testData.equals(decrypted);
            
            if (!isValid) {
                log.error("Encryption validation failed: data mismatch");
            }
            
            return isValid;
            
        } catch (Exception e) {
            log.error("Encryption validation failed", e);
            return false;
        }
    }
    
    /**
     * Gets encryption performance metrics.
     * 
     * @return encryption metrics
     */
    public EncryptionMetrics getMetrics() {
        return EncryptionMetrics.builder()
                .algorithm(ENCRYPTION_ALGORITHM)
                .transformation(TRANSFORMATION)
                .keyLength(AES_KEY_LENGTH)
                .ivLength(GCM_IV_LENGTH)
                .tagLength(GCM_TAG_LENGTH)
                .cacheEnabled(cacheEnabled)
                .auditEnabled(auditEnabled)
                .currentKeyVersion(keyService.getCurrentKeyVersion())
                .build();
    }
    
    /**
     * Exception thrown when encryption/decryption operations fail.
     */
    public static class EncryptionException extends RuntimeException {
        public EncryptionException(String message) {
            super(message);
        }
        
        public EncryptionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}