package com.focushive.api.security;

import com.focushive.user.entity.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import javax.crypto.SecretKey;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
@Slf4j
public class JwtTokenProvider {

    private final SecretKey key;
    private final Long expiration;
    private final Long refreshExpiration;
    private final Optional<JwtTokenBlacklistService> blacklistService;
    private final Optional<JwtCacheService> cacheService;

    // Primary constructor with blacklist and cache support
    public JwtTokenProvider(String secret,
                           Long expiration,
                           Optional<JwtTokenBlacklistService> blacklistService,
                           Optional<JwtCacheService> cacheService) {
        this.blacklistService = blacklistService;
        this.cacheService = cacheService;
        this.key = initializeSecretKey(secret);
        this.expiration = expiration;
        this.refreshExpiration = expiration * 2;
    }

    // Constructor with blacklist support (for backward compatibility)
    public JwtTokenProvider(String secret,
                           Long expiration,
                           Optional<JwtTokenBlacklistService> blacklistService) {
        this(secret, expiration, blacklistService, Optional.empty());
    }

    // Backward compatibility constructor (existing tests)
    public JwtTokenProvider(String secret,
                           Long expiration) {
        this(secret, expiration, Optional.empty());
    }

    private SecretKey initializeSecretKey(String secret) {
        // Validate JWT secret strength - CRITICAL SECURITY REQUIREMENT
        if (secret == null || secret.trim().isEmpty()) {
            throw new IllegalArgumentException("JWT_SECRET environment variable must be set and not empty");
        }
        
        // JWT secret must be at least 256 bits (32 characters) for HS256/HS512 security
        if (secret.length() < 32) {
            throw new IllegalArgumentException("JWT secret must be at least 32 characters (256 bits) long for security. Current length: " + secret.length());
        }
        
        // Check for common weak patterns that should not be used in production
        if (secret.contains("your-super-secret") || 
            secret.contains("changeme") || 
            secret.contains("secret") ||
            secret.contains("password") ||
            secret.equals("test")) {
            throw new IllegalArgumentException("JWT secret contains insecure patterns. Use a cryptographically secure random string.");
        }
        
        log.info("JWT secret validation passed - length: {} characters", secret.length());
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    public String generateToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("email", user.getEmail());
        claims.put("role", user.getRole().name());
        return createToken(claims, user.getUsername());
    }

    public String generateToken(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return generateToken(user);
    }

