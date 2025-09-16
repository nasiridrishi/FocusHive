package com.focushive.notification.dto;

import com.focushive.notification.annotation.SensitiveData;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * User information DTO for identity service responses.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserInfo {
    private String userId;

    @SensitiveData(maskingPattern = "***@***.***")
    private String email;

    private String name;

    @SensitiveData(maskingPattern = "***-***-****")
    private String phoneNumber;
    private boolean emailVerified;
    private boolean phoneVerified;
    private long lastUpdated;
    private boolean stale; // Indicates if data is from cache

    /**
     * Check if the cached data is stale (older than 5 minutes).
     */
    public boolean isStale() {
        long fiveMinutesAgo = System.currentTimeMillis() - (5 * 60 * 1000);
        return lastUpdated < fiveMinutesAgo || stale;
    }

    /**
     * Check if this is fallback data.
     */
    public boolean isFallbackData() {
        return stale;
    }
}