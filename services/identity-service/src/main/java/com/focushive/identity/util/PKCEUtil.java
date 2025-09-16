package com.focushive.identity.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Utility class for PKCE (Proof Key for Code Exchange) operations.
 * Implements RFC 7636 for OAuth 2.0 public clients.
 *
 * PKCE prevents authorization code interception attacks by requiring
 * the client to generate a random code verifier and send its hash
 * (code challenge) during authorization, then send the original
 * verifier during token exchange.
 */
public class PKCEUtil {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int VERIFIER_MIN_LENGTH = 43;
    private static final int VERIFIER_MAX_LENGTH = 128;
    private static final int DEFAULT_VERIFIER_LENGTH = 64;

    /**
     * Generates a cryptographically random code verifier for PKCE.
     *
     * According to RFC 7636, the code verifier must be a high-entropy
     * cryptographic random string using unreserved characters [A-Z] / [a-z] / [0-9] / "-" / "." / "_" / "~"
     * with a minimum length of 43 characters and maximum of 128 characters.
     *
     * @return A base64url-encoded random string suitable for use as a PKCE code verifier
     */
    public static String generateCodeVerifier() {
        return generateCodeVerifier(DEFAULT_VERIFIER_LENGTH);
    }

    /**
     * Generates a cryptographically random code verifier with specified length.
     *
     * @param length The desired length of the code verifier (must be between 43 and 128)
     * @return A base64url-encoded random string suitable for use as a PKCE code verifier
     * @throws IllegalArgumentException if length is not between 43 and 128
     */
    public static String generateCodeVerifier(int length) {
        if (length < VERIFIER_MIN_LENGTH || length > VERIFIER_MAX_LENGTH) {
            throw new IllegalArgumentException(
                String.format("Code verifier length must be between %d and %d characters",
                    VERIFIER_MIN_LENGTH, VERIFIER_MAX_LENGTH)
            );
        }

        // Generate random bytes
        byte[] randomBytes = new byte[length];
        SECURE_RANDOM.nextBytes(randomBytes);

        // Encode to base64url without padding
        return Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(randomBytes)
            .substring(0, length);
    }

    /**
     * Generates a code challenge from the provided code verifier using SHA256.
     *
     * According to RFC 7636, the code challenge is created by taking the SHA256 hash
     * of the code verifier and base64url-encoding it.
     *
     * challenge = BASE64URL(SHA256(verifier))
     *
     * @param codeVerifier The code verifier to hash
     * @return The base64url-encoded SHA256 hash of the code verifier
     * @throws IllegalArgumentException if codeVerifier is null or empty
     */
    public static String generateCodeChallenge(String codeVerifier) {
        if (codeVerifier == null || codeVerifier.isEmpty()) {
            throw new IllegalArgumentException("Code verifier cannot be null or empty");
        }

        return generateCodeChallengeWithMethod(codeVerifier, "S256");
    }

    /**
     * Generates a code challenge using the specified method.
     *
     * @param codeVerifier The code verifier
     * @param method The challenge method ("plain" or "S256")
     * @return The code challenge
     * @throws IllegalArgumentException if method is not supported
     */
    public static String generateCodeChallengeWithMethod(String codeVerifier, String method) {
        if (codeVerifier == null || codeVerifier.isEmpty()) {
            throw new IllegalArgumentException("Code verifier cannot be null or empty");
        }

        if ("plain".equals(method)) {
            // Plain method: challenge = verifier
            return codeVerifier;
        } else if ("S256".equals(method)) {
            // S256 method: challenge = BASE64URL(SHA256(verifier))
            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                byte[] hash = digest.digest(codeVerifier.getBytes(StandardCharsets.US_ASCII));
                return Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(hash);
            } catch (NoSuchAlgorithmException e) {
                // This should never happen as SHA-256 is a standard algorithm
                throw new RuntimeException("SHA-256 algorithm not available", e);
            }
        } else {
            throw new IllegalArgumentException("Unsupported code challenge method: " + method);
        }
    }

    /**
     * Verifies that a code verifier matches a code challenge.
     *
     * @param codeVerifier The code verifier provided during token exchange
     * @param codeChallenge The code challenge provided during authorization
     * @param method The challenge method used ("plain" or "S256")
     * @return true if the verifier matches the challenge, false otherwise
     */
    public static boolean verifyCodeChallenge(String codeVerifier, String codeChallenge, String method) {
        if (codeVerifier == null || codeChallenge == null) {
            return false;
        }

        if ("plain".equals(method)) {
            return codeChallenge.equals(codeVerifier);
        } else if ("S256".equals(method)) {
            String calculatedChallenge = generateCodeChallenge(codeVerifier);
            return codeChallenge.equals(calculatedChallenge);
        }

        return false;
    }

    /**
     * Validates that a code verifier meets RFC 7636 requirements.
     *
     * @param codeVerifier The code verifier to validate
     * @return true if the verifier is valid, false otherwise
     */
    public static boolean isValidCodeVerifier(String codeVerifier) {
        if (codeVerifier == null) {
            return false;
        }

        int length = codeVerifier.length();
        if (length < VERIFIER_MIN_LENGTH || length > VERIFIER_MAX_LENGTH) {
            return false;
        }

        // Check that all characters are unreserved characters
        // [A-Z] / [a-z] / [0-9] / "-" / "." / "_" / "~"
        return codeVerifier.matches("^[A-Za-z0-9\\-._~]+$");
    }
}