    public String generateRefreshToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("type", "refresh");
        return createToken(claims, user.getUsername(), refreshExpiration);
    }

    private String createToken(Map<String, Object> claims, String subject) {
        return createToken(claims, subject, expiration);
    }

    private String createToken(Map<String, Object> claims, String subject, Long expiration) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public String extractUserId(String token) {
        return extractClaim(token, claims -> claims.get("userId", String.class));
    }

    public String extractEmail(String token) {
        return extractClaim(token, claims -> claims.get("email", String.class));
    }

    public String extractRole(String token) {
        return extractClaim(token, claims -> claims.get("role", String.class));
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public List<SimpleGrantedAuthority> extractAuthorities(String token) {
        String role = extractRole(token);
        return List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }

    public Boolean isTokenExpired(String token) {
        try {
            return extractExpiration(token).before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        }
    }

    public Boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token);
            return !isTokenExpired(token);
        } catch (JwtException | IllegalArgumentException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    public Boolean validateToken(String token, String username) {
        final String tokenUsername = extractUsername(token);
        return (tokenUsername.equals(username) && !isTokenExpired(token));
    }

    public Boolean canTokenBeRefreshed(String token) {
        return !isTokenExpired(token);
    }

    public String refreshToken(String token) {
        if (!canTokenBeRefreshed(token)) {
            throw new IllegalArgumentException("Token cannot be refreshed");
        }

        Claims claims = extractAllClaims(token);
        
        // Create new token with updated timestamps
        return Jwts.builder()
                .claims(claims)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(key)
                .compact();
    }

    // ========== ENHANCED METHODS WITH BLACKLIST INTEGRATION ==========

    /**
     * Validate token with blacklist checking and caching.
     * This method combines standard JWT validation with blacklist verification and performance caching.
     *
     * @param token The JWT token to validate
     * @return true if token is valid and not blacklisted, false otherwise
     */
    public Boolean validateTokenWithBlacklist(String token) {
        try {
            // Check cache first for performance
            if (cacheService.isPresent() && cacheService.get().isTokenValidationCached(token)) {
                log.debug("Token validation found in cache");
                return true;
            }

            // Check if token is blacklisted (fast Redis operation)
            if (blacklistService.isPresent() && blacklistService.get().isTokenBlacklisted(token)) {
                log.debug("Token is blacklisted");
                return false;
            }

            // Perform standard JWT validation
            boolean isValid = validateToken(token);

            // Cache successful validation for performance
            if (isValid && cacheService.isPresent()) {
                cacheService.get().cacheValidToken(token);
            }

            return isValid;
        } catch (Exception e) {
            log.error("Error during token validation with blacklist", e);
            // Fail-safe: reject token on any error for security
            return false;
        }
    }

    /**
     * Invalidate a token by adding it to the blacklist and removing from cache.
     * The token will remain blacklisted until its natural expiration.
     *
     * @param token The JWT token to invalidate
     */
    public void invalidateToken(String token) {
        if (blacklistService.isPresent()) {
            try {
                // Calculate TTL based on token expiration
                Date expiration = extractExpiration(token);
                long ttlMs = expiration.getTime() - System.currentTimeMillis();

                if (ttlMs > 0) {
                    Duration ttl = Duration.ofMillis(ttlMs);
                    blacklistService.get().blacklistToken(token, ttl);
                    log.debug("Token invalidated and added to blacklist");

                    // Also invalidate from cache
                    if (cacheService.isPresent()) {
                        cacheService.get().invalidateTokenCache(token);
                    }
                } else {
                    log.debug("Token already expired, not adding to blacklist");
                }
            } catch (Exception e) {
                log.error("Failed to invalidate token", e);
            }
        } else {
            log.warn("Cannot invalidate token - blacklist service not available");
        }
    }

    /**
     * Extract JTI (JWT ID) claim from token.
     * For tokens that don't have JTI, this generates a deterministic ID based on token hash.
     *
     * @param token The JWT token
     * @return JTI if present, otherwise a hash-based identifier
     */
    public String extractJti(String token) {
        try {
            Claims claims = extractAllClaims(token);
            String jti = claims.getId();

            if (jti != null) {
                return jti;
            } else {
                // Generate deterministic JTI from token hash for backward compatibility
                return generateTokenHash(token);
            }
        } catch (Exception e) {
            log.error("Error extracting JTI from token", e);
            return generateTokenHash(token);
        }
    }

    /**
     * Generate a deterministic hash of the token for use as JTI.
     * This provides backward compatibility for tokens without explicit JTI claims.
     *
     * @param token The JWT token
     * @return SHA-256 hash of the token (first 16 characters)
     */
    private String generateTokenHash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes());
            StringBuilder hexString = new StringBuilder();

            for (int i = 0; i < Math.min(hash.length, 8); i++) {
                String hex = Integer.toHexString(0xff & hash[i]);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }

            return hexString.toString();
        } catch (Exception e) {
            log.error("Error generating token hash", e);
            return UUID.randomUUID().toString().substring(0, 16);
        }
    }

    /**
     * Enhanced token generation with JTI (JWT ID) for blacklist tracking.
     * This method adds a unique identifier to each token for precise blacklist management.
     *
     * @param user The user for whom to generate the token
     * @return JWT token with JTI claim
     */
    public String generateTokenWithJti(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("email", user.getEmail());
        claims.put("role", user.getRole().name());

        // Add JTI (JWT ID) for blacklist tracking
        String jti = UUID.randomUUID().toString();

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .claims(claims)
                .subject(user.getUsername())
                .id(jti) // JTI claim for blacklist tracking
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
    }

    /**
     * Check if blacklist service is available.
     * Useful for conditional logic in services that depend on blacklist functionality.
     *
     * @return true if blacklist service is configured and available
     */
    public boolean isBlacklistServiceAvailable() {
        return blacklistService.isPresent();
    }

}