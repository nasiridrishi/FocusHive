package com.focushive.identity.dto;

import com.focushive.identity.entity.OAuthRefreshToken;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Result of a token rotation operation containing both the entity and the token value.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenRotationResult {
    /**
     * The new refresh token entity (contains hash, not actual value).
     */
    private OAuthRefreshToken refreshToken;

    /**
     * The actual refresh token value to return to the client.
     */
    private String tokenValue;
}