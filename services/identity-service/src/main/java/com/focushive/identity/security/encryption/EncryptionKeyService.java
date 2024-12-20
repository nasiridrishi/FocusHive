package com.focushive.identity.security.encryption;

import com.focushive.identity.entity.EncryptionKeyEntity;
import com.focushive.identity.repository.EncryptionKeyRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Service for managing encryption keys with automatic rotation support.
 * Implements secure key storage and rotation for GDPR compliance.
 */
@Service
@Slf4j
public class EncryptionKeyService {

    private final EncryptionKeyRepository encryptionKeyRepository;
    
    private final ConcurrentHashMap<String, EncryptionKey> keyCache = new ConcurrentHashMap<>();
    private final ReadWriteLock keyRotationLock = new ReentrantReadWriteLock();
    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Constructor with repository injection for persistence.
     */
    public EncryptionKeyService(EncryptionKeyRepository encryptionKeyRepository) {
        this.encryptionKeyRepository = encryptionKeyRepository;
    }

    @Value("${app.encryption.master-key}")
    private String masterKeyBase64;
    
    @Value("${app.encryption.key-rotation-days:90}")
    private int keyRotationDays;
    
    @Value("${app.encryption.key-retention-days:365}")
    private int keyRetentionDays;
    
    @Value("${app.encryption.auto-rotation-enabled:true}")
    private boolean autoRotationEnabled;
    
    private volatile String currentKeyVersion;
    private volatile boolean initialized = false;
    
    /**
     * Initialize the encryption key service.
     * Loads existing keys from database or creates the initial key if none exists.
     */
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void initialize() {
        if (masterKeyBase64 == null || masterKeyBase64.isEmpty()) {
            throw new IllegalStateException("Master key not configured. Set app.encryption.master-key");
        }
        
        log.info("Initializing encryption key service");
        
        try {
            // Validate master key format
            validateMasterKey();

            // Load existing keys from database
            loadExistingKeys();

            // Create initial key if none exists
            if (currentKeyVersion == null) {
                log.info("No active encryption key found in database, creating new key");
                createNewKey();
            } else {
                log.info("Loaded active encryption key version: {}", currentKeyVersion);
            }

            // Clean up expired keys
            cleanupExpiredKeys();
            
            initialized = true;
            log.info("Encryption key service initialized successfully with {} keys", keyCache.size());
            
        } catch (Exception e) {
            log.error("Failed to initialize encryption key service", e);
            throw new IllegalStateException("Encryption key service initialization failed", e);
        }
    }

    /**
     * Load existing encryption keys from database into cache.
     * Sets the current key version if an active key exists.
     */
    private void loadExistingKeys() {
        log.debug("Loading existing encryption keys from database");

        // Load all valid keys (active and not expired)
        List<EncryptionKeyEntity> validKeys = encryptionKeyRepository.findValidKeys(Instant.now());

        if (validKeys.isEmpty()) {
            log.info("No valid encryption keys found in database");
            return;
        }

        // Load keys into cache
        for (EncryptionKeyEntity entity : validKeys) {
            EncryptionKey key = entity.toDomainObject();
            keyCache.put(key.getVersion(), key);

            // Set current version if this is the active key
            if (entity.isActive()) {
                currentKeyVersion = key.getVersion();
                log.info("Set current encryption key version: {}", currentKeyVersion);
            }
        }

        log.info("Loaded {} encryption keys from database", validKeys.size());
    }

    /**
     * Gets the current active encryption key.
     * 
     * @return current encryption key
     * @throws IllegalStateException if service is not initialized
     */
    public EncryptionKey getCurrentEncryptionKey() {
        if (!initialized) {
            throw new IllegalStateException("Encryption key service not initialized");
        }
        
        keyRotationLock.readLock().lock();
        try {
            EncryptionKey key = keyCache.get(currentKeyVersion);
            if (key == null || !key.isValid()) {
                throw new IllegalStateException("Current encryption key is invalid or missing");
            }
            return key;
        } finally {
            keyRotationLock.readLock().unlock();
        }
    }
    
    /**
     * Gets an encryption key by version for decryption.
     * 
     * @param version the key version to retrieve
     * @return encryption key for the specified version
     * @throws IllegalArgumentException if key version not found
     */
    public EncryptionKey getEncryptionKey(String version) {
        if (!initialized) {
            throw new IllegalStateException("Encryption key service not initialized");
        }
        
        keyRotationLock.readLock().lock();
        try {
            EncryptionKey key = keyCache.get(version);
            if (key == null) {
                // Try to derive key from master key if it's a valid version format
                if (isValidVersionFormat(version)) {
                    key = deriveKeyFromMaster(version);
                    keyCache.put(version, key);
                    log.info("Derived encryption key for version: {}", version);
                } else {
                    throw new IllegalArgumentException("Invalid encryption key version: " + version);
                }
            }
            return key;
        } finally {
            keyRotationLock.readLock().unlock();
        }
    }
    
