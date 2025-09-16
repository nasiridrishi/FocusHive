package com.focushive.hive.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.focushive.hive.dto.CreateHiveRequest;
import com.focushive.hive.dto.HiveResponse;
import com.focushive.hive.dto.UpdateHiveRequest;
import com.focushive.hive.entity.Hive;
import com.focushive.hive.entity.HiveMember;
import com.focushive.hive.service.HiveService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Enhanced Controller tests for HiveController - TDD approach focusing on REST API behaviors.
 * These tests will FAIL initially until enhanced controller implementation is complete.
 */
@WebMvcTest(HiveController.class)
@DisplayName("Enhanced Hive Controller Tests")
class EnhancedHiveControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private HiveService hiveService;

    @Autowired
    private ObjectMapper objectMapper;

    private CreateHiveRequest createRequest;
    private UpdateHiveRequest updateRequest;
    private HiveResponse hiveResponse;

    @BeforeEach
    void setUp() {
        // Setup test data
        createRequest = new CreateHiveRequest();
        createRequest.setName("Test Hive");
        createRequest.setDescription("Test description");
        createRequest.setMaxMembers(20);
        createRequest.setIsPublic(true);
        createRequest.setType(Hive.HiveType.STUDY);

        updateRequest = new UpdateHiveRequest();
        updateRequest.setName("Updated Test Hive");
        updateRequest.setDescription("Updated description");

        hiveResponse = new HiveResponse();
        hiveResponse.setId(UUID.randomUUID().toString());
        hiveResponse.setName("Test Hive");
        hiveResponse.setDescription("Test description");
        hiveResponse.setMaxMembers(20);
        hiveResponse.setIsPublic(true);
        hiveResponse.setType(Hive.HiveType.STUDY);
        hiveResponse.setCurrentMembers(1);
        hiveResponse.setCreatedAt(LocalDateTime.now());
    }

    // ===== ENHANCED CREATE ENDPOINT TESTS =====

    @Nested
    @DisplayName("Enhanced Create Hive Tests")
    class CreateHiveTests {

        @Test
        @WithMockUser
        @DisplayName("Should return 201 with valid hive creation")
        void createHive_ValidRequest_Returns201() throws Exception {
            // ARRANGE
            when(hiveService.createHive(any(CreateHiveRequest.class), anyString()))
                    .thenReturn(hiveResponse);

            // ACT & ASSERT
            mockMvc.perform(post("/api/v1/hives")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createRequest)))
                    .andDo(print())
                    .andExpect(status().isCreated())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").exists())
                    .andExpect(jsonPath("$.name").value("Test Hive"))
                    .andExpect(jsonPath("$.description").value("Test description"));

            verify(hiveService).createHive(any(CreateHiveRequest.class), anyString());
        }

        @Test
        @WithMockUser
        @DisplayName("Should return 400 for invalid input validation")
        void createHive_InvalidInput_Returns400() throws Exception {
            // ARRANGE - Invalid request with null/empty fields
            CreateHiveRequest invalidRequest = new CreateHiveRequest();
            invalidRequest.setName(""); // Empty name should fail validation
            invalidRequest.setMaxMembers(-1); // Negative max members should fail

            // ACT & ASSERT
            mockMvc.perform(post("/api/v1/hives")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.error").exists())
                    .andExpect(jsonPath("$.message").exists())
                    .andExpect(jsonPath("$.validationErrors").exists());

            verify(hiveService, never()).createHive(any(), any());
        }

        @Test
        @WithMockUser
        @DisplayName("Should apply rate limiting to prevent spam")
        void createHive_RateLimited_Returns429() throws Exception {
            // This test will initially fail until rate limiting is implemented

            // Simulate multiple rapid requests (will need rate limiting implementation)
            for (int i = 0; i < 10; i++) {
                mockMvc.perform(post("/api/v1/hives")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                        .andDo(print());
            }

            // The 11th request should be rate limited
            mockMvc.perform(post("/api/v1/hives")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createRequest)))
                    .andDo(print())
                    .andExpect(status().isTooManyRequests())
                    .andExpect(header().exists("X-RateLimit-Remaining"))
                    .andExpect(header().exists("X-RateLimit-Reset"));
        }

        @Test
        @DisplayName("Should return 401 for unauthenticated requests")
        void createHive_Unauthenticated_Returns401() throws Exception {
            // ACT & ASSERT
            mockMvc.perform(post("/api/v1/hives")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createRequest)))
                    .andDo(print())
                    .andExpect(status().isUnauthorized());

            verify(hiveService, never()).createHive(any(), any());
        }
    }

    // ===== ENHANCED READ ENDPOINT TESTS =====

    @Nested
    @DisplayName("Enhanced Get Hive Tests")
    class GetHiveTests {

        @Test
        @WithMockUser
        @DisplayName("Should return hive with proper response headers")
        void getHive_ValidId_ReturnsWithHeaders() throws Exception {
            // ARRANGE
            String hiveId = UUID.randomUUID().toString();
            when(hiveService.getHive(hiveId, anyString())).thenReturn(hiveResponse);

            // ACT & ASSERT
            mockMvc.perform(get("/api/v1/hives/{id}", hiveId))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(header().exists("Cache-Control"))
                    .andExpect(header().string("Cache-Control", "public, max-age=300"))
                    .andExpect(jsonPath("$.id").value(hiveResponse.getId()))
                    .andExpect(jsonPath("$.name").value(hiveResponse.getName()));

            verify(hiveService).getHive(hiveId, anyString());
        }

        @Test
        @WithMockUser
        @DisplayName("Should handle ETag for cache validation")
        void getHive_ETagSupport_HandlesConditionalRequests() throws Exception {
            // This will test ETag implementation for conditional requests
            String hiveId = UUID.randomUUID().toString();
            when(hiveService.getHive(hiveId, anyString())).thenReturn(hiveResponse);

            // First request should return full response with ETag
            String etag = "\"" + hiveResponse.getUpdatedAt().toString() + "\"";

            mockMvc.perform(get("/api/v1/hives/{id}", hiveId))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(header().exists("ETag"))
                    .andExpect(header().string("ETag", etag));

            // Second request with If-None-Match should return 304
            mockMvc.perform(get("/api/v1/hives/{id}", hiveId)
                            .header("If-None-Match", etag))
                    .andDo(print())
                    .andExpect(status().isNotModified());
        }

        @Test
        @WithMockUser
        @DisplayName("Should return 404 for non-existent hives")
        void getHive_NotFound_Returns404() throws Exception {
            // ARRANGE
            String nonExistentId = UUID.randomUUID().toString();
            when(hiveService.getHive(nonExistentId, anyString()))
                    .thenThrow(new com.focushive.common.exception.ResourceNotFoundException("Hive not found"));

            // ACT & ASSERT
            mockMvc.perform(get("/api/v1/hives/{id}", nonExistentId))
                    .andDo(print())
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error").value("RESOURCE_NOT_FOUND"))
                    .andExpect(jsonPath("$.message").value("Hive not found"));

            verify(hiveService).getHive(nonExistentId, anyString());
        }
    }

    // ===== ENHANCED UPDATE ENDPOINT TESTS =====

    @Nested
    @DisplayName("Enhanced Update Hive Tests")
    class UpdateHiveTests {

        @Test
        @WithMockUser
        @DisplayName("Should validate partial updates correctly")
        void updateHive_PartialUpdate_ValidatesCorrectly() throws Exception {
            // ARRANGE
            String hiveId = UUID.randomUUID().toString();
            UpdateHiveRequest partialUpdate = new UpdateHiveRequest();
            partialUpdate.setName("New Name Only"); // Only update name

            HiveResponse updatedResponse = new HiveResponse();
            updatedResponse.setId(hiveId);
            updatedResponse.setName("New Name Only");

            when(hiveService.updateHive(eq(hiveId), any(UpdateHiveRequest.class), anyString()))
                    .thenReturn(updatedResponse);

            // ACT & ASSERT
            mockMvc.perform(put("/api/v1/hives/{id}", hiveId)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(partialUpdate)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("New Name Only"));

            verify(hiveService).updateHive(eq(hiveId), any(UpdateHiveRequest.class), anyString());
        }

        @Test
        @WithMockUser
        @DisplayName("Should return 403 for unauthorized update attempts")
        void updateHive_Unauthorized_Returns403() throws Exception {
            // ARRANGE
            String hiveId = UUID.randomUUID().toString();
            when(hiveService.updateHive(eq(hiveId), any(UpdateHiveRequest.class), anyString()))
                    .thenThrow(new com.focushive.common.exception.ForbiddenException("Access denied"));

            // ACT & ASSERT
            mockMvc.perform(put("/api/v1/hives/{id}", hiveId)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest)))
                    .andDo(print())
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.error").value("FORBIDDEN"))
                    .andExpect(jsonPath("$.message").value("Access denied"));
        }

        @Test
        @WithMockUser
        @DisplayName("Should validate business rule constraints")
        void updateHive_BusinessRuleViolation_Returns400() throws Exception {
            // ARRANGE
            String hiveId = UUID.randomUUID().toString();
            UpdateHiveRequest invalidUpdate = new UpdateHiveRequest();
            invalidUpdate.setMaxMembers(1); // Less than current members

            when(hiveService.updateHive(eq(hiveId), any(UpdateHiveRequest.class), anyString()))
                    .thenThrow(new com.focushive.common.exception.BadRequestException(
                            "Cannot set max members to 1, hive currently has 5 members"));

            // ACT & ASSERT
            mockMvc.perform(put("/api/v1/hives/{id}", hiveId)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidUpdate)))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("BAD_REQUEST"))
                    .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Cannot set max members")));
        }
    }

    // ===== ENHANCED LIST/SEARCH ENDPOINT TESTS =====

    @Nested
    @DisplayName("Enhanced List and Search Tests")
    class ListSearchTests {

        @Test
        @WithMockUser
        @DisplayName("Should support pagination with proper headers")
        void listHives_Pagination_ReturnsWithHeaders() throws Exception {
            // ARRANGE
            Page<HiveResponse> hivePage = new PageImpl<>(
                    List.of(hiveResponse), PageRequest.of(0, 20), 1);
            when(hiveService.listPublicHives(any())).thenReturn(hivePage);

            // ACT & ASSERT
            mockMvc.perform(get("/api/v1/hives")
                            .param("page", "0")
                            .param("size", "20"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(header().exists("X-Total-Count"))
                    .andExpect(header().string("X-Total-Count", "1"))
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content.length()").value(1))
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.totalPages").value(1));

            verify(hiveService).listPublicHives(any());
        }

        @Test
        @WithMockUser
        @DisplayName("Should support advanced search with filters")
        void searchHives_AdvancedFilters_ReturnsFiltered() throws Exception {
            // ARRANGE
            Page<HiveResponse> filteredResults = new PageImpl<>(List.of(hiveResponse));
            when(hiveService.searchHives(anyString(), any())).thenReturn(filteredResults);

            // ACT & ASSERT
            mockMvc.perform(get("/api/v1/hives/search")
                            .param("query", "study")
                            .param("type", "STUDY")
                            .param("minMembers", "1")
                            .param("maxMembers", "10"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content[0].name").value("Test Hive"));

            verify(hiveService).searchHives(eq("study"), any());
        }

        @Test
        @WithMockUser
        @DisplayName("Should handle empty search results gracefully")
        void searchHives_NoResults_ReturnsEmptyPage() throws Exception {
            // ARRANGE
            Page<HiveResponse> emptyResults = new PageImpl<>(List.of());
            when(hiveService.searchHives(anyString(), any())).thenReturn(emptyResults);

            // ACT & ASSERT
            mockMvc.perform(get("/api/v1/hives/search")
                            .param("query", "nonexistent"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content.length()").value(0))
                    .andExpect(jsonPath("$.totalElements").value(0));
        }
    }

    // ===== ENHANCED SECURITY TESTS =====

    @Nested
    @DisplayName("Enhanced Security Tests")
    class SecurityTests {

        @Test
        @WithMockUser
        @DisplayName("Should validate CSRF protection on state-changing operations")
        void stateChangingOperations_CSRFProtection_RequiresToken() throws Exception {
            // Test CSRF protection is enforced
            mockMvc.perform(post("/api/v1/hives")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createRequest))
                            // No CSRF token
                    )
                    .andDo(print())
                    .andExpect(status().isForbidden());

            mockMvc.perform(put("/api/v1/hives/{id}", UUID.randomUUID().toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest))
                            // No CSRF token
                    )
                    .andDo(print())
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser
        @DisplayName("Should include security headers in responses")
        void allEndpoints_SecurityHeaders_IncludedInResponse() throws Exception {
            // ARRANGE
            when(hiveService.getHive(anyString(), anyString())).thenReturn(hiveResponse);

            // ACT & ASSERT
            mockMvc.perform(get("/api/v1/hives/{id}", UUID.randomUUID().toString()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(header().exists("X-Content-Type-Options"))
                    .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                    .andExpect(header().exists("X-Frame-Options"))
                    .andExpect(header().string("X-Frame-Options", "DENY"))
                    .andExpect(header().exists("X-XSS-Protection"))
                    .andExpect(header().exists("Strict-Transport-Security"));
        }

        @Test
        @WithMockUser
        @DisplayName("Should sanitize input to prevent injection attacks")
        void userInput_Sanitization_PreventsInjection() throws Exception {
            // ARRANGE - Potentially malicious input
            CreateHiveRequest maliciousRequest = new CreateHiveRequest();
            maliciousRequest.setName("<script>alert('xss')</script>");
            maliciousRequest.setDescription("'; DROP TABLE hives; --");

            // This should be sanitized by the controller/service
            when(hiveService.createHive(any(), anyString())).thenReturn(hiveResponse);

            // ACT & ASSERT
            mockMvc.perform(post("/api/v1/hives")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(maliciousRequest)))
                    .andDo(print())
                    .andExpect(status().isCreated());

            // Verify that sanitized input was passed to service
            verify(hiveService).createHive(argThat(request ->
                    !request.getName().contains("<script>") &&
                    !request.getDescription().contains("DROP TABLE")
            ), anyString());
        }
    }

    // ===== PERFORMANCE TESTS =====

    @Nested
    @DisplayName("Enhanced Performance Tests")
    class PerformanceTests {

        @Test
        @WithMockUser
        @DisplayName("Should respond within performance thresholds")
        void allEndpoints_ResponseTime_UnderThreshold() throws Exception {
            // ARRANGE
            when(hiveService.listPublicHives(any())).thenReturn(new PageImpl<>(List.of(hiveResponse)));

            // ACT
            long startTime = System.currentTimeMillis();
            mockMvc.perform(get("/api/v1/hives"))
                    .andExpect(status().isOk());
            long responseTime = System.currentTimeMillis() - startTime;

            // ASSERT
            if (responseTime > 100) {
                throw new AssertionError("Response time " + responseTime + "ms exceeds 100ms threshold");
            }
        }

        @Test
        @WithMockUser
        @DisplayName("Should implement proper compression for large responses")
        void largeResponses_Compression_OptimizedSize() throws Exception {
            // This will test response compression implementation
            when(hiveService.listPublicHives(any())).thenReturn(new PageImpl<>(List.of(hiveResponse)));

            mockMvc.perform(get("/api/v1/hives")
                            .header("Accept-Encoding", "gzip, deflate"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(header().string("Content-Encoding", "gzip"));
        }
    }

    // Helper method to check if assertThat is available (will be added in imports when tests run)
    private void assertThat(long actual) {
        if (actual > 100) {
            throw new AssertionError("Response time " + actual + "ms exceeds 100ms threshold");
        }
    }
}