package com.focushive.identity.security.encryption;

import lombok.Builder;
import lombok.Data;

/**
 * Metrics and configuration information for the encryption system.
 */
@Data
@Builder
public class EncryptionMetrics {
    
    private final String algorithm;
    private final String transformation;
    private final int keyLength;
    private final int ivLength;
    private final int tagLength;
    private final boolean cacheEnabled;
    private final boolean auditEnabled;
    private final String currentKeyVersion;
    
    /**
     * Get a summary of the encryption configuration.
     * 
     * @return configuration summary
     */
    public String getConfigurationSummary() {
        return String.format("%s-%d with %s, IV: %d bits, Tag: %d bits", 
                           algorithm, keyLength, transformation, ivLength * 8, tagLength * 8);
    }
    
    /**
     * Check if encryption is using recommended security settings.
     * 
     * @return true if using recommended settings
     */
    public boolean isSecure() {
        return "AES".equals(algorithm) && 
               keyLength >= 256 && 
               transformation.contains("GCM") &&
               ivLength >= 12 &&
               tagLength >= 16;
    }
}