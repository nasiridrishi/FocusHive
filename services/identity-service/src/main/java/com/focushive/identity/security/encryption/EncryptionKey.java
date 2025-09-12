package com.focushive.identity.security.encryption;

import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.time.Instant;

/**
 * Represents an encryption key with version and metadata.
 * Used for key rotation and management.
 */
@Data
@Builder
@RequiredArgsConstructor
public class EncryptionKey {
    
    private final String version;
    private final byte[] keyBytes;
    private final String salt;
    private final Instant createdAt;
    private final Instant expiresAt;
    private final boolean active;
    private final String algorithm;
    
    /**
     * Check if this key is expired.
     * 
     * @return true if the key has expired
     */
    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }
    
    /**
     * Check if this key is valid for use.
     * 
     * @return true if the key is active and not expired
     */
    public boolean isValid() {
        return active && !isExpired();
    }
    
    /**
     * Get the age of this key in days.
     * 
     * @return age in days
     */
    public long getAgeInDays() {
        if (createdAt == null) {
            return 0;
        }
        return (Instant.now().getEpochSecond() - createdAt.getEpochSecond()) / (24 * 60 * 60);
    }
    
    /**
     * Get remaining days until expiration.
     * 
     * @return days until expiration, or -1 if no expiration set
     */
    public long getDaysUntilExpiration() {
        if (expiresAt == null) {
            return -1;
        }
        return (expiresAt.getEpochSecond() - Instant.now().getEpochSecond()) / (24 * 60 * 60);
    }
}