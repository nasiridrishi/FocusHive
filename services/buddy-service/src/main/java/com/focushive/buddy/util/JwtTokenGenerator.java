package com.focushive.buddy.util;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

/**
 * Utility class for generating test JWT tokens.
 * Can be run as a standalone Java application.
 */
public class JwtTokenGenerator {

    private static final String DEFAULT_SECRET = "your-secret-key-must-be-changed-in-production";

    public static String generateToken(String userId, List<String> roles, long expirationHours) {
        String secret = System.getenv("JWT_SECRET");
        if (secret == null || secret.isEmpty()) {
            secret = DEFAULT_SECRET;
        }

        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));

        Date now = new Date();
        Date expiration = new Date(now.getTime() + (expirationHours * 3600 * 1000));

        return Jwts.builder()
                .subject(userId)
                .claim("userId", userId)
                .claim("roles", roles)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(key)
                .compact();
    }

    public static void main(String[] args) {
        String userId = args.length > 0 ? args[0] : "test-user-1";
        String role = args.length > 1 ? args[1] : "USER";
        long hours = args.length > 2 ? Long.parseLong(args[2]) : 24;

        String token = generateToken(userId, List.of(role), hours);

        System.out.println("JWT Token Generated:");
        System.out.println("===================");
        System.out.println(token);
        System.out.println();
        System.out.println("Token Details:");
        System.out.println("  User ID: " + userId);
        System.out.println("  Role: " + role);
        System.out.println("  Expires in: " + hours + " hours");
        System.out.println();
        System.out.println("Usage:");
        System.out.println("  export TOKEN=\"" + token + "\"");
        System.out.println("  curl -H \"Authorization: Bearer $TOKEN\" http://localhost:8087/api/v1/buddy/...");
    }
}