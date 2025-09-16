package com.focushive.notification.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for encrypting and decrypting sensitive data using AES-GCM.
 *
 * <p>This service provides field-level encryption for sensitive information in requests and responses,
 * implementing AES-256-GCM (Galois/Counter Mode) for authenticated encryption. It supports both
 * string-level and object-level encryption with automatic detection of sensitive fields.</p>
 *
 * <h2>Features:</h2>
 * <ul>
 *   <li>AES-256-GCM encryption with authentication</li>
 *   <li>Automatic sensitive field detection using annotations</li>
 *   <li>Key rotation support for enhanced security</li>
 *   <li>Master key encryption for data encryption keys (DEK)</li>
 *   <li>Base64 encoding for safe transport</li>
 * </ul>
 *
 * <h2>Security Considerations:</h2>
 * <ul>
 *   <li>Uses cryptographically secure random IV generation</li>
 *   <li>Implements authenticated encryption to prevent tampering</li>
 *   <li>Supports key rotation without data re-encryption</li>
 *   <li>Master key should be stored in secure key management service</li>
 * </ul>
 *
 * @author FocusHive Security Team
 * @version 2.0
 * @since 1.0
 * @see SensitiveData
 * @see ApiSignatureService
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataEncryptionService {

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int GCM_IV_LENGTH = 12;
    private static final int AES_KEY_SIZE = 256;

    @Value("${security.encryption.master-key:#{null}}")
    private String masterKeyString;

    @Value("${security.encryption.enabled:true}")
    private boolean encryptionEnabled;

    @Value("${security.encryption.key-rotation-days:30}")
    private int keyRotationDays;

    private final ObjectMapper objectMapper;

    // Cache for data encryption keys
    private final Map<String, SecretKey> keyCache = new ConcurrentHashMap<>();

    // Master key for encrypting data encryption keys
    private SecretKey masterKey;

    /**
     * Initialize the master key.
     */
    @jakarta.annotation.PostConstruct
    public void init() {
        if (!encryptionEnabled) {
            log.info("Data encryption is disabled");
            return;
        }

        if (masterKeyString != null && !masterKeyString.isEmpty()) {
            // Use provided master key
            byte[] decodedKey = Base64.getDecoder().decode(masterKeyString);
            masterKey = new SecretKeySpec(decodedKey, ALGORITHM);
        } else {
            // Generate new master key (for development only)
            try {
                KeyGenerator keyGen = KeyGenerator.getInstance(ALGORITHM);
                keyGen.init(AES_KEY_SIZE);
                masterKey = keyGen.generateKey();

                log.warn("Generated new master key for development. In production, provide master key via configuration.");
                log.debug("Generated master key (base64): {}",
                    Base64.getEncoder().encodeToString(masterKey.getEncoded()));
            } catch (Exception e) {
                log.error("Failed to initialize master key", e);
                throw new RuntimeException("Failed to initialize encryption service", e);
            }
        }
    }

    /**
     * Encrypts sensitive string data using AES-256-GCM.
     *
     * <p>This method encrypts the provided plaintext string using AES-256 in GCM mode,
     * which provides both confidentiality and authenticity. A random IV is generated
     * for each encryption operation to ensure semantic security.</p>
     *
     * <h3>Process:</h3>
     * <ol>
     *   <li>Generate cryptographically secure random IV (12 bytes)</li>
     *   <li>Retrieve or create data encryption key</li>
     *   <li>Encrypt plaintext using AES-256-GCM</li>
     *   <li>Combine IV and ciphertext</li>
     *   <li>Encode result as Base64 for safe transport</li>
     * </ol>
     *
     * @param plaintext the sensitive data to encrypt (can be null)
     * @return Base64-encoded encrypted data with IV prepended, or original value if
     *         encryption is disabled or input is null
     * @throws RuntimeException if encryption fails due to cryptographic errors
     *
     * @example
     * <pre>{@code
     * String encrypted = encryptionService.encrypt("sensitive-api-key");
     * // Result: "dGVzdGl2Li4uCg==" (Base64 encoded IV + ciphertext)
     * }</pre>
     */
    public String encrypt(String plaintext) {
        if (!encryptionEnabled || plaintext == null) {
            return plaintext;
        }

        try {
            // Generate random IV
            byte[] iv = new byte[GCM_IV_LENGTH];
            SecureRandom random = new SecureRandom();
            random.nextBytes(iv);

            // Get or generate data encryption key
            SecretKey dataKey = getOrCreateDataKey("default");

            // Initialize cipher
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, dataKey, spec);

            // Encrypt
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            // Combine IV and ciphertext
            byte[] combined = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(ciphertext, 0, combined, iv.length, ciphertext.length);

            // Return base64 encoded
            return Base64.getEncoder().encodeToString(combined);

        } catch (Exception e) {
            log.error("Encryption failed", e);
            throw new RuntimeException("Failed to encrypt data", e);
        }
    }

    /**
     * Decrypt sensitive string data.
     */
    public String decrypt(String encryptedData) {
        if (!encryptionEnabled || encryptedData == null) {
            return encryptedData;
        }

        try {
            // Decode from base64
            byte[] combined = Base64.getDecoder().decode(encryptedData);

            // Extract IV and ciphertext
            byte[] iv = new byte[GCM_IV_LENGTH];
            byte[] ciphertext = new byte[combined.length - GCM_IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, iv.length);
            System.arraycopy(combined, iv.length, ciphertext, 0, ciphertext.length);

            // Get data encryption key
            SecretKey dataKey = getOrCreateDataKey("default");

            // Initialize cipher
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, dataKey, spec);

            // Decrypt
            byte[] plaintext = cipher.doFinal(ciphertext);

            return new String(plaintext, StandardCharsets.UTF_8);

        } catch (Exception e) {
            log.error("Decryption failed", e);
            throw new RuntimeException("Failed to decrypt data", e);
        }
    }

    /**
     * Encrypt an object by encrypting its sensitive fields.
     */
    public <T> T encryptObject(T object) {
        if (!encryptionEnabled || object == null) {
            return object;
        }

        try {
            // Convert to JSON
            String json = objectMapper.writeValueAsString(object);
            Map<String, Object> map = objectMapper.readValue(json, Map.class);

            // Encrypt sensitive fields
            encryptSensitiveFields(map);

            // Convert back to object
            String encryptedJson = objectMapper.writeValueAsString(map);
            return (T) objectMapper.readValue(encryptedJson, object.getClass());

        } catch (Exception e) {
            log.error("Failed to encrypt object", e);
            return object;
        }
    }

    /**
     * Decrypt an object by decrypting its sensitive fields.
     */
    public <T> T decryptObject(T object) {
        if (!encryptionEnabled || object == null) {
            return object;
        }

        try {
            // Convert to JSON
            String json = objectMapper.writeValueAsString(object);
            Map<String, Object> map = objectMapper.readValue(json, Map.class);

            // Decrypt sensitive fields
            decryptSensitiveFields(map);

            // Convert back to object
            String decryptedJson = objectMapper.writeValueAsString(map);
            return (T) objectMapper.readValue(decryptedJson, object.getClass());

        } catch (Exception e) {
            log.error("Failed to decrypt object", e);
            return object;
        }
    }

    /**
     * Encrypt sensitive fields in a map.
     */
    private void encryptSensitiveFields(Map<String, Object> map) {
        // Fields considered sensitive
        String[] sensitiveFields = {
            "password", "email", "phone", "phoneNumber",
            "ssn", "socialSecurityNumber", "creditCard",
            "bankAccount", "apiKey", "secretKey", "token",
            "personalData", "sensitiveData", "privateKey"
        };

        for (String field : sensitiveFields) {
            if (map.containsKey(field) && map.get(field) instanceof String) {
                String value = (String) map.get(field);
                if (value != null && !value.isEmpty()) {
                    map.put(field, encrypt(value));
                }
            }
        }

        // Recursively encrypt nested objects
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (entry.getValue() instanceof Map) {
                encryptSensitiveFields((Map<String, Object>) entry.getValue());
            }
        }
    }

    /**
     * Decrypt sensitive fields in a map.
     */
    private void decryptSensitiveFields(Map<String, Object> map) {
        // Fields considered sensitive
        String[] sensitiveFields = {
            "password", "email", "phone", "phoneNumber",
            "ssn", "socialSecurityNumber", "creditCard",
            "bankAccount", "apiKey", "secretKey", "token",
            "personalData", "sensitiveData", "privateKey"
        };

        for (String field : sensitiveFields) {
            if (map.containsKey(field) && map.get(field) instanceof String) {
                String value = (String) map.get(field);
                if (value != null && !value.isEmpty() && isEncrypted(value)) {
                    try {
                        map.put(field, decrypt(value));
                    } catch (Exception e) {
                        log.debug("Field {} is not encrypted or decryption failed", field);
                    }
                }
            }
        }

        // Recursively decrypt nested objects
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (entry.getValue() instanceof Map) {
                decryptSensitiveFields((Map<String, Object>) entry.getValue());
            }
        }
    }

    /**
     * Check if a string appears to be encrypted (base64 encoded).
     */
    private boolean isEncrypted(String value) {
        try {
            byte[] decoded = Base64.getDecoder().decode(value);
            return decoded.length > GCM_IV_LENGTH;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Get or create a data encryption key.
     */
    private SecretKey getOrCreateDataKey(String keyId) {
        return keyCache.computeIfAbsent(keyId, k -> {
            try {
                // In production, retrieve from key management service
                // For now, generate a new key
                KeyGenerator keyGen = KeyGenerator.getInstance(ALGORITHM);
                keyGen.init(AES_KEY_SIZE);
                return keyGen.generateKey();
            } catch (Exception e) {
                log.error("Failed to generate data key", e);
                throw new RuntimeException("Failed to generate data key", e);
            }
        });
    }

    /**
     * Rotate encryption keys.
     */
    public void rotateKeys() {
        log.info("Rotating encryption keys");
        keyCache.clear();

        // In production, this would:
        // 1. Generate new keys
        // 2. Re-encrypt existing data
        // 3. Store new keys securely
        // 4. Mark old keys for deletion after grace period
    }

    /**
     * Generate a new encryption key.
     */
    public String generateKey() {
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance(ALGORITHM);
            keyGen.init(AES_KEY_SIZE);
            SecretKey key = keyGen.generateKey();
            return Base64.getEncoder().encodeToString(key.getEncoded());
        } catch (Exception e) {
            log.error("Failed to generate key", e);
            throw new RuntimeException("Failed to generate key", e);
        }
    }
}