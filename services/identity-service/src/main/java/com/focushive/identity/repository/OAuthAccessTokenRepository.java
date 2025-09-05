package com.focushive.identity.repository;

import com.focushive.identity.entity.OAuthAccessToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for OAuth2 access token management.
 */
@Repository
public interface OAuthAccessTokenRepository extends JpaRepository<OAuthAccessToken, UUID> {
    
    /**
     * Find token by token hash.
     */
    Optional<OAuthAccessToken> findByTokenHash(String tokenHash);
    
    /**
     * Find valid token by hash (not revoked and not expired).
     */
    @Query("SELECT t FROM OAuthAccessToken t WHERE t.tokenHash = :tokenHash AND t.revoked = false AND t.expiresAt > :now")
    Optional<OAuthAccessToken> findValidTokenByHash(@Param("tokenHash") String tokenHash, @Param("now") Instant now);
    
    /**
     * Find all active tokens for a user.
     */
    @Query("SELECT t FROM OAuthAccessToken t WHERE t.userId = :userId AND t.revoked = false AND t.expiresAt > :now")
    List<OAuthAccessToken> findActiveTokensByUser(@Param("userId") UUID userId, @Param("now") Instant now);
    
    /**
     * Find all active tokens for a client.
     */
    @Query("SELECT t FROM OAuthAccessToken t WHERE t.clientId = :clientId AND t.revoked = false AND t.expiresAt > :now")
    List<OAuthAccessToken> findActiveTokensByClient(@Param("clientId") UUID clientId, @Param("now") Instant now);
    
    /**
     * Find expired tokens.
     */
    @Query("SELECT t FROM OAuthAccessToken t WHERE t.revoked = false AND t.expiresAt <= :now")
    List<OAuthAccessToken> findExpiredTokens(@Param("now") Instant now);
    
    /**
     * Revoke all tokens for a user.
     */
    @Modifying
    @Query("UPDATE OAuthAccessToken t SET t.revoked = true, t.revokedAt = :now, t.revocationReason = :reason WHERE t.userId = :userId AND t.revoked = false")
    int revokeAllTokensForUser(@Param("userId") UUID userId, @Param("now") Instant now, @Param("reason") String reason);
    
    /**
     * Revoke all tokens for a client.
     */
    @Modifying
    @Query("UPDATE OAuthAccessToken t SET t.revoked = true, t.revokedAt = :now, t.revocationReason = :reason WHERE t.clientId = :clientId AND t.revoked = false")
    int revokeAllTokensForClient(@Param("clientId") UUID clientId, @Param("now") Instant now, @Param("reason") String reason);
    
    /**
     * Count active tokens for a user.
     */
    @Query("SELECT COUNT(t) FROM OAuthAccessToken t WHERE t.userId = :userId AND t.revoked = false AND t.expiresAt > :now")
    long countActiveTokensByUser(@Param("userId") UUID userId, @Param("now") Instant now);
    
    /**
     * Update usage information.
     */
    @Modifying
    @Query("UPDATE OAuthAccessToken t SET t.lastUsedAt = :timestamp, t.usageCount = t.usageCount + 1 WHERE t.id = :tokenId")
    void updateUsage(@Param("tokenId") UUID tokenId, @Param("timestamp") Instant timestamp);
    
    /**
     * Delete expired tokens.
     */
    @Modifying
    @Query("DELETE FROM OAuthAccessToken t WHERE t.expiresAt <= :threshold")
    int deleteExpiredTokens(@Param("threshold") Instant threshold);
}