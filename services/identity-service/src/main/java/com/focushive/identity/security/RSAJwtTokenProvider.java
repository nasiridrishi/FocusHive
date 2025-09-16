package com.focushive.identity.security;

import com.focushive.identity.dto.JwtTokenClaims;
import com.focushive.identity.entity.User;
import com.focushive.identity.entity.Persona;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Enhanced JWT token provider with RSA signing support.
 * Production-ready implementation with key rotation support.
 */
@Slf4j
@Component
@Primary
@Profile("!performance")
public class RSAJwtTokenProvider extends JwtTokenProvider {

    // RSA Key Management
    private final Map<String, KeyPair> rsaKeyPairs = new ConcurrentHashMap<>();
    private String activeKeyId;
    private SignatureAlgorithm signingAlgorithm = SignatureAlgorithm.RS256;

    // Configuration
    private final String issuer;
    private final long tokenExpiration;
    private final long refreshTokenExpiration;
    private final long rememberMeExpiration;
    private final boolean useRSA;
    private final ResourceLoader resourceLoader;

    // Legacy HMAC support for migration
    private final Key hmacKey;

    /**
     * Generate a secure placeholder secret for RSA mode.
     * This is only used when RSA is enabled and no HMAC secret is provided.
     */
    private static String generateSecurePlaceholderSecret() {
        return "RSA_MODE_PLACEHOLDER_" + UUID.randomUUID().toString().replace("-", "") +
               UUID.randomUUID().toString().replace("-", "").substring(0, 11);
    }

    public RSAJwtTokenProvider(
            @Value("${jwt.secret:}") String secret,
            @Value("${jwt.access-token-expiration-ms:3600000}") long tokenExpiration,
            @Value("${jwt.refresh-token-expiration-ms:2592000000}") long refreshTokenExpiration,
            @Value("${jwt.remember-me-expiration-ms:7776000000}") long rememberMeTokenExpiration,
            @Value("${jwt.issuer:http://localhost:8081/identity}") String issuer,
            @Value("${jwt.use-rsa:true}") boolean useRSA,
            @Value("${jwt.rsa.private-key-path:classpath:keys/jwt-private.pem}") String privateKeyPath,
            @Value("${jwt.rsa.public-key-path:classpath:keys/jwt-public.pem}") String publicKeyPath,
            @Value("${jwt.rsa.key-id:focushive-2025-01}") String keyId,
            ResourceLoader resourceLoader) {

        // For RSA mode, we don't need HMAC secret, but parent class requires it
        // Use a secure random placeholder that passes validation
        super(
            (useRSA && (secret == null || secret.isEmpty()))
                ? generateSecurePlaceholderSecret()
                : secret,
            tokenExpiration, refreshTokenExpiration, rememberMeTokenExpiration, issuer);

        this.issuer = issuer;
        this.tokenExpiration = tokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
        this.rememberMeExpiration = rememberMeTokenExpiration;
        this.useRSA = useRSA;
        this.resourceLoader = resourceLoader;
        this.activeKeyId = keyId;

        // Keep HMAC for backward compatibility during migration
        if (secret != null && !secret.isEmpty() && secret.length() >= 32) {
            this.hmacKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        } else if (!useRSA) {
            throw new IllegalArgumentException("JWT secret required when not using RSA");
        } else {
            this.hmacKey = null;
        }

        if (useRSA) {
            initializeRSAKeys(privateKeyPath, publicKeyPath, keyId);
        }
    }

    @PostConstruct
    public void init() {
        if (useRSA) {
            log.info("JWT Provider initialized with RSA signing (Algorithm: {})", signingAlgorithm);
            log.info("Active key ID: {}", activeKeyId);
        } else {
            log.warn("JWT Provider using HMAC signing - NOT RECOMMENDED FOR PRODUCTION");
        }
    }

    /**
     * Initialize RSA keys from files or generate new ones.
     */
    private void initializeRSAKeys(String privateKeyPath, String publicKeyPath, String keyId) {
        try {
            KeyPair keyPair = null;

            // Try to load existing keys
            if (privateKeyPath != null && publicKeyPath != null) {
                try {
                    Resource privateResource = resourceLoader.getResource(privateKeyPath);
                    Resource publicResource = resourceLoader.getResource(publicKeyPath);

                    if (privateResource.exists() && publicResource.exists()) {
                        String privateKeyPEM = StreamUtils.copyToString(
                            privateResource.getInputStream(), StandardCharsets.UTF_8);
                        String publicKeyPEM = StreamUtils.copyToString(
                            publicResource.getInputStream(), StandardCharsets.UTF_8);

                        PrivateKey privateKey = loadPrivateKeyFromPEM(privateKeyPEM);
                        PublicKey publicKey = loadPublicKeyFromPEM(publicKeyPEM);
                        keyPair = new KeyPair(publicKey, privateKey);
                        log.info("Loaded RSA keys from files: {} and {}", privateKeyPath, publicKeyPath);
                    }
                } catch (IOException e) {
                    log.warn("Could not load RSA keys from files, will generate new ones: {}", e.getMessage());
                }
            }

            // Generate new keys if not loaded
            if (keyPair == null) {
                keyPair = generateRSAKeyPair();
                log.info("Generated new RSA key pair with 2048-bit key size");

                // Log the keys for development (remove in production)
                if (log.isDebugEnabled()) {
                    log.debug("Private Key (PEM):\n{}", convertPrivateKeyToPEM(keyPair.getPrivate()));
                    log.debug("Public Key (PEM):\n{}", convertPublicKeyToPEM(keyPair.getPublic()));
                }
            }

            rsaKeyPairs.put(keyId, keyPair);
            this.activeKeyId = keyId;

        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize RSA keys", e);
        }
    }

    /**
     * Generate new RSA key pair.
     */
    private KeyPair generateRSAKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048); // 2048-bit key size for production
        return keyGen.generateKeyPair();
    }

    /**
     * Generate access token with user and persona information using RSA.
     * Overrides the parent method to use RSA instead of HMAC.
     */
    @Override
    public String generateAccessToken(User user, Persona activePersona) {
        if (!useRSA) {
            return super.generateAccessToken(user, activePersona);
        }

        KeyPair keyPair = rsaKeyPairs.get(activeKeyId);
        if (keyPair == null) {
            throw new IllegalStateException("No RSA key pair available for signing");
        }

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId().toString());
        claims.put("email", user.getEmail());
        claims.put("displayName", user.getUsername());
        claims.put("emailVerified", user.isEmailVerified());
        claims.put("type", "access");

        // Add persona information
        if (activePersona != null) {
            claims.put("personaId", activePersona.getId().toString());
            claims.put("personaName", activePersona.getName());
            claims.put("personaType", activePersona.getType().toString());
        }

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + tokenExpiration);

        return Jwts.builder()
            .claims(claims)
            .subject(user.getUsername())
            .id(UUID.randomUUID().toString())
            .issuedAt(now)
            .expiration(expiryDate)
            .issuer(issuer)
            .setHeaderParam("kid", activeKeyId)
            .signWith(keyPair.getPrivate(), signingAlgorithm)
            .compact();
    }

    /**
     * Generate refresh token using RSA.
     * Overrides the parent method to use RSA instead of HMAC.
     */
    @Override
    public String generateRefreshToken(User user) {
        if (!useRSA) {
            return super.generateRefreshToken(user);
        }

        KeyPair keyPair = rsaKeyPairs.get(activeKeyId);
        if (keyPair == null) {
            throw new IllegalStateException("No RSA key pair available for signing");
        }

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId().toString());
        claims.put("type", "refresh");

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshTokenExpiration);

        return Jwts.builder()
            .claims(claims)
            .subject(user.getUsername())
            .id(UUID.randomUUID().toString())
            .issuedAt(now)
            .expiration(expiryDate)
            .issuer(issuer)
            .setHeaderParam("kid", activeKeyId)
            .signWith(keyPair.getPrivate(), signingAlgorithm)
            .compact();
    }

    /**
     * Generate long-lived refresh token for "remember me" using RSA.
     * Overrides the parent method to use RSA instead of HMAC.
     */
    @Override
    public String generateLongLivedRefreshToken(User user) {
        if (!useRSA) {
            return super.generateLongLivedRefreshToken(user);
        }

        KeyPair keyPair = rsaKeyPairs.get(activeKeyId);
        if (keyPair == null) {
            throw new IllegalStateException("No RSA key pair available for signing");
        }

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId().toString());
        claims.put("type", "refresh");
        claims.put("longLived", true);

        // Use remember-me expiration from field
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + rememberMeExpiration);

        return Jwts.builder()
            .claims(claims)
            .subject(user.getUsername())
            .id(UUID.randomUUID().toString())
            .issuedAt(now)
            .expiration(expiryDate)
            .issuer(issuer)
            .setHeaderParam("kid", activeKeyId)
            .signWith(keyPair.getPrivate(), signingAlgorithm)
            .compact();
    }

    /**
     * Generate token with RSA signing for User.
     */
    public String generateToken(User user, Persona activePersona) {
        if (useRSA) {
            return generateRSAToken(user, activePersona);
        } else {
            // Fall back to HMAC for backward compatibility
            Map<String, Object> claims = new HashMap<>();
            claims.put("userId", user.getId().toString());
            claims.put("username", user.getUsername());
            claims.put("email", user.getEmail());
            claims.put("roles", user.getAuthorities().stream()
                .map(auth -> auth.getAuthority())
                .collect(Collectors.toSet()));
            if (activePersona != null) {
                claims.put("personaId", activePersona.getId().toString());
                claims.put("personaType", activePersona.getType().toString());
            }
            return super.generateToken(user.getId().toString(), claims,
                (int)(tokenExpiration / 1000));
        }
    }

    /**
     * Generate OAuth2 token with custom claims and expiration.
     * This is the method called by ServiceJwtTokenProvider for service-to-service auth.
     */
    @Override
    public String generateToken(String subject, Map<String, Object> customClaims, int expirationSeconds) {
        if (!useRSA) {
            // Fall back to HMAC for backward compatibility
            return super.generateToken(subject, customClaims, expirationSeconds);
        }

        KeyPair keyPair = rsaKeyPairs.get(activeKeyId);
        if (keyPair == null) {
            throw new IllegalStateException("No RSA key pair available for signing");
        }

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + (expirationSeconds * 1000L));

        return Jwts.builder()
            .claims(customClaims)
            .subject(subject)
            .id(UUID.randomUUID().toString())
            .issuedAt(now)
            .expiration(expiryDate)
            .issuer(issuer)
            .setHeaderParam("kid", activeKeyId)
            .signWith(keyPair.getPrivate(), signingAlgorithm)
            .compact();
    }

    /**
     * Generate OAuth2 token with custom claims.
     */
    public String generateToken(JwtTokenClaims claims) {
        if (useRSA) {
            return generateRSAToken(claims);
        } else {
            // Fall back to HMAC for backward compatibility
            Map<String, Object> claimsMap = new HashMap<>();
            claimsMap.put("userId", claims.getUserId());
            claimsMap.put("username", claims.getUsername());
            claimsMap.put("email", claims.getEmail());
            claimsMap.put("roles", claims.getRoles());
            claimsMap.put("scopes", claims.getScopes());
            claimsMap.put("clientId", claims.getClientId());
            return super.generateToken(claims.getSubject(), claimsMap,
                (int)(tokenExpiration / 1000));
        }
    }

    /**
     * Generate RSA signed token.
     */
    private String generateRSAToken(User user, Persona activePersona) {
        KeyPair keyPair = rsaKeyPairs.get(activeKeyId);
        if (keyPair == null) {
            throw new IllegalStateException("No RSA key pair available for signing");
        }

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId().toString());
        claims.put("username", user.getUsername());
        claims.put("email", user.getEmail());
        claims.put("roles", user.getAuthorities().stream()
            .map(auth -> auth.getAuthority())
            .collect(Collectors.toSet()));

        if (activePersona != null) {
            claims.put("personaId", activePersona.getId().toString());
            claims.put("personaType", activePersona.getType().toString());
        }

        return Jwts.builder()
            .setClaims(claims)
            .setSubject(user.getId().toString())
            .setIssuer(issuer)
            .setIssuedAt(new Date())
            .setExpiration(Date.from(Instant.now().plus(tokenExpiration, ChronoUnit.MILLIS)))
            .setHeaderParam("kid", activeKeyId)
            .signWith(keyPair.getPrivate(), signingAlgorithm)
            .compact();
    }

    /**
     * Generate RSA signed token from claims.
     */
    private String generateRSAToken(JwtTokenClaims claims) {
        KeyPair keyPair = rsaKeyPairs.get(activeKeyId);
        if (keyPair == null) {
            throw new IllegalStateException("No RSA key pair available for signing");
        }

        Map<String, Object> claimsMap = new HashMap<>();
        if (claims.getUserId() != null) claimsMap.put("userId", claims.getUserId());
        if (claims.getUsername() != null) claimsMap.put("username", claims.getUsername());
        if (claims.getEmail() != null) claimsMap.put("email", claims.getEmail());
        if (claims.getRoles() != null) claimsMap.put("roles", claims.getRoles());
        if (claims.getScopes() != null) claimsMap.put("scopes", claims.getScopes());
        if (claims.getClientId() != null) claimsMap.put("clientId", claims.getClientId());
        if (claims.getPersonaId() != null) claimsMap.put("personaId", claims.getPersonaId());

        return Jwts.builder()
            .setClaims(claimsMap)
            .setSubject(claims.getSubject())
            .setIssuer(issuer)
            .setAudience(claims.getAudience())
            .setIssuedAt(new Date())
            .setExpiration(Date.from(Instant.now().plus(tokenExpiration, ChronoUnit.MILLIS)))
            .setId(UUID.randomUUID().toString())
            .setHeaderParam("kid", activeKeyId)
            .signWith(keyPair.getPrivate(), signingAlgorithm)
            .compact();
    }

    /**
     * Validate token (supports both RSA and HMAC).
     */
    @Override
    public boolean validateToken(String token) {
        if (token == null || token.isEmpty()) {
            return false;
        }

        try {
            // First, try to parse the header to determine the algorithm
            String[] chunks = token.split("\\.");
            if (chunks.length != 3) {
                return false;
            }

            // Decode header
            String header = new String(Base64.getUrlDecoder().decode(chunks[0]));

            if (useRSA || header.contains("RS256") || header.contains("RS384") || header.contains("RS512")) {
                // Validate with RSA
                return validateRSAToken(token);
            } else {
                // Fall back to HMAC validation
                return super.validateToken(token);
            }
        } catch (Exception e) {
            log.error("Token validation failed", e);
            return false;
        }
    }

    /**
     * Validate RSA signed token.
     */
    private boolean validateRSAToken(String token) {
        try {
            // Try each available public key (for key rotation support)
            for (Map.Entry<String, KeyPair> entry : rsaKeyPairs.entrySet()) {
                try {
                    Jwts.parser()
                        .verifyWith(entry.getValue().getPublic())
                        .build()
                        .parseSignedClaims(token);
                    return true; // Token is valid
                } catch (SignatureException | MalformedJwtException e) {
                    // Try next key
                    continue;
                }
            }
            return false; // No key could validate the token
        } catch (ExpiredJwtException e) {
            log.debug("Token expired: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("Token validation error", e);
            return false;
        }
    }

    /**
     * Get JWKS for public key distribution.
     */
    public List<Map<String, Object>> getJWKS() {
        List<Map<String, Object>> keys = new ArrayList<>();

        if (!useRSA) {
            // Cannot expose HMAC keys
            log.warn("JWKS requested but using HMAC - cannot expose symmetric keys");
            return keys;
        }

        for (Map.Entry<String, KeyPair> entry : rsaKeyPairs.entrySet()) {
            PublicKey publicKey = entry.getValue().getPublic();
            if (publicKey instanceof java.security.interfaces.RSAPublicKey) {
                java.security.interfaces.RSAPublicKey rsaKey =
                    (java.security.interfaces.RSAPublicKey) publicKey;

                Map<String, Object> key = new LinkedHashMap<>();
                key.put("kty", "RSA");
                key.put("use", "sig");
                key.put("key_ops", Arrays.asList("sign", "verify"));
                key.put("alg", signingAlgorithm.getValue());
                key.put("kid", entry.getKey());

                // RSA specific parameters
                key.put("n", Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(rsaKey.getModulus().toByteArray()));
                key.put("e", Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(rsaKey.getPublicExponent().toByteArray()));

                keys.add(key);
            }
        }

        return keys;
    }

    /**
     * Add a new key pair for rotation.
     */
    public void addKeyPair(String keyId, KeyPair keyPair) {
        rsaKeyPairs.put(keyId, keyPair);
        log.info("Added new RSA key pair with ID: {}", keyId);
    }

    /**
     * Rotate to a new active key.
     */
    public void rotateActiveKey(String newKeyId) {
        if (!rsaKeyPairs.containsKey(newKeyId)) {
            throw new IllegalArgumentException("Key ID not found: " + newKeyId);
        }
        this.activeKeyId = newKeyId;
        log.info("Rotated active key to: {}", newKeyId);
    }

    /**
     * Remove an old key (after rotation grace period).
     */
    public void removeKey(String keyId) {
        if (keyId.equals(activeKeyId)) {
            throw new IllegalArgumentException("Cannot remove active key");
        }
        rsaKeyPairs.remove(keyId);
        log.info("Removed RSA key: {}", keyId);
    }

    // Helper methods for PEM conversion
    private String convertPrivateKeyToPEM(PrivateKey privateKey) {
        Base64.Encoder encoder = Base64.getMimeEncoder(64, "\n".getBytes());
        String encoded = encoder.encodeToString(privateKey.getEncoded());
        return "-----BEGIN PRIVATE KEY-----\n" + encoded + "\n-----END PRIVATE KEY-----";
    }

    private String convertPublicKeyToPEM(PublicKey publicKey) {
        Base64.Encoder encoder = Base64.getMimeEncoder(64, "\n".getBytes());
        String encoded = encoder.encodeToString(publicKey.getEncoded());
        return "-----BEGIN PUBLIC KEY-----\n" + encoded + "\n-----END PUBLIC KEY-----";
    }

    private PrivateKey loadPrivateKeyFromPEM(String pem) throws Exception {
        String privateKeyPEM = pem
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replaceAll("\\s", "");

        byte[] encoded = Base64.getDecoder().decode(privateKeyPEM);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encoded);
        return keyFactory.generatePrivate(keySpec);
    }

    private PublicKey loadPublicKeyFromPEM(String pem) throws Exception {
        String publicKeyPEM = pem
            .replace("-----BEGIN PUBLIC KEY-----", "")
            .replace("-----END PUBLIC KEY-----", "")
            .replaceAll("\\s", "");

        byte[] encoded = Base64.getDecoder().decode(publicKeyPEM);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(encoded);
        return keyFactory.generatePublic(keySpec);
    }

    /**
     * Get active key ID for monitoring.
     */
    public String getActiveKeyId() {
        return activeKeyId;
    }

    /**
     * Get all key IDs for monitoring.
     */
    public Set<String> getAllKeyIds() {
        return new HashSet<>(rsaKeyPairs.keySet());
    }

    /**
     * Extract all claims from RSA-signed token using the correct RSA public key.
     * Overrides the parent method to prevent HMAC secret usage on RSA tokens.
     */
    @Override
    public Claims extractAllClaims(String token) {
        if (!useRSA) {
            // Fall back to HMAC for backward compatibility
            return super.extractAllClaims(token);
        }
        
        // Try each available RSA public key (for key rotation support)
        for (Map.Entry<String, KeyPair> entry : rsaKeyPairs.entrySet()) {
            try {
                return Jwts.parser()
                    .verifyWith(entry.getValue().getPublic())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            } catch (io.jsonwebtoken.security.SignatureException | MalformedJwtException e) {
                // Try next key
                continue;
            }
        }
        
        // If no key could verify the token, throw appropriate exception
        throw new io.jsonwebtoken.security.InvalidKeyException(
            "No RSA public key could verify the token signature. Available keys: " + 
            rsaKeyPairs.keySet());
    }

    /**
     * Extract specific claim from RSA token using RSA validation.
     */
    @Override
    public <T> T extractClaim(String token, ClaimsResolver<T> claimsResolver) {
        final Claims claims = extractAllClaims(token); // This will now use RSA validation
        return claimsResolver.resolve(claims);
    }

    /**
     * Extract user ID from RSA token using RSA validation.
     */
    @Override
    public UUID extractUserId(String token) {
        String userId = extractClaim(token, claims -> claims.get("userId", String.class));
        return UUID.fromString(userId);
    }

    /**
     * Extract persona ID from RSA token using RSA validation.
     */
    @Override
    public UUID extractPersonaId(String token) {
        String personaId = extractClaim(token, claims -> claims.get("personaId", String.class));
        return personaId != null ? UUID.fromString(personaId) : null;
    }

    /**
     * Extract email from RSA token using RSA validation.
     */
    @Override
    public String extractEmail(String token) {
        return extractClaim(token, claims -> claims.get("email", String.class));
    }

    /**
     * Extract expiration date from RSA token using RSA validation.
     */
    @Override
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Extract issued at date from RSA token using RSA validation.
     */
    @Override
    public Date extractIssuedAt(String token) {
        return extractClaim(token, Claims::getIssuedAt);
    }

    /**
     * Extract username from RSA token using RSA validation.
     */
    @Override
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Check if RSA token is expired using RSA validation.
     */
    @Override
    public boolean isTokenExpired(String token) {
        try {
            Date expiration = extractExpiration(token); // Uses RSA validation
            return expiration.before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * Get user ID from RSA token using RSA validation.
     */
    @Override
    public String getUserIdFromToken(String token) {
        return extractClaim(token, claims -> claims.get("userId", String.class));
    }

    /**
     * Get persona ID from RSA token using RSA validation.
     */
    @Override
    public String getPersonaIdFromToken(String token) {
        return extractClaim(token, claims -> claims.get("personaId", String.class));
    }

    /**
     * Get claims from RSA token using RSA validation.
     */
    @Override
    public Claims getClaimsFromToken(String token) {
        return extractAllClaims(token); // Uses RSA validation
    }
}
