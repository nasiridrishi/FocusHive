package com.focushive.websocket.integration.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.test.util.TestSocketUtils;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Utility methods for WebSocket integration testing
 */
public class WebSocketTestUtils {
    
    private static final Logger log = LoggerFactory.getLogger(WebSocketTestUtils.class);
    
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * Generate a random JWT token for testing
     */
    public static String generateTestJwtToken(Long userId, String username) {
        // This is a mock JWT token for testing purposes
        // In real tests, you would generate a proper JWT
        return "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9." +
               "eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ." +
               "SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";
    }
    
    /**
     * Generate an expired JWT token for testing
     */
    public static String generateExpiredJwtToken() {
        // Mock expired token
        return "expired.jwt.token";
    }
    
    /**
     * Generate an invalid JWT token for testing
     */
    public static String generateInvalidJwtToken() {
        return "invalid.jwt.token";
    }
    
    /**
     * Create test user data for presence updates
     */
    public static Map<String, Object> createPresenceUpdateData(String status, Long hiveId, String activity) {
        Map<String, Object> data = new HashMap<>();
        data.put("status", status);
        if (hiveId != null) {
            data.put("hiveId", hiveId);
        }
        if (activity != null) {
            data.put("activity", activity);
        }
        return data;
    }
    
    /**
     * Create test focus session data
     */
    public static Map<String, Object> createFocusSessionData(Long hiveId, Integer minutes) {
        Map<String, Object> data = new HashMap<>();
        if (hiveId != null) {
            data.put("hiveId", hiveId);
        }
        data.put("minutes", minutes != null ? minutes : 25);
        return data;
    }
    
    /**
     * Create test buddy session data
     */
    public static Map<String, Object> createBuddySessionData(Long buddyId) {
        Map<String, Object> data = new HashMap<>();
        data.put("buddyId", buddyId);
        return data;
    }
    
    /**
     * Create test typing indicator data
     */
    public static Map<String, Object> createTypingData(String location, boolean isTyping) {
        Map<String, Object> data = new HashMap<>();
        data.put("location", location);
        data.put("isTyping", isTyping);
        return data;
    }
    
    /**
     * Create STOMP headers with authentication
     */
    public static StompHeaders createAuthHeaders(String jwtToken) {
        StompHeaders headers = new StompHeaders();
        headers.add("Authorization", "Bearer " + jwtToken);
        return headers;
    }
    
    /**
     * Create STOMP headers with custom values
     */
    public static StompHeaders createHeaders(Map<String, String> headerMap) {
        StompHeaders headers = new StompHeaders();
        headerMap.forEach(headers::add);
        return headers;
    }
    
    /**
     * Find available port for testing
     */
    public static int findAvailablePort() {
        return TestSocketUtils.findAvailableTcpPort();
    }
    
    /**
     * Generate random user ID for testing
     */
    public static Long generateTestUserId() {
        return ThreadLocalRandom.current().nextLong(1000, 9999);
    }
    
    /**
     * Generate random hive ID for testing
     */
    public static Long generateTestHiveId() {
        return ThreadLocalRandom.current().nextLong(100, 999);
    }
    
    /**
     * Convert object to JSON string
     */
    public static String toJson(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to convert object to JSON", e);
        }
    }
    
    /**
     * Convert JSON string to object
     */
    public static <T> T fromJson(String json, Class<T> clazz) {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to convert JSON to object", e);
        }
    }
    
    /**
     * Wait for condition with timeout
     */
    public static boolean waitForCondition(Runnable condition, Duration timeout) {
        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < timeout.toMillis()) {
            try {
                condition.run();
                return true;
            } catch (Exception | AssertionError e) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
        }
        return false;
    }
    
    /**
     * Sleep for specified duration (helper for testing)
     */
    public static void sleep(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Sleep interrupted", e);
        }
    }
    
    /**
     * Create WebSocket URL for testing
     */
    public static String createWebSocketUrl(int port, String endpoint) {
        return String.format("ws://localhost:%d%s", port, endpoint);
    }
    
    /**
     * Verify message payload contains expected data
     */
    public static boolean verifyMessagePayload(String actualPayload, Map<String, Object> expectedData) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> actualData = objectMapper.readValue(actualPayload, Map.class);
            
            for (Map.Entry<String, Object> entry : expectedData.entrySet()) {
                Object actualValue = actualData.get(entry.getKey());
                Object expectedValue = entry.getValue();
                
                if (actualValue == null && expectedValue != null) {
                    log.warn("Expected key '{}' not found in payload", entry.getKey());
                    return false;
                }
                
                if (!actualValue.equals(expectedValue)) {
                    log.warn("Key '{}': expected '{}', got '{}'", 
                           entry.getKey(), expectedValue, actualValue);
                    return false;
                }
            }
            
            return true;
        } catch (JsonProcessingException e) {
            log.error("Failed to parse message payload: {}", actualPayload, e);
            return false;
        }
    }
    
    /**
     * Extract user ID from WebSocket session (for testing)
     */
    public static Long extractUserIdFromSession(String sessionId) {
        // For testing purposes, assume session ID contains user ID
        // In real implementation, this would be extracted from JWT token
        try {
            return Long.parseLong(sessionId.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            return 1L; // Default test user ID
        }
    }
    
    /**
     * Calculate message latency
     */
    public static long calculateLatency(long sentTimestamp, long receivedTimestamp) {
        return receivedTimestamp - sentTimestamp;
    }
    
    /**
     * Check if latency is acceptable (< 1000ms for integration tests)
     */
    public static boolean isLatencyAcceptable(long latency) {
        return latency < 1000;
    }
}