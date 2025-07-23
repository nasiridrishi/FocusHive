package com.focushive.identity.security;

import com.focushive.identity.entity.Persona;
import com.focushive.identity.entity.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * JWT token provider for generating and validating tokens.
 * Supports user authentication with active persona information.
 */
@Slf4j
@Component
public class JwtTokenProvider {
    
    private final SecretKey secretKey;
    private final long accessTokenExpirationMs;
    private final long refreshTokenExpirationMs;
    private final long rememberMeTokenExpirationMs;
    private final String issuer;
    
    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-expiration-ms:3600000}") long accessTokenExpirationMs, // 1 hour
            @Value("${jwt.refresh-token-expiration-ms:2592000000}") long refreshTokenExpirationMs, // 30 days
            @Value("${jwt.remember-me-expiration-ms:7776000000}") long rememberMeTokenExpirationMs, // 90 days
            @Value("${jwt.issuer:identity-service}") String issuer) {
        
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpirationMs = accessTokenExpirationMs;
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;
        this.rememberMeTokenExpirationMs = rememberMeTokenExpirationMs;
        this.issuer = issuer;
    }
    
    /**
     * Generate access token with user and persona information.
     */
    public String generateAccessToken(User user, Persona activePersona) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId().toString());
        claims.put("email", user.getEmail());
        claims.put("displayName", user.getDisplayName());
        claims.put("emailVerified", user.isEmailVerified());
        claims.put("personaId", activePersona.getId().toString());
        claims.put("personaName", activePersona.getName());
        claims.put("personaType", activePersona.getType().name());
        claims.put("type", "access");
        
        return createToken(claims, user.getUsername(), accessTokenExpirationMs);
    }
    
    /**
     * Generate refresh token for user.
     */
    public String generateRefreshToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId().toString());
        claims.put("type", "refresh");
        
        return createToken(claims, user.getUsername(), refreshTokenExpirationMs);
    }
    
    /**
     * Generate long-lived refresh token for remember me functionality.
     */
    public String generateLongLivedRefreshToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId().toString());
        claims.put("type", "refresh");
        claims.put("rememberMe", true);
        
        return createToken(claims, user.getUsername(), rememberMeTokenExpirationMs);
    }
    
    /**
     * Create JWT token with claims.
     */
    private String createToken(Map<String, Object> claims, String subject, long expirationMs) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMs);
        
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .setIssuer(issuer)
                .signWith(secretKey, SignatureAlgorithm.HS512)
                .compact();
    }
    
    /**
     * Extract username from token.
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }
    
    /**
     * Extract user ID from token.
     */
    public UUID extractUserId(String token) {
        String userId = extractClaim(token, claims -> claims.get("userId", String.class));
        return UUID.fromString(userId);
    }
    
    /**
     * Extract persona ID from token.
     */
    public UUID extractPersonaId(String token) {
        String personaId = extractClaim(token, claims -> claims.get("personaId", String.class));
        return personaId != null ? UUID.fromString(personaId) : null;
    }
    
    /**
     * Extract email from token.
     */
    public String extractEmail(String token) {
        return extractClaim(token, claims -> claims.get("email", String.class));
    }
    
    /**
     * Extract expiration date from token.
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }
    
    /**
     * Extract issued at date from token.
     */
    public Date extractIssuedAt(String token) {
        return extractClaim(token, Claims::getIssuedAt);
    }
    
    /**
     * Extract specific claim from token.
     */
    public <T> T extractClaim(String token, ClaimsResolver<T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.resolve(claims);
    }
    
    /**
     * Extract all claims from token.
     */
    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
    
    /**
     * Validate token.
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (MalformedJwtException ex) {
            log.error("Invalid JWT token");
        } catch (ExpiredJwtException ex) {
            log.error("Expired JWT token");
        } catch (UnsupportedJwtException ex) {
            log.error("Unsupported JWT token");
        } catch (IllegalArgumentException ex) {
            log.error("JWT claims string is empty");
        } catch (SignatureException ex) {
            log.error("Invalid JWT signature");
        }
        return false;
    }
    
    /**
     * Check if token is expired.
     */
    public boolean isTokenExpired(String token) {
        try {
            Date expiration = extractExpiration(token);
            return expiration.before(new Date());
        } catch (Exception e) {
            return true;
        }
    }
    
    /**
     * Get access token expiration in seconds.
     */
    public long getAccessTokenExpirationSeconds() {
        return accessTokenExpirationMs / 1000;
    }
    
    /**
     * Get user ID from token.
     */
    public String getUserIdFromToken(String token) {
        return extractClaim(token, claims -> claims.get("userId", String.class));
    }
    
    /**
     * Get persona ID from token.
     */
    public String getPersonaIdFromToken(String token) {
        return extractClaim(token, claims -> claims.get("personaId", String.class));
    }
    
    /**
     * Get expiration from token as LocalDateTime.
     */
    public LocalDateTime getExpirationFromToken(String token) {
        Date expiration = extractExpiration(token);
        return LocalDateTime.ofInstant(expiration.toInstant(), ZoneOffset.UTC);
    }
    
    /**
     * Get claims from token.
     */
    public Claims getClaimsFromToken(String token) {
        return extractAllClaims(token);
    }
    
    /**
     * Functional interface for claims resolution.
     */
    @FunctionalInterface
    public interface ClaimsResolver<T> {
        T resolve(Claims claims);
    }
}