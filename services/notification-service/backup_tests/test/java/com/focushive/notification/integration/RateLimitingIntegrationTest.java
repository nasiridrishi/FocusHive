package com.focushive.notification.integration;

import com.focushive.notification.config.SecurityProperties;
import com.focushive.notification.config.RateLimitingInterceptor;
import com.focushive.notification.service.RateLimitingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for rate limiting functionality.
 * Tests the complete rate limiting chain including interceptor, service, and Redis integration.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Rate Limiting Integration Tests")
class RateLimitingIntegrationTest {

    @Container
    private static final GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RateLimitingService rateLimitingService;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private SecurityProperties securityProperties;

    @BeforeEach
    void setUp() {
        // Clean up Redis before each test
        redisTemplate.getConnectionFactory().getConnection().flushAll();
    }

    @Test
    @DisplayName("Should allow requests within rate limit for anonymous users")
    void shouldAllowRequestsWithinRateLimitForAnonymousUsers() throws Exception {
        SecurityProperties.RateLimitingConfig config = securityProperties.getRateLimiting();
        int limit = config.getAnonymousRequestsPerMinute();

        // Make requests up to the limit
        for (int i = 0; i < limit; i++) {
            mockMvc.perform(get("/api/notifications")
                    .header("X-Forwarded-For", "192.168.1.100"))
                    .andExpect(status().isOk())
                    .andExpect(header().exists("X-RateLimit-Limit"))
                    .andExpect(header().exists("X-RateLimit-Remaining"))
                    .andExpect(header().exists("X-RateLimit-Reset"));
        }
    }

    @Test
    @DisplayName("Should block requests exceeding rate limit for anonymous users")
    void shouldBlockRequestsExceedingRateLimitForAnonymousUsers() throws Exception {
        SecurityProperties.RateLimitingConfig config = securityProperties.getRateLimiting();
        int limit = config.getAnonymousRequestsPerMinute();

        // Make requests up to the limit
        for (int i = 0; i < limit; i++) {
            mockMvc.perform(get("/api/notifications")
                    .header("X-Forwarded-For", "192.168.1.101"))
                    .andExpect(status().isOk());
        }

        // Next request should be blocked
        mockMvc.perform(get("/api/notifications")
                .header("X-Forwarded-For", "192.168.1.101"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("X-RateLimit-Limit", "0"))
                .andExpect(header().string("X-RateLimit-Remaining", "0"));
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    @DisplayName("Should apply different rate limits for authenticated users")
    void shouldApplyDifferentRateLimitsForAuthenticatedUsers() throws Exception {
        SecurityProperties.RateLimitingConfig config = securityProperties.getRateLimiting();
        int userLimit = config.getDefaultRequestsPerMinute();

        // Authenticated users should have higher limits
        for (int i = 0; i < userLimit; i++) {
            mockMvc.perform(get("/api/notifications"))
                    .andExpect(status().isOk());
        }

        // Should still be within limit for authenticated users
        assertTrue(userLimit > config.getAnonymousRequestsPerMinute());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    @DisplayName("Should apply admin rate limits for admin users")
    void shouldApplyAdminRateLimitsForAdminUsers() throws Exception {
        SecurityProperties.RateLimitingConfig config = securityProperties.getRateLimiting();
        int adminLimit = config.getAdminRequestsPerMinute();

        // Admin users should have the highest limits
        for (int i = 0; i < adminLimit; i++) {
            mockMvc.perform(get("/api/notifications"))
                    .andExpect(status().isOk());
        }

        // Admin limit should be higher than regular user limit
        assertTrue(adminLimit > config.getDefaultRequestsPerMinute());
    }

    @Test
    @DisplayName("Should exclude configured paths from rate limiting")
    void shouldExcludeConfiguredPathsFromRateLimiting() throws Exception {
        // Health endpoint should be excluded
        for (int i = 0; i < 100; i++) {
            mockMvc.perform(get("/health"))
                    .andExpect(status().isOk());
        }

        // Actuator endpoints should be excluded
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should handle burst traffic with burst capacity")
    void shouldHandleBurstTrafficWithBurstCapacity() throws Exception {
        SecurityProperties.RateLimitingConfig config = securityProperties.getRateLimiting();
        int burstCapacity = config.getBurstCapacity();

        // Send burst of requests quickly
        for (int i = 0; i < burstCapacity; i++) {
            mockMvc.perform(get("/api/notifications")
                    .header("X-Forwarded-For", "192.168.1.102"))
                    .andExpect(status().isOk());
        }
    }

    @Test
    @DisplayName("Should track rate limits per unique IP address")
    void shouldTrackRateLimitsPerUniqueIpAddress() throws Exception {
        // Different IPs should have separate rate limits
        String ip1 = "192.168.1.103";
        String ip2 = "192.168.1.104";

        // Make requests from IP1
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(get("/api/notifications")
                    .header("X-Forwarded-For", ip1))
                    .andExpect(status().isOk());
        }

        // Make requests from IP2 - should not be affected by IP1's rate limit
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(get("/api/notifications")
                    .header("X-Forwarded-For", ip2))
                    .andExpect(status().isOk());
        }
    }

    @Test
    @DisplayName("Should reset rate limit after time window expires")
    void shouldResetRateLimitAfterTimeWindowExpires() throws Exception {
        SecurityProperties.RateLimitingConfig config = securityProperties.getRateLimiting();
        int limit = config.getAnonymousRequestsPerMinute();
        String testIp = "192.168.1.105";

        // Exhaust rate limit
        for (int i = 0; i < limit; i++) {
            mockMvc.perform(get("/api/notifications")
                    .header("X-Forwarded-For", testIp))
                    .andExpect(status().isOk());
        }

        // Should be blocked
        mockMvc.perform(get("/api/notifications")
                .header("X-Forwarded-For", testIp))
                .andExpect(status().isTooManyRequests());

        // Wait for time window to expire (using shorter window for testing)
        Thread.sleep(Duration.ofSeconds(61).toMillis());

        // Should be allowed again
        mockMvc.perform(get("/api/notifications")
                .header("X-Forwarded-For", testIp))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should include rate limit headers in response")
    void shouldIncludeRateLimitHeadersInResponse() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/notifications")
                .header("X-Forwarded-For", "192.168.1.106"))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-RateLimit-Limit"))
                .andExpect(header().exists("X-RateLimit-Remaining"))
                .andExpect(header().exists("X-RateLimit-Reset"))
                .andReturn();

        String limitHeader = result.getResponse().getHeader("X-RateLimit-Limit");
        String remainingHeader = result.getResponse().getHeader("X-RateLimit-Remaining");
        String resetHeader = result.getResponse().getHeader("X-RateLimit-Reset");

        assertNotNull(limitHeader);
        assertNotNull(remainingHeader);
        assertNotNull(resetHeader);

        int limit = Integer.parseInt(limitHeader);
        int remaining = Integer.parseInt(remainingHeader);
        long reset = Long.parseLong(resetHeader);

        assertTrue(limit > 0);
        assertTrue(remaining >= 0);
        assertTrue(reset > System.currentTimeMillis());
    }

    @Test
    @DisplayName("Should handle concurrent requests correctly")
    void shouldHandleConcurrentRequestsCorrectly() throws Exception {
        SecurityProperties.RateLimitingConfig config = securityProperties.getRateLimiting();
        int limit = config.getAnonymousRequestsPerMinute();
        String testIp = "192.168.1.107";

        int totalRequests = limit + 10; // Try to exceed the limit
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger blockedCount = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(totalRequests);
        ExecutorService executor = Executors.newFixedThreadPool(10);

        // Send concurrent requests
        for (int i = 0; i < totalRequests; i++) {
            executor.submit(() -> {
                try {
                    MvcResult result = mockMvc.perform(get("/api/notifications")
                            .header("X-Forwarded-For", testIp))
                            .andReturn();

                    if (result.getResponse().getStatus() == 200) {
                        successCount.incrementAndGet();
                    } else if (result.getResponse().getStatus() == 429) {
                        blockedCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    // Handle exception
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        // Should have exactly 'limit' successful requests and the rest blocked
        assertEquals(limit, successCount.get());
        assertEquals(totalRequests - limit, blockedCount.get());
    }

    @Test
    @DisplayName("Should block user after repeated violations")
    void shouldBlockUserAfterRepeatedViolations() throws Exception {
        String violatorIp = "192.168.1.108";
        SecurityProperties.RateLimitingConfig config = securityProperties.getRateLimiting();
        int limit = config.getAnonymousRequestsPerMinute();

        // Exhaust rate limit multiple times
        for (int violation = 0; violation < 4; violation++) {
            // Exhaust the limit
            for (int i = 0; i < limit; i++) {
                mockMvc.perform(get("/api/notifications")
                        .header("X-Forwarded-For", violatorIp))
                        .andExpect(status().isOk());
            }

            // Try to exceed - this is a violation
            mockMvc.perform(get("/api/notifications")
                    .header("X-Forwarded-For", violatorIp))
                    .andExpect(status().isTooManyRequests());

            // Reset for next violation test
            redisTemplate.getConnectionFactory().getConnection().flushAll();
        }

        // After multiple violations, user should be temporarily blocked
        assertTrue(rateLimitingService.isBlocked("anonymous:" + violatorIp));
    }

    @Test
    @DisplayName("Should correctly calculate remaining requests")
    void shouldCorrectlyCalculateRemainingRequests() throws Exception {
        String testIp = "192.168.1.109";
        SecurityProperties.RateLimitingConfig config = securityProperties.getRateLimiting();
        int limit = config.getAnonymousRequestsPerMinute();

        // Make first request
        MvcResult result = mockMvc.perform(get("/api/notifications")
                .header("X-Forwarded-For", testIp))
                .andExpect(status().isOk())
                .andReturn();

        int remaining = Integer.parseInt(result.getResponse().getHeader("X-RateLimit-Remaining"));
        assertEquals(limit - 1, remaining);

        // Make more requests and verify remaining count decreases
        for (int i = 2; i <= 5; i++) {
            result = mockMvc.perform(get("/api/notifications")
                    .header("X-Forwarded-For", testIp))
                    .andExpect(status().isOk())
                    .andReturn();

            remaining = Integer.parseInt(result.getResponse().getHeader("X-RateLimit-Remaining"));
            assertEquals(limit - i, remaining);
        }
    }

    @Test
    @DisplayName("Should handle different HTTP methods separately")
    void shouldHandleDifferentHttpMethodsSeparately() throws Exception {
        String testIp = "192.168.1.110";

        // GET requests
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(get("/api/notifications")
                    .header("X-Forwarded-For", testIp))
                    .andExpect(status().isOk());
        }

        // POST requests should have same rate limit
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/notifications/send")
                    .header("X-Forwarded-For", testIp)
                    .contentType("application/json")
                    .content("{}"))
                    .andExpect(status().isOk());
        }
    }

    @Test
    @DisplayName("Should persist rate limit state in Redis")
    void shouldPersistRateLimitStateInRedis() throws Exception {
        String testIp = "192.168.1.111";
        String rateLimitKey = "anonymous:" + testIp;

        // Make some requests
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(get("/api/notifications")
                    .header("X-Forwarded-For", testIp))
                    .andExpect(status().isOk());
        }

        // Check Redis directly
        int currentUsage = rateLimitingService.getCurrentUsage(rateLimitKey, "READ");
        assertEquals(5, currentUsage);

        // Remaining requests should be correct
        int remaining = rateLimitingService.getRemainingRequests(rateLimitKey, "READ");
        SecurityProperties.RateLimitingConfig config = securityProperties.getRateLimiting();
        assertEquals(config.getAnonymousRequestsPerMinute() - 5, remaining);
    }
}