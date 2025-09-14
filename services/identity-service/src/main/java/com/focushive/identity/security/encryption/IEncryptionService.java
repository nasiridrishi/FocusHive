package com.focushive.identity.security.encryption;

/**
 * Interface for encryption services to enable dependency injection and testing.
 * Provides field-level encryption with GDPR Article 32 compliance.
 */
public interface IEncryptionService {
    
    /**
     * Encrypts the given plaintext using AES-256-GCM.
     * 
     * @param plaintext the text to encrypt
     * @return encrypted text in format: keyVersion:base64(iv+encrypted+tag)
     * @throws EncryptionService.EncryptionException if encryption fails
     */
    String encrypt(String plaintext);
    
    /**
     * Decrypts the given encrypted text.
     * 
     * @param encryptedText encrypted text in format: keyVersion:base64(iv+encrypted+tag)
     * @return decrypted plaintext
     * @throws EncryptionService.EncryptionException if decryption fails
     */
    String decrypt(String encryptedText);
    
    /**
     * Creates a searchable hash of the given value using SHA-256.
     * This allows searching on encrypted fields without decryption.
     * 
     * @param value the value to hash
     * @return hexadecimal hash string
     */
    String createSearchableHash(String value);
    
    /**
     * Creates a one-way hash of data for searching encrypted fields.
     * Uses SHA-256 with salt for security.
     * 
     * @param data the data to hash
     * @return the hex-encoded hash
     */
    String hash(String data);
    
    /**
     * Verifies if a plaintext value matches the given hash.
     * 
     * @param value plaintext value to verify
     * @param hash the hash to verify against
     * @return true if the value matches the hash
     */
    boolean verifyHash(String value, String hash);
    
    /**
     * Encrypts data for batch operations with performance optimization.
     * 
     * @param plaintextArray array of plaintext strings
     * @return array of encrypted strings
     */
    String[] encryptBatch(String[] plaintextArray);
    
    /**
     * Decrypts data for batch operations with performance optimization.
     * 
     * @param encryptedArray array of encrypted strings
     * @return array of decrypted strings
     */
    String[] decryptBatch(String[] encryptedArray);
    
    /**
     * Validates that encryption is working correctly.
     * Used for health checks and initialization.
     * 
     * @return true if encryption is working
     */
    boolean validateEncryption();
    
    /**
     * Gets encryption performance metrics.
     * 
     * @return encryption metrics
     */
    EncryptionMetrics getMetrics();
}