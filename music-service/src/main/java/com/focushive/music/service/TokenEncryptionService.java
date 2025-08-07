package com.focushive.music.service;

import com.focushive.music.exception.MusicServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Service for encrypting and decrypting OAuth tokens.
 * 
 * Uses AES-256-GCM encryption for secure token storage.
 * Each token is encrypted with a unique IV for enhanced security.
 * 
 * @author FocusHive Development Team
 * @version 1.0.0
 */
@Slf4j
@Service
public class TokenEncryptionService {

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 16;

    private final SecretKey secretKey;

    public TokenEncryptionService(@Value("${spotify.encryption-key:}") String encryptionKey) {
        this.secretKey = deriveSecretKey(encryptionKey);
        log.info("TokenEncryptionService initialized with AES-256-GCM encryption");
    }

    /**
     * Encrypts a token using AES-256-GCM.
     * 
     * @param plaintext The token to encrypt
     * @return Base64-encoded encrypted token with IV prepended
     * @throws MusicServiceException.SecurityException if encryption fails
     */
    public String encrypt(String plaintext) {
        if (plaintext == null || plaintext.isEmpty()) {
            return null;
        }

        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            
            // Generate random IV
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);
            
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);
            
            byte[] encryptedData = cipher.doFinal(plaintext.getBytes());
            
            // Prepend IV to encrypted data
            byte[] encryptedWithIv = new byte[iv.length + encryptedData.length];
            System.arraycopy(iv, 0, encryptedWithIv, 0, iv.length);
            System.arraycopy(encryptedData, 0, encryptedWithIv, iv.length, encryptedData.length);
            
            String encrypted = Base64.getEncoder().encodeToString(encryptedWithIv);
            log.debug("Token encrypted successfully, length: {}", encrypted.length());
            
            return encrypted;
            
        } catch (Exception e) {
            log.error("Failed to encrypt token", e);
            throw new MusicServiceException.SecurityException("Token encryption failed", e);
        }
    }

    /**
     * Decrypts a token using AES-256-GCM.
     * 
     * @param ciphertext Base64-encoded encrypted token with IV prepended
     * @return Decrypted token
     * @throws MusicServiceException.SecurityException if decryption fails
     */
    public String decrypt(String ciphertext) {
        if (ciphertext == null || ciphertext.isEmpty()) {
            return null;
        }

        try {
            byte[] encryptedWithIv = Base64.getDecoder().decode(ciphertext);
            
            // Extract IV
            byte[] iv = new byte[GCM_IV_LENGTH];
            System.arraycopy(encryptedWithIv, 0, iv, 0, iv.length);
            
            // Extract encrypted data
            byte[] encryptedData = new byte[encryptedWithIv.length - GCM_IV_LENGTH];
            System.arraycopy(encryptedWithIv, GCM_IV_LENGTH, encryptedData, 0, encryptedData.length);
            
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);
            
            byte[] decryptedData = cipher.doFinal(encryptedData);
            String decrypted = new String(decryptedData);
            
            log.debug("Token decrypted successfully, length: {}", decrypted.length());
            return decrypted;
            
        } catch (Exception e) {
            log.error("Failed to decrypt token", e);
            throw new MusicServiceException.SecurityException("Token decryption failed", e);
        }
    }

    /**
     * Generates a masked version of a token for logging purposes.
     * 
     * @param token The token to mask
     * @return Partially masked token
     */
    public String maskToken(String token) {
        if (token == null || token.length() < 8) {
            return "***";
        }
        return token.substring(0, 4) + "***" + token.substring(token.length() - 4);
    }

    /**
     * Validates that a token can be encrypted and decrypted correctly.
     * 
     * @param token The token to validate
     * @return true if the token is valid for encryption/decryption
     */
    public boolean validateToken(String token) {
        if (token == null || token.isEmpty()) {
            return false;
        }

        try {
            String encrypted = encrypt(token);
            String decrypted = decrypt(encrypted);
            return token.equals(decrypted);
        } catch (Exception e) {
            log.warn("Token validation failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Derives a secret key from the provided encryption key or generates a new one.
     * 
     * @param encryptionKey Base64-encoded encryption key or empty string
     * @return SecretKey for AES encryption
     */
    private SecretKey deriveSecretKey(String encryptionKey) {
        try {
            if (encryptionKey != null && !encryptionKey.isEmpty()) {
                // Use provided key
                byte[] keyBytes = Base64.getDecoder().decode(encryptionKey);
                if (keyBytes.length == 32) { // 256 bits
                    log.info("Using provided encryption key");
                    return new SecretKeySpec(keyBytes, ALGORITHM);
                } else {
                    log.warn("Provided encryption key has invalid length: {}, generating new key", keyBytes.length);
                }
            }
            
            // Generate new key
            log.warn("Generating new encryption key - tokens from previous sessions will not be decryptable!");
            KeyGenerator keyGenerator = KeyGenerator.getInstance(ALGORITHM);
            keyGenerator.init(256); // AES-256
            SecretKey key = keyGenerator.generateKey();
            
            String base64Key = Base64.getEncoder().encodeToString(key.getEncoded());
            log.info("Generated new AES-256 key. To persist this key, set: spotify.encryption-key={}", base64Key);
            
            return key;
            
        } catch (Exception e) {
            log.error("Failed to derive secret key", e);
            throw new MusicServiceException.SecurityException("Failed to initialize encryption", e);
        }
    }

    /**
     * Rotates the encryption key and re-encrypts existing tokens.
     * This method would be used in a production environment for periodic key rotation.
     * 
     * @param newEncryptionKey The new Base64-encoded encryption key
     * @return true if rotation was successful
     */
    public boolean rotateKey(String newEncryptionKey) {
        // This would typically involve:
        // 1. Validating the new key
        // 2. Decrypting all existing tokens with the old key
        // 3. Re-encrypting them with the new key
        // 4. Updating the secret key
        // For now, this is a placeholder for future implementation
        log.warn("Key rotation not yet implemented");
        return false;
    }
}