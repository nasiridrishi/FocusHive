package com.focushive.identity.repository;

import com.focushive.identity.entity.OAuthRefreshToken;
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
 * Repository for OAuth2 refresh token management.
 */
@Repository
public interface OAuthRefreshTokenRepository extends JpaRepository<OAuthRefreshToken, UUID> {
    
    /**
     * Find token by token hash.
     */
    Optional<OAuthRefreshToken> findByTokenHash(String tokenHash);
    
    /**
     * Find valid token by hash (not revoked and not expired).
     */
    @Query("SELECT t FROM OAuthRefreshToken t WHERE t.tokenHash = :tokenHash AND t.revoked = false AND (t.expiresAt IS NULL OR t.expiresAt > :now)")
    Optional<OAuthRefreshToken> findValidTokenByHash(@Param("tokenHash") String tokenHash, @Param("now") Instant now);
    
    /**
     * Find tokens by access token.
     */
    @Query("SELECT t FROM OAuthRefreshToken t WHERE t.accessToken.id = :accessTokenId")
    Optional<OAuthRefreshToken> findByAccessTokenId(@Param("accessTokenId") UUID accessTokenId);
    
    /**
     * Find all active tokens for a user.
     */
    @Query("SELECT t FROM OAuthRefreshToken t WHERE t.userId = :userId AND t.revoked = false AND (t.expiresAt IS NULL OR t.expiresAt > :now)")
    List<OAuthRefreshToken> findActiveTokensByUser(@Param("userId") UUID userId, @Param("now") Instant now);
    
    /**
     * Find all active tokens for a client.
     */
    @Query("SELECT t FROM OAuthRefreshToken t WHERE t.clientId = :clientId AND t.revoked = false AND (t.expiresAt IS NULL OR t.expiresAt > :now)")
    List<OAuthRefreshToken> findActiveTokensByClient(@Param("clientId") UUID clientId, @Param("now") Instant now);
    
    /**
     * Find tokens by session ID.
     */
    List<OAuthRefreshToken> findBySessionId(String sessionId);
    
    /**
     * Find token chain by following replaced tokens.
     * TODO: Implement recursive token chain lookup using programmatic approach
     * since WITH RECURSIVE is not supported by HQL
     */
    // List<OAuthRefreshToken> findTokenChain(@Param("tokenId") UUID tokenId);
    
    /**
     * Revoke token and its chain.
     */
    @Modifying
    @Query("UPDATE OAuthRefreshToken t SET t.revoked = true, t.revokedAt = :now, t.revocationReason = :reason " +
           "WHERE t.id IN (SELECT rt.id FROM OAuthRefreshToken rt WHERE rt.id = :tokenId OR rt.replacedToken.id = :tokenId)")
    int revokeTokenChain(@Param("tokenId") UUID tokenId, @Param("now") Instant now, @Param("reason") String reason);
    
    /**
     * Revoke all tokens for a user.
     */
    @Modifying
    @Query("UPDATE OAuthRefreshToken t SET t.revoked = true, t.revokedAt = :now, t.revocationReason = :reason WHERE t.userId = :userId AND t.revoked = false")
    int revokeAllTokensForUser(@Param("userId") UUID userId, @Param("now") Instant now, @Param("reason") String reason);
    
    /**
     * Revoke all tokens for a client.
     */
    @Modifying
    @Query("UPDATE OAuthRefreshToken t SET t.revoked = true, t.revokedAt = :now, t.revocationReason = :reason WHERE t.clientId = :clientId AND t.revoked = false")
    int revokeAllTokensForClient(@Param("clientId") UUID clientId, @Param("now") Instant now, @Param("reason") String reason);
    
    /**
     * Update usage information.
     */
    @Modifying
    @Query("UPDATE OAuthRefreshToken t SET t.lastUsedAt = :timestamp, t.usageCount = t.usageCount + 1 WHERE t.id = :tokenId")
    void updateUsage(@Param("tokenId") UUID tokenId, @Param("timestamp") Instant timestamp);
    
    /**
     * Delete expired tokens.
     */
    @Modifying
    @Query("DELETE FROM OAuthRefreshToken t WHERE t.expiresAt IS NOT NULL AND t.expiresAt <= :threshold")
    int deleteExpiredTokens(@Param("threshold") Instant threshold);
}