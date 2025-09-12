package com.focushive.api.security;

import com.focushive.user.entity.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Slf4j
@Component
public class JwtTokenProvider {

    private final SecretKey key;
    private final Long expiration;
    private final Long refreshExpiration;

    public JwtTokenProvider(@Value("${spring.security.jwt.secret}") String secret,
                           @Value("${spring.security.jwt.expiration}") Long expiration) {
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
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
        this.expiration = expiration;
        this.refreshExpiration = expiration * 2; // Refresh tokens last twice as long
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

}