    /**
     * Gets the current key version.
     * 
     * @return current key version string
     */
    public String getCurrentKeyVersion() {
        return currentKeyVersion;
    }
    
    /**
     * Manually rotates the encryption key.
     * Creates a new key and marks it as current.
     * 
     * @return new key version
     */
    public String rotateKey() {
        if (!initialized) {
            throw new IllegalStateException("Encryption key service not initialized");
        }
        
        keyRotationLock.writeLock().lock();
        try {
            String oldVersion = currentKeyVersion;
            String newVersion = createNewKey();
            
            log.info("Encryption key rotated from version {} to {}", oldVersion, newVersion);
            
            // Keep the old key active for decryption but mark new one as current
            return newVersion;
            
        } finally {
            keyRotationLock.writeLock().unlock();
        }
    }
    
    /**
     * Scheduled task to automatically rotate keys.
     * Runs daily and checks if rotation is needed.
     */
    @Scheduled(cron = "0 0 2 * * ?") // Daily at 2 AM
    public void autoRotateKey() {
        if (!autoRotationEnabled || !initialized) {
            return;
        }
        
        try {
            EncryptionKey currentKey = getCurrentEncryptionKey();
            long ageInDays = currentKey.getAgeInDays();
            
            if (ageInDays >= keyRotationDays) {
                log.info("Auto-rotating encryption key (age: {} days, threshold: {} days)", 
                        ageInDays, keyRotationDays);
                rotateKey();
            } else {
                log.debug("Encryption key rotation not needed (age: {} days)", ageInDays);
            }
            
        } catch (Exception e) {
            log.error("Auto key rotation failed", e);
        }
    }
    
    /**
     * Scheduled task to clean up expired keys.
     * Runs weekly to remove old keys that are no longer needed.
     */
    @Scheduled(cron = "0 0 3 * * SUN") // Weekly on Sunday at 3 AM
    public void cleanupExpiredKeys() {
        if (!initialized) {
            return;
        }
        
        keyRotationLock.writeLock().lock();
        try {
            Instant cutoffTime = Instant.now().minus(keyRetentionDays, ChronoUnit.DAYS);
            
            keyCache.entrySet().removeIf(entry -> {
                EncryptionKey key = entry.getValue();
                boolean shouldRemove = key.getCreatedAt().isBefore(cutoffTime) && 
                                     !entry.getKey().equals(currentKeyVersion);
                
                if (shouldRemove) {
                    log.info("Removing expired encryption key version: {}", entry.getKey());
                }
                return shouldRemove;
            });
            
        } finally {
            keyRotationLock.writeLock().unlock();
        }
    }
    
    /**
     * Creates a new encryption key, persists it to database, and adds it to the cache.
     *
     * @return new key version
     */
    @Transactional
    private String createNewKey() {
        try {
            String version = generateKeyVersion();
            EncryptionKey key = deriveKeyFromMaster(version);

            // Persist to database
            EncryptionKeyEntity entity = EncryptionKeyEntity.fromDomainObject(key);

            // Deactivate any existing active keys
            encryptionKeyRepository.deactivateAll();

            // Save the new key as active
            entity = encryptionKeyRepository.save(entity);

            // Add to cache
            keyCache.put(version, key);
            currentKeyVersion = version;

            log.info("Created and persisted new encryption key with version: {}", version);
            return version;
            
        } catch (Exception e) {
            log.error("Failed to create new encryption key", e);
            throw new RuntimeException("Key creation failed", e);
        }
    }
    
    /**
     * Derives an encryption key from the master key using PBKDF2-like approach.
     * 
     * @param version key version to use as salt component
     * @return derived encryption key
     */
    private EncryptionKey deriveKeyFromMaster(String version) {
        try {
            // Decode master key
            byte[] masterKeyBytes = Base64.getDecoder().decode(masterKeyBase64);
            
            // Create salt combining version and master key
            String saltInput = version + "-" + Base64.getEncoder().encodeToString(masterKeyBytes);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String salt = Base64.getEncoder().encodeToString(
                digest.digest(saltInput.getBytes(StandardCharsets.UTF_8))
            );
            
            // Derive key using HMAC approach
            byte[] keyMaterial = deriveKeyMaterial(masterKeyBytes, version.getBytes(StandardCharsets.UTF_8));
            
            Instant now = Instant.now();
            return EncryptionKey.builder()
                    .version(version)
                    .keyBytes(keyMaterial)
                    .salt(salt)
                    .createdAt(now)
                    .expiresAt(now.plus(keyRetentionDays, ChronoUnit.DAYS))
                    .active(true)
                    .algorithm("AES-256-GCM")
                    .build();
            
        } catch (Exception e) {
            log.error("Failed to derive encryption key for version: {}", version, e);
            throw new RuntimeException("Key derivation failed", e);
        }
    }
    
    /**
     * Derives key material using a simple but secure approach.
     * In production, consider using PBKDF2 or Argon2.
     * 
     * @param masterKey master key bytes
     * @param salt salt bytes
     * @return derived key material
     */
    private byte[] deriveKeyMaterial(byte[] masterKey, byte[] salt) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        
        // Multiple rounds of hashing for key stretching
        byte[] material = new byte[masterKey.length + salt.length];
        System.arraycopy(masterKey, 0, material, 0, masterKey.length);
        System.arraycopy(salt, 0, material, masterKey.length, salt.length);
        
        for (int i = 0; i < 10000; i++) { // 10,000 rounds
            material = digest.digest(material);
        }
        
        // Ensure we have exactly 32 bytes for AES-256
        if (material.length >= 32) {
            byte[] key = new byte[32];
            System.arraycopy(material, 0, key, 0, 32);
            return key;
        } else {
            // Extend if needed (shouldn't happen with SHA-256)
            throw new IllegalStateException("Derived key material too short");
        }
    }
    
    /**
     * Generates a new key version based on timestamp and randomness.
     * 
     * @return new key version string
     */
    private String generateKeyVersion() {
        long timestamp = Instant.now().getEpochSecond();
        byte[] randomBytes = new byte[8];
        secureRandom.nextBytes(randomBytes);
        String randomPart = Base64.getEncoder().encodeToString(randomBytes)
                                  .substring(0, 8)
                                  .replace("+", "A")
                                  .replace("/", "B")
                                  .replace("=", "C");
        
        return "v" + timestamp + "-" + randomPart;
    }
    
    /**
     * Validates the master key format and strength.
     */
    private void validateMasterKey() {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(masterKeyBase64);
            if (keyBytes.length < 32) {
                throw new IllegalArgumentException("Master key must be at least 256 bits (32 bytes)");
            }
            
            // Check for weak keys (all zeros, all ones, etc.)
            boolean allSame = true;
            byte first = keyBytes[0];
            for (byte b : keyBytes) {
                if (b != first) {
                    allSame = false;
                    break;
                }
            }
            
            if (allSame) {
                throw new IllegalArgumentException("Master key appears to be weak (all bytes are the same)");
            }
            
            log.info("Master key validation successful (key length: {} bytes)", keyBytes.length);
            
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid master key format. Must be Base64 encoded.", e);
        }
    }
    
    /**
     * Validates the format of a key version string.
     * 
     * @param version version string to validate
     * @return true if format is valid
     */
    private boolean isValidVersionFormat(String version) {
        if (version == null || version.isEmpty()) {
            return false;
        }
        
        // Expected format: v{timestamp}-{8-char-random}
        return version.matches("^v\\d+-[A-Za-z0-9]{8}$");
    }
    
    /**
     * Gets statistics about the key cache.
     * 
     * @return key cache statistics
     */
    public KeyCacheStats getKeyStats() {
        keyRotationLock.readLock().lock();
        try {
            int totalKeys = keyCache.size();
            long activeKeys = keyCache.values().stream()
                                   .mapToLong(key -> key.isValid() ? 1 : 0)
                                   .sum();
            
            EncryptionKey currentKey = getCurrentEncryptionKey();
            
            return KeyCacheStats.builder()
                    .totalKeys(totalKeys)
                    .activeKeys((int)activeKeys)
                    .currentKeyVersion(currentKeyVersion)
                    .currentKeyAge(currentKey.getAgeInDays())
                    .rotationThreshold(keyRotationDays)
                    .retentionDays(keyRetentionDays)
                    .autoRotationEnabled(autoRotationEnabled)
                    .build();
            
        } finally {
            keyRotationLock.readLock().unlock();
        }
    }
    
    /**
     * Statistics about the key cache.
     */
    @lombok.Builder
    @lombok.Data
    public static class KeyCacheStats {
        private final int totalKeys;
        private final int activeKeys;
        private final String currentKeyVersion;
        private final long currentKeyAge;
        private final int rotationThreshold;
        private final int retentionDays;
        private final boolean autoRotationEnabled;
        
        public boolean needsRotation() {
            return currentKeyAge >= rotationThreshold;
        }
    }
}