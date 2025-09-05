package com.focushive.ratelimit;

import com.focushive.ratelimit.config.BucketManager;
import com.focushive.ratelimit.config.RateLimitProperties;
import com.focushive.ratelimit.service.RateLimitService;
import com.focushive.ratelimit.model.RateLimitResult;
import io.github.bucket4j.Bucket;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RateLimitService.
 * Tests rate limiting logic, bucket configuration, and various scenarios.
 */
@ExtendWith(MockitoExtension.class)
class RateLimitServiceTest {

    @Mock
    private BucketManager bucketManager;

    @Mock
    private RateLimitProperties rateLimitProperties;

    @Mock
    private Bucket bucket;

    private RateLimitService rateLimitService;

    @BeforeEach
    void setUp() {
        rateLimitService = new RateLimitService(bucketManager, rateLimitProperties);
        
        // Default configuration with lenient stubbing
        lenient().when(rateLimitProperties.isEnabled()).thenReturn(true);
        lenient().when(rateLimitProperties.getExcludedEndpoints()).thenReturn(new String[0]);
        lenient().when(rateLimitProperties.getPublicApiRequestsPerHour()).thenReturn(100);
        lenient().when(rateLimitProperties.getAuthenticatedApiRequestsPerHour()).thenReturn(1000);
        lenient().when(rateLimitProperties.getAdminApiRequestsPerHour()).thenReturn(10000);
        lenient().when(rateLimitProperties.getWebsocketConnectionsPerMinute()).thenReturn(60);
    }

    @Test
    void shouldAllowRequestWhenBucketHasTokens() {
        // Given
        String key = "user:123";
        String endpoint = "/api/hives";
        when(bucketManager.getBucket(key, endpoint, "user")).thenReturn(bucket);
        when(bucket.tryConsume(1)).thenReturn(true);
        when(bucket.getAvailableTokens()).thenReturn(99L);

        // When
        RateLimitResult result = rateLimitService.checkRateLimit(key, endpoint, "user");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isAllowed()).isTrue();
        assertThat(result.getRemainingTokens()).isEqualTo(99L);
        assertThat(result.getRetryAfterSeconds()).isEqualTo(0);
    }

    @Test
    void shouldRejectRequestWhenBucketIsEmpty() {
        // Given
        String key = "user:123";
        String endpoint = "/api/hives";
        when(bucketManager.getBucket(key, endpoint, "user")).thenReturn(bucket);
        when(bucket.tryConsume(1)).thenReturn(false);
        when(bucket.getAvailableTokens()).thenReturn(0L);

        // When
        RateLimitResult result = rateLimitService.checkRateLimit(key, endpoint, "user");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isAllowed()).isFalse();
        assertThat(result.getRemainingTokens()).isEqualTo(0L);
        assertThat(result.getRetryAfterSeconds()).isGreaterThan(0);
    }

    @Test
    void shouldUseDifferentBucketsForDifferentKeys() {
        // Given
        String key1 = "user:123";
        String key2 = "user:456";
        String endpoint = "/api/hives";

        Bucket bucket1 = mock(Bucket.class);
        Bucket bucket2 = mock(Bucket.class);
        when(bucket1.tryConsume(1)).thenReturn(true);
        when(bucket2.tryConsume(1)).thenReturn(true);
        when(bucket1.getAvailableTokens()).thenReturn(99L);
        when(bucket2.getAvailableTokens()).thenReturn(49L);

        when(bucketManager.getBucket(key1, endpoint, "user")).thenReturn(bucket1);
        when(bucketManager.getBucket(key2, endpoint, "user")).thenReturn(bucket2);

        // When
        RateLimitResult result1 = rateLimitService.checkRateLimit(key1, endpoint, "user");
        RateLimitResult result2 = rateLimitService.checkRateLimit(key2, endpoint, "user");

        // Then
        assertThat(result1.getRemainingTokens()).isEqualTo(99L);
        assertThat(result2.getRemainingTokens()).isEqualTo(49L);
    }

    @Test
    void shouldApplyDifferentLimitsForDifferentEndpoints() {
        // Given
        String key = "user:123";
        String publicEndpoint = "/api/public/status";
        String authEndpoint = "/api/hives";

        when(rateLimitProperties.getPublicApiRequestsPerHour()).thenReturn(100);
        when(rateLimitProperties.getAuthenticatedApiRequestsPerHour()).thenReturn(1000);

        when(bucketManager.getBucket(key, publicEndpoint, "anonymous")).thenReturn(bucket);
        when(bucketManager.getBucket(key, authEndpoint, "user")).thenReturn(bucket);
        when(bucket.tryConsume(1)).thenReturn(true);
        when(bucket.getAvailableTokens()).thenReturn(99L, 999L);

        // When
        RateLimitResult publicResult = rateLimitService.checkRateLimit(key, publicEndpoint, "anonymous");
        RateLimitResult authResult = rateLimitService.checkRateLimit(key, authEndpoint, "user");

        // Then
        assertThat(publicResult).isNotNull();
        assertThat(authResult).isNotNull();
        // Verify different configurations were applied
        verify(rateLimitProperties, atLeastOnce()).getPublicApiRequestsPerHour();
        verify(rateLimitProperties, atLeastOnce()).getAuthenticatedApiRequestsPerHour();
    }

    @Test
    void shouldApplyBurstProtection() {
        // Given
        String key = "user:123";
        String endpoint = "/api/hives";
        
        lenient().when(rateLimitProperties.getBurstCapacity()).thenReturn(20);
        lenient().when(rateLimitProperties.getBurstRefillDuration()).thenReturn(Duration.ofMinutes(1));

        when(bucketManager.getBucket(key, endpoint, "user")).thenReturn(bucket);

        // Simulate rapid requests
        when(bucket.tryConsume(1)).thenReturn(true, true, true, true, false);
        when(bucket.getAvailableTokens()).thenReturn(16L, 15L, 14L, 13L, 0L);

        // When - simulate 5 rapid requests
        for (int i = 0; i < 5; i++) {
            RateLimitResult result = rateLimitService.checkRateLimit(key, endpoint, "user");
            if (i < 4) {
                assertThat(result.isAllowed()).isTrue();
            } else {
                assertThat(result.isAllowed()).isFalse();
            }
        }

        // Then
        verify(bucket, times(5)).tryConsume(1);
    }

    @Test
    void shouldExtractClientIpFromRequest() {
        // Given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("192.168.1.100");

        // When
        String key = rateLimitService.getRateLimitKey(request);

        // Then
        assertThat(key).isEqualTo("ip:192.168.1.100");
    }

    @Test
    void shouldExtractClientIpFromXForwardedForHeader() {
        // Given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "10.0.0.1, 192.168.1.1");
        request.setRemoteAddr("192.168.1.100");

        // When
        String key = rateLimitService.getRateLimitKey(request);

        // Then
        assertThat(key).isEqualTo("ip:10.0.0.1");
    }

    @Test
    void shouldUseUserIdWhenAuthenticated() {
        // Given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("192.168.1.100");
        request.setUserPrincipal(() -> "user-123");

        // When
        String key = rateLimitService.getRateLimitKey(request);

        // Then
        assertThat(key).isEqualTo("user:user-123");
    }

    @Test
    void shouldReturnCorrectRateLimitHeaders() {
        // Given
        String key = "user:123";
        String endpoint = "/api/hives";
        when(bucketManager.getBucket(key, endpoint, "user")).thenReturn(bucket);
        when(bucket.tryConsume(1)).thenReturn(true);
        when(bucket.getAvailableTokens()).thenReturn(99L);

        // When
        RateLimitResult result = rateLimitService.checkRateLimit(key, endpoint, "user");

        // Then
        assertThat(result.getHeaders()).containsKeys(
            "X-RateLimit-Limit",
            "X-RateLimit-Remaining",
            "X-RateLimit-Reset"
        );
        assertThat(result.getHeaders().get("X-RateLimit-Remaining")).isEqualTo("99");
    }

    @Test
    void shouldHandleAdminEndpointsWithHigherLimits() {
        // Given
        String key = "admin:123";
        String endpoint = "/api/admin/users";
        
        when(rateLimitProperties.getAdminApiRequestsPerHour()).thenReturn(10000);
        when(bucketManager.getBucket(key, endpoint, "admin")).thenReturn(bucket);
        when(bucket.tryConsume(1)).thenReturn(true);
        when(bucket.getAvailableTokens()).thenReturn(9999L);

        // When
        RateLimitResult result = rateLimitService.checkRateLimit(key, endpoint, "admin");

        // Then
        assertThat(result.isAllowed()).isTrue();
        assertThat(result.getRemainingTokens()).isEqualTo(9999L);
        verify(rateLimitProperties).getAdminApiRequestsPerHour();
    }
}