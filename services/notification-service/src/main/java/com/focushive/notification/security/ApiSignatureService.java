package com.focushive.notification.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for generating and verifying API request signatures using HMAC-SHA256.
 *
 * <p>This service implements request signing to ensure API request authenticity and integrity.
 * It uses HMAC-SHA256 for signature generation with timestamp and nonce validation to prevent
 * replay attacks. The service supports multiple API keys with different access levels.</p>
 *
 * <h2>Security Features:</h2>
 * <ul>
 *   <li>HMAC-SHA256 signature generation for request integrity</li>
 *   <li>Timestamp validation to prevent replay attacks (5-minute window)</li>
 *   <li>Nonce tracking to prevent request replay</li>
 *   <li>Constant-time string comparison to prevent timing attacks</li>
 *   <li>SHA-256 hashing for request body integrity</li>
 * </ul>
 *
 * <h2>Signature Components:</h2>
 * <ul>
 *   <li><b>HTTP Method:</b> GET, POST, PUT, DELETE, etc.</li>
 *   <li><b>Request Path:</b> API endpoint path</li>
 *   <li><b>Timestamp:</b> Unix epoch timestamp of request</li>
 *   <li><b>Nonce:</b> Unique identifier for request</li>
 *   <li><b>API Key:</b> Client's API key identifier</li>
 *   <li><b>Headers:</b> Canonicalized X-API-* headers</li>
 *   <li><b>Body Hash:</b> SHA-256 hash of request body</li>
 * </ul>
 *
 * <h2>Usage Example:</h2>
 * <pre>{@code
 * // Generate signature for request
 * ApiSignature signature = apiSignatureService.generateSignature(
 *     "client-001", "POST", "/api/notifications",
 *     headers, requestBody
 * );
 *
 * // Verify incoming request signature
 * boolean isValid = apiSignatureService.verifySignature(
 *     apiKey, signature, timestamp, nonce,
 *     method, path, headers, body
 * );
 * }</pre>
 *
 * @author FocusHive Security Team
 * @version 1.0
 * @since 1.0
 * @see ApiSignatureFilter
 * @see DataEncryptionService
 */
@Slf4j
@Service
public class ApiSignatureService {

    private static final String SIGNATURE_ALGORITHM = "HmacSHA256";
    private static final String HASH_ALGORITHM = "SHA-256";
    private static final long SIGNATURE_VALIDITY_SECONDS = 300; // 5 minutes

    @Value("${api.security.signature.enabled:true}")
    private boolean signatureEnabled;

    @Value("${api.security.signature.header:X-API-Signature}")
    private String signatureHeader;

    @Value("${api.security.signature.timestamp-header:X-API-Timestamp}")
    private String timestampHeader;

    @Value("${api.security.signature.key-header:X-API-Key}")
    private String apiKeyHeader;

    @Value("${api.security.signature.nonce-header:X-API-Nonce}")
    private String nonceHeader;

    // In production, these would be stored securely in a vault or database
    private final Map<String, String> apiKeys = new HashMap<>();
    private final Set<String> usedNonces = Collections.synchronizedSet(new HashSet<>());

    public ApiSignatureService() {
        // Initialize with some API keys (in production, load from secure storage)
        apiKeys.put("client-001", "secret-key-001-change-in-production");
        apiKeys.put("client-002", "secret-key-002-change-in-production");
        apiKeys.put("admin-key", "admin-secret-key-change-in-production");
    }

    /**
     * Generate a signature for an API request.
     */
    public ApiSignature generateSignature(String apiKey, String httpMethod, String path,
                                         Map<String, String> headers, String body) {
        if (!signatureEnabled) {
            return ApiSignature.disabled();
        }

        String apiSecret = apiKeys.get(apiKey);
        if (apiSecret == null) {
            throw new IllegalArgumentException("Invalid API key: " + apiKey);
        }

        long timestamp = Instant.now().getEpochSecond();
        String nonce = UUID.randomUUID().toString();

        // Build the string to sign
        String stringToSign = buildStringToSign(httpMethod, path, headers, body, timestamp, nonce, apiKey);

        // Generate the signature
        String signature = computeHmacSha256(stringToSign, apiSecret);

        return ApiSignature.builder()
            .apiKey(apiKey)
            .signature(signature)
            .timestamp(timestamp)
            .nonce(nonce)
            .build();
    }

    /**
     * Verify an API request signature.
     */
    public boolean verifySignature(String apiKey, String signature, long timestamp, String nonce,
                                  String httpMethod, String path, Map<String, String> headers, String body) {
        if (!signatureEnabled) {
            return true;
        }

        // Check if API key exists
        String apiSecret = apiKeys.get(apiKey);
        if (apiSecret == null) {
            log.warn("Invalid API key attempted: {}", apiKey);
            return false;
        }

        // Check timestamp validity (prevent replay attacks)
        long currentTime = Instant.now().getEpochSecond();
        if (Math.abs(currentTime - timestamp) > SIGNATURE_VALIDITY_SECONDS) {
            log.warn("Signature timestamp expired for API key: {}", apiKey);
            return false;
        }

        // Check nonce (prevent replay attacks)
        if (usedNonces.contains(nonce)) {
            log.warn("Nonce already used for API key: {}", apiKey);
            return false;
        }

        // Build the string that should have been signed
        String stringToSign = buildStringToSign(httpMethod, path, headers, body, timestamp, nonce, apiKey);

        // Compute the expected signature
        String expectedSignature = computeHmacSha256(stringToSign, apiSecret);

        // Verify the signature
        boolean isValid = constantTimeEquals(signature, expectedSignature);

        if (isValid) {
            // Add nonce to used set (with cleanup of old nonces)
            usedNonces.add(nonce);
            cleanupOldNonces();
        } else {
            log.warn("Invalid signature for API key: {}", apiKey);
        }

        return isValid;
    }

    /**
     * Build the string to sign for the request.
     */
    private String buildStringToSign(String httpMethod, String path, Map<String, String> headers,
                                    String body, long timestamp, String nonce, String apiKey) {
        StringBuilder stringBuilder = new StringBuilder();

        // Add HTTP method
        stringBuilder.append(httpMethod.toUpperCase()).append("\n");

        // Add path
        stringBuilder.append(path).append("\n");

        // Add timestamp
        stringBuilder.append(timestamp).append("\n");

        // Add nonce
        stringBuilder.append(nonce).append("\n");

        // Add API key
        stringBuilder.append(apiKey).append("\n");

        // Add canonical headers (sorted by key)
        String canonicalHeaders = headers.entrySet().stream()
            .filter(e -> e.getKey().toLowerCase().startsWith("x-api-"))
            .sorted(Map.Entry.comparingByKey())
            .map(e -> e.getKey().toLowerCase() + ":" + e.getValue().trim())
            .collect(Collectors.joining("\n"));

        if (!canonicalHeaders.isEmpty()) {
            stringBuilder.append(canonicalHeaders).append("\n");
        }

        // Add body hash (if present)
        if (body != null && !body.isEmpty()) {
            String bodyHash = computeSha256Hash(body);
            stringBuilder.append(bodyHash);
        }

        return stringBuilder.toString();
    }

    /**
     * Compute HMAC-SHA256 signature.
     */
    private String computeHmacSha256(String data, String secret) {
        try {
            Mac mac = Mac.getInstance(SIGNATURE_ALGORITHM);
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                secret.getBytes(StandardCharsets.UTF_8), SIGNATURE_ALGORITHM);
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("Error computing HMAC-SHA256", e);
            throw new RuntimeException("Failed to compute signature", e);
        }
    }

    /**
     * Compute SHA-256 hash of data.
     */
    private String computeSha256Hash(String data) {
        try {
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
            byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            log.error("Error computing SHA-256 hash", e);
            throw new RuntimeException("Failed to compute hash", e);
        }
    }

    /**
     * Constant time string comparison to prevent timing attacks.
     */
    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        if (a.length() != b.length()) {
            return false;
        }

        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }

    /**
     * Clean up old nonces to prevent memory leak.
     */
    private synchronized void cleanupOldNonces() {
        if (usedNonces.size() > 10000) {
            // Simple cleanup - in production, use a time-based expiry
            usedNonces.clear();
            log.info("Cleared nonce cache due to size limit");
        }
    }

    /**
     * Register a new API key.
     */
    public void registerApiKey(String apiKey, String apiSecret) {
        apiKeys.put(apiKey, apiSecret);
        log.info("Registered new API key: {}", apiKey);
    }

    /**
     * Revoke an API key.
     */
    public void revokeApiKey(String apiKey) {
        apiKeys.remove(apiKey);
        log.info("Revoked API key: {}", apiKey);
    }

    /**
     * Check if an API key is valid.
     */
    public boolean isValidApiKey(String apiKey) {
        return apiKeys.containsKey(apiKey);
    }

    /**
     * Get all registered API keys (for admin purposes).
     */
    public Set<String> getAllApiKeys() {
        return new HashSet<>(apiKeys.keySet());
    }

    /**
     * API Signature data class.
     */
    @lombok.Builder
    @lombok.Data
    public static class ApiSignature {
        private String apiKey;
        private String signature;
        private long timestamp;
        private String nonce;
        private boolean enabled;

        public static ApiSignature disabled() {
            return ApiSignature.builder()
                .enabled(false)
                .build();
        }
    }
}