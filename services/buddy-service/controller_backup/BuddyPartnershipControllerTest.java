package com.focushive.buddy.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.focushive.buddy.constant.PartnershipStatus;
import com.focushive.buddy.dto.*;
import com.focushive.buddy.controller.BuddyPartnershipController;
import com.focushive.buddy.service.BuddyPartnershipService;
import com.focushive.buddy.exception.BuddyServiceException;
import com.focushive.buddy.exception.PartnershipConflictException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import com.focushive.buddy.config.TestSecurityConfig;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.containsString;

/**
 * Comprehensive test suite for BuddyPartnershipController.
 * Tests all REST endpoints for partnership lifecycle, requests, approvals, and management.
 *
 * TDD RED PHASE: These tests are designed to FAIL initially until controller is implemented.
 */
@WebMvcTest(BuddyPartnershipController.class)
@Import(TestSecurityConfig.class)
class BuddyPartnershipControllerTest {

    // Test UUID constants
    private static final UUID TEST_USER_1_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    private static final UUID TEST_USER_2_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");
    private static final String TEST_USER_1_STR = "550e8400-e29b-41d4-a716-446655440000";
    private static final String TEST_USER_2_STR = "550e8400-e29b-41d4-a716-446655440001";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BuddyPartnershipService buddyPartnershipService;

    private UUID testPartnershipId;
    private PartnershipRequestDto testRequest;
    private PartnershipResponseDto testResponse;
    private PartnershipStatisticsDto testStatistics;
    private PartnershipHealthDto testHealth;

    @BeforeEach
    void setUp() {
        testPartnershipId = UUID.randomUUID();

        testRequest = PartnershipRequestDto.builder()
                .requesterId(TEST_USER_1_ID)
                .recipientId(TEST_USER_2_ID)
                .durationDays(30)
                .agreementText("Let's work together on our goals!")
                .build();

        testResponse = PartnershipResponseDto.builder()
                .id(testPartnershipId)
                .user1Id(TEST_USER_1_ID)
                .user2Id(TEST_USER_2_ID)
                .status(PartnershipStatus.PENDING)
                .durationDays(30)
                .agreementText("Let's work together on our goals!")
                .healthScore(BigDecimal.valueOf(0.8))
                .compatibilityScore(BigDecimal.valueOf(0.85))
                .createdAt(LocalDateTime.now())
                .currentDurationDays(0L)
                .isActive(false)
                .isPending(true)
                .isEnded(false)
                .isPaused(false)
                .build();

        testStatistics = PartnershipStatisticsDto.builder()
                .userId(TEST_USER_1_ID.toString())
                .generatedAt(ZonedDateTime.now())
                .periodDescription("All time")
                .totalPartnerships(5L)
                .activePartnerships(2L)
                .completedPartnerships(2L)
                .endedPartnerships(1L)
                .pendingRequests(1L)
                .averageHealthScore(BigDecimal.valueOf(0.8))
                .partnershipSuccessRate(BigDecimal.valueOf(0.8))
                .build();

        testHealth = PartnershipHealthDto.builder()
                .partnershipId(testPartnershipId)
                .overallHealthScore(BigDecimal.valueOf(0.85))
                .lastAssessmentAt(ZonedDateTime.now())
                .healthStatus("GOOD")
                .communicationScore(BigDecimal.valueOf(0.9))
                .engagementScore(BigDecimal.valueOf(0.8))
                .goalAlignmentScore(BigDecimal.valueOf(0.85))
                .consistencyScore(BigDecimal.valueOf(0.8))
                .responsiveScore(BigDecimal.valueOf(0.85))
                .interventionSuggestions(Arrays.asList("Continue regular check-ins"))
                .positiveIndicators(Arrays.asList("High communication score"))
                .concernIndicators(Arrays.asList())
                .healthTrend("STABLE")
                .trendScore(BigDecimal.ZERO)
                .build();
    }

    @Nested
    @DisplayName("Partnership Request Management")
    class PartnershipRequestTests {

        @Test
        @DisplayName("POST /api/v1/buddy/partnerships/request - Create partnership request")
        void createPartnershipRequest_Success() throws Exception {
            when(buddyPartnershipService.createPartnershipRequest(any()))
                    .thenReturn(testResponse);

            mockMvc.perform(post("/api/v1/buddy/partnerships/request")
                    .with(csrf())
                    .header("X-User-ID", TEST_USER_1_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(testRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(testPartnershipId.toString()))
                    .andExpect(jsonPath("$.data.user1Id").value(TEST_USER_1_ID.toString()))
                    .andExpect(jsonPath("$.data.user2Id").value(TEST_USER_2_ID.toString()))
                    .andExpect(jsonPath("$.data.status").value("PENDING"))
                    .andExpect(jsonPath("$.data.durationDays").value(30))
                    .andExpect(jsonPath("$.message").value("Partnership request created successfully"));

            verify(buddyPartnershipService).createPartnershipRequest(any());
        }

        @Test
        @DisplayName("POST /api/v1/buddy/partnerships/request - Invalid request data")
        void createPartnershipRequest_InvalidData() throws Exception {
            PartnershipRequestDto invalidRequest = PartnershipRequestDto.builder()
                    .requesterId(TEST_USER_1_ID)
                    .recipientId(TEST_USER_1_ID) // Same as requester - this is the error we want to test
                    .durationDays(30) // Valid duration so self-partnership check runs
                    .build();

            when(buddyPartnershipService.createPartnershipRequest(any()))
                    .thenThrow(new IllegalArgumentException("Self-partnership not allowed"));

            mockMvc.perform(post("/api/v1/buddy/partnerships/request")
                    .with(csrf())
                    .header("X-User-ID", TEST_USER_1_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Self-partnership not allowed"));
        }

        @Test
        @DisplayName("POST /api/v1/buddy/partnerships/request - User ID mismatch")
        void createPartnershipRequest_UserIdMismatch() throws Exception {
            PartnershipRequestDto mismatchRequest = PartnershipRequestDto.builder()
                    .requesterId(UUID.fromString("550e8400-e29b-41d4-a716-446655440002")) // Different from header
                    .recipientId(TEST_USER_2_ID)
                    .durationDays(30)
                    .build();

            mockMvc.perform(post("/api/v1/buddy/partnerships/request")
                    .with(csrf())
                    .header("X-User-ID", TEST_USER_1_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(mismatchRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Requester ID must match authenticated user"));

            verifyNoInteractions(buddyPartnershipService);
        }

        @Test
        @DisplayName("POST /api/v1/buddy/partnerships/request - Partnership conflict")
        void createPartnershipRequest_Conflict() throws Exception {
            when(buddyPartnershipService.createPartnershipRequest(any()))
                    .thenThrow(new PartnershipConflictException("Active partnership already exists"));

            mockMvc.perform(post("/api/v1/buddy/partnerships/request")
                    .with(csrf())
                    .header("X-User-ID", TEST_USER_1_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(testRequest)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Active partnership already exists"));
        }
    }

    @Nested
    @DisplayName("Partnership Approval/Rejection")
    class ApprovalRejectionTests {

        @Test
        @DisplayName("PUT /api/v1/buddy/partnerships/{id}/approve - Approve partnership")
        void approvePartnership_Success() throws Exception {
            PartnershipResponseDto approvedResponse = testResponse.toBuilder()
                    .status(PartnershipStatus.ACTIVE)
                    .startedAt(ZonedDateTime.now())
                    .isActive(true)
                    .isPending(false)
                    .build();

            when(buddyPartnershipService.approvePartnershipRequest(eq(testPartnershipId), eq(TEST_USER_2_ID)))
                    .thenReturn(approvedResponse);

            mockMvc.perform(put("/api/v1/buddy/partnerships/{id}/approve", testPartnershipId)
                    .with(csrf())
                    .header("X-User-ID", TEST_USER_2_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                    .andExpect(jsonPath("$.data.isActive").value(true))
                    .andExpect(jsonPath("$.data.isPending").value(false))
                    .andExpect(jsonPath("$.message").value("Partnership approved successfully"));

            verify(buddyPartnershipService).approvePartnershipRequest(testPartnershipId, TEST_USER_2_ID);
        }

        @Test
        @DisplayName("PUT /api/v1/buddy/partnerships/{id}/approve - Partnership not found")
        void approvePartnership_NotFound() throws Exception {
            when(buddyPartnershipService.approvePartnershipRequest(eq(testPartnershipId), eq(TEST_USER_2_ID)))
                    .thenThrow(new IllegalArgumentException("Partnership not found: " + testPartnershipId));

            mockMvc.perform(put("/api/v1/buddy/partnerships/{id}/approve", testPartnershipId)
                    .with(csrf())
                    .header("X-User-ID", TEST_USER_2_ID))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Partnership not found: " + testPartnershipId));
        }

        @Test
        @DisplayName("PUT /api/v1/buddy/partnerships/{id}/approve - Unauthorized user")
        void approvePartnership_Unauthorized() throws Exception {
            when(buddyPartnershipService.approvePartnershipRequest(eq(testPartnershipId), eq(UUID.fromString("550e8400-e29b-41d4-a716-446655440003"))))
                    .thenThrow(new IllegalArgumentException("User not involved in this partnership"));

            mockMvc.perform(put("/api/v1/buddy/partnerships/{id}/approve", testPartnershipId)
                    .with(csrf())
                    .header("X-User-ID", "550e8400-e29b-41d4-a716-446655440003"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("User not involved in this partnership"));
        }

        @Test
        @DisplayName("PUT /api/v1/buddy/partnerships/{id}/reject - Reject partnership")
        void rejectPartnership_Success() throws Exception {
            RejectPartnershipRequest rejectRequest = new RejectPartnershipRequest();
            rejectRequest.setReason("Not compatible with my schedule");

            doNothing().when(buddyPartnershipService)
                    .rejectPartnershipRequest(testPartnershipId, TEST_USER_2_ID, "Not compatible with my schedule");

            mockMvc.perform(put("/api/v1/buddy/partnerships/{id}/reject", testPartnershipId)
                    .with(csrf())
                    .header("X-User-ID", TEST_USER_2_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(rejectRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Partnership rejected successfully"));

            verify(buddyPartnershipService).rejectPartnershipRequest(testPartnershipId, TEST_USER_2_ID, "Not compatible with my schedule");
        }

        @Test
        @DisplayName("PUT /api/v1/buddy/partnerships/{id}/reject - Already processed")
        void rejectPartnership_AlreadyProcessed() throws Exception {
            RejectPartnershipRequest rejectRequest = new RejectPartnershipRequest();
            rejectRequest.setReason("Changed my mind");

            doThrow(new IllegalStateException("Partnership is not in pending status"))
                    .when(buddyPartnershipService)
                    .rejectPartnershipRequest(testPartnershipId, TEST_USER_2_ID, "Changed my mind");

            mockMvc.perform(put("/api/v1/buddy/partnerships/{id}/reject", testPartnershipId)
                    .with(csrf())
                    .header("X-User-ID", TEST_USER_2_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(rejectRequest)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Partnership is not in pending status"));
        }
    }

    @Nested
    @DisplayName("Partnership Lifecycle Management")
    class LifecycleManagementTests {

        @Test
        @DisplayName("GET /api/v1/buddy/partnerships - List user partnerships")
        void getPartnerships_Success() throws Exception {
            List<PartnershipResponseDto> partnerships = Arrays.asList(testResponse);
            when(buddyPartnershipService.findActivePartnershipsByUser(TEST_USER_1_ID))
                    .thenReturn(partnerships);

            mockMvc.perform(get("/api/v1/buddy/partnerships")
                    .header("X-User-ID", TEST_USER_1_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.partnerships").isArray())
                    .andExpect(jsonPath("$.data.partnerships", hasSize(1)))
                    .andExpect(jsonPath("$.data.partnerships[0].id").value(testPartnershipId.toString()))
                    .andExpect(jsonPath("$.data.totalCount").value(1));

            verify(buddyPartnershipService).findActivePartnershipsByUser(TEST_USER_1_ID);
        }

        @Test
        @DisplayName("GET /api/v1/buddy/partnerships - With status filter")
        void getPartnerships_WithStatusFilter() throws Exception {
            List<PartnershipResponseDto> partnerships = Arrays.asList(testResponse);
            when(buddyPartnershipService.findPartnershipsByStatus(PartnershipStatus.PENDING))
                    .thenReturn(partnerships);

            mockMvc.perform(get("/api/v1/buddy/partnerships")
                    .header("X-User-ID", TEST_USER_1_ID)
                    .param("status", "PENDING"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.partnerships", hasSize(1)))
                    .andExpect(jsonPath("$.data.status").value("PENDING"));

            verify(buddyPartnershipService).findPartnershipsByStatus(PartnershipStatus.PENDING);
        }

        @Test
        @DisplayName("GET /api/v1/buddy/partnerships/{id} - Get partnership details")
        void getPartnershipDetails_Success() throws Exception {
            when(buddyPartnershipService.findPartnershipById(testPartnershipId))
                    .thenReturn(testResponse);

            mockMvc.perform(get("/api/v1/buddy/partnerships/{id}", testPartnershipId)
                    .header("X-User-ID", TEST_USER_1_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(testPartnershipId.toString()))
                    .andExpect(jsonPath("$.data.status").value("PENDING"));

            verify(buddyPartnershipService).findPartnershipById(testPartnershipId);
        }

        @Test
        @DisplayName("GET /api/v1/buddy/partnerships/{id} - Partnership not found")
        void getPartnershipDetails_NotFound() throws Exception {
            when(buddyPartnershipService.findPartnershipById(testPartnershipId))
                    .thenReturn(null);

            mockMvc.perform(get("/api/v1/buddy/partnerships/{id}", testPartnershipId)
                    .header("X-User-ID", TEST_USER_1_ID))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Partnership not found"));
        }

        @Test
        @DisplayName("PUT /api/v1/buddy/partnerships/{id}/end - End partnership")
        void endPartnership_Success() throws Exception {
            EndPartnershipRequest endRequest = new EndPartnershipRequest();
            endRequest.setReason("Goals achieved");

            doNothing().when(buddyPartnershipService)
                    .endPartnership(testPartnershipId, TEST_USER_1_ID, "Goals achieved");

            mockMvc.perform(put("/api/v1/buddy/partnerships/{id}/end", testPartnershipId)
                    .with(csrf())
                    .header("X-User-ID", TEST_USER_1_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(endRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Partnership ended successfully"));

            verify(buddyPartnershipService).endPartnership(testPartnershipId, TEST_USER_1_ID, "Goals achieved");
        }

        @Test
        @DisplayName("PUT /api/v1/buddy/partnerships/{id}/pause - Pause partnership")
        void pausePartnership_Success() throws Exception {
            PausePartnershipRequest pauseRequest = new PausePartnershipRequest();
            pauseRequest.setReason("Temporary break");

            doNothing().when(buddyPartnershipService)
                    .pausePartnership(testPartnershipId, TEST_USER_1_ID, "Temporary break");

            mockMvc.perform(put("/api/v1/buddy/partnerships/{id}/pause", testPartnershipId)
                    .with(csrf())
                    .header("X-User-ID", TEST_USER_1_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(pauseRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Partnership paused successfully"));

            verify(buddyPartnershipService).pausePartnership(testPartnershipId, TEST_USER_1_ID, "Temporary break");
        }

        @Test
        @DisplayName("PUT /api/v1/buddy/partnerships/{id}/resume - Resume partnership")
        void resumePartnership_Success() throws Exception {
            PartnershipResponseDto resumedResponse = testResponse.toBuilder()
                    .status(PartnershipStatus.ACTIVE)
                    .isActive(true)
                    .isPaused(false)
                    .build();

            when(buddyPartnershipService.resumePartnership(eq(testPartnershipId), eq(TEST_USER_1_ID)))
                    .thenReturn(resumedResponse);

            mockMvc.perform(put("/api/v1/buddy/partnerships/{id}/resume", testPartnershipId)
                    .with(csrf())
                    .header("X-User-ID", TEST_USER_1_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                    .andExpect(jsonPath("$.data.isActive").value(true))
                    .andExpect(jsonPath("$.message").value("Partnership resumed successfully"));

            verify(buddyPartnershipService).resumePartnership(testPartnershipId, TEST_USER_1_ID);
        }
    }

    @Nested
    @DisplayName("Partnership Health and Analytics")
    class HealthAndAnalyticsTests {

        @Test
        @DisplayName("GET /api/v1/buddy/partnerships/{id}/health - Get partnership health")
        void getPartnershipHealth_Success() throws Exception {
            when(buddyPartnershipService.calculatePartnershipHealth(testPartnershipId))
                    .thenReturn(testHealth);

            mockMvc.perform(get("/api/v1/buddy/partnerships/{id}/health", testPartnershipId)
                    .header("X-User-ID", TEST_USER_1_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.partnershipId").value(testPartnershipId.toString()))
                    .andExpect(jsonPath("$.data.overallHealthScore").value(0.85))
                    .andExpect(jsonPath("$.data.healthStatus").value("GOOD"))
                    .andExpect(jsonPath("$.data.communicationScore").value(0.9))
                    .andExpect(jsonPath("$.data.interventionSuggestions", hasSize(1)));

            verify(buddyPartnershipService).calculatePartnershipHealth(testPartnershipId);
        }

        @Test
        @DisplayName("GET /api/v1/buddy/partnerships/statistics - Get partnership statistics")
        void getPartnershipStatistics_Success() throws Exception {
            when(buddyPartnershipService.getPartnershipStatistics(TEST_USER_1_ID))
                    .thenReturn(testStatistics);

            mockMvc.perform(get("/api/v1/buddy/partnerships/statistics")
                    .header("X-User-ID", TEST_USER_1_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.userId").value(TEST_USER_1_ID.toString()))
                    .andExpect(jsonPath("$.data.totalPartnerships").value(5))
                    .andExpect(jsonPath("$.data.activePartnerships").value(2))
                    .andExpect(jsonPath("$.data.completedPartnerships").value(2))
                    .andExpect(jsonPath("$.data.averageHealthScore").value(0.8))
                    .andExpect(jsonPath("$.data.partnershipSuccessRate").value(0.8));

            verify(buddyPartnershipService).getPartnershipStatistics(TEST_USER_1_ID);
        }

        @Test
        @DisplayName("GET /api/v1/buddy/partnerships/summary - Get partnership summary")
        void getPartnershipSummary_Success() throws Exception {
            Map<String, Object> summary = Map.of(
                    "userId", TEST_USER_1_ID,
                    "totalPartnerships", 5,
                    "activePartnerships", 2L,
                    "pendingRequests", 1L,
                    "averageHealthScore", BigDecimal.valueOf(0.8),
                    "canCreateNewPartnership", true
            );

            when(buddyPartnershipService.getUserPartnershipSummary(TEST_USER_1_ID))
                    .thenReturn(summary);

            mockMvc.perform(get("/api/v1/buddy/partnerships/summary")
                    .header("X-User-ID", TEST_USER_1_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.userId").value(TEST_USER_1_ID.toString()))
                    .andExpect(jsonPath("$.data.totalPartnerships").value(5))
                    .andExpect(jsonPath("$.data.activePartnerships").value(2))
                    .andExpect(jsonPath("$.data.canCreateNewPartnership").value(true));

            verify(buddyPartnershipService).getUserPartnershipSummary(TEST_USER_1_ID);
        }

        @Test
        @DisplayName("GET /api/v1/buddy/partnerships/{id}/engagement - Get engagement metrics")
        void getEngagementMetrics_Success() throws Exception {
            Map<String, Object> metrics = Map.of(
                    "partnershipId", testPartnershipId,
                    "healthScore", BigDecimal.valueOf(0.85),
                    "checkinsLast7Days", 5,
                    "mutualGoals", 3,
                    "daysSinceLastInteraction", 1L
            );

            when(buddyPartnershipService.calculateEngagementMetrics(testPartnershipId))
                    .thenReturn(metrics);

            mockMvc.perform(get("/api/v1/buddy/partnerships/{id}/engagement", testPartnershipId)
                    .header("X-User-ID", TEST_USER_1_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.partnershipId").value(testPartnershipId.toString()))
                    .andExpect(jsonPath("$.data.healthScore").value(0.85))
                    .andExpect(jsonPath("$.data.checkinsLast7Days").value(5))
                    .andExpect(jsonPath("$.data.mutualGoals").value(3));

            verify(buddyPartnershipService).calculateEngagementMetrics(testPartnershipId);
        }
    }

    @Nested
    @DisplayName("Advanced Partnership Operations")
    class AdvancedOperationsTests {

        @Test
        @DisplayName("GET /api/v1/buddy/partnerships/pending - Get pending requests")
        void getPendingRequests_Success() throws Exception {
            List<PartnershipResponseDto> pendingRequests = Arrays.asList(testResponse);
            when(buddyPartnershipService.getPendingRequests(TEST_USER_1_ID))
                    .thenReturn(pendingRequests);

            mockMvc.perform(get("/api/v1/buddy/partnerships/pending")
                    .header("X-User-ID", TEST_USER_1_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.requests").isArray())
                    .andExpect(jsonPath("$.data.requests", hasSize(1)))
                    .andExpect(jsonPath("$.data.requests[0].status").value("PENDING"));

            verify(buddyPartnershipService).getPendingRequests(TEST_USER_1_ID);
        }

        @Test
        @DisplayName("POST /api/v1/buddy/partnerships/{id}/renew - Renew partnership")
        void renewPartnership_Success() throws Exception {
            RenewPartnershipRequest renewRequest = new RenewPartnershipRequest();
            renewRequest.setExtensionDays(30);

            PartnershipResponseDto renewedResponse = testResponse.toBuilder()
                    .durationDays(60) // Extended
                    .build();

            when(buddyPartnershipService.renewPartnership(eq(testPartnershipId), eq(TEST_USER_1_ID), eq(30)))
                    .thenReturn(renewedResponse);

            mockMvc.perform(post("/api/v1/buddy/partnerships/{id}/renew", testPartnershipId)
                    .with(csrf())
                    .header("X-User-ID", TEST_USER_1_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(renewRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.durationDays").value(60))
                    .andExpect(jsonPath("$.message").value("Partnership renewed successfully"));

            verify(buddyPartnershipService).renewPartnership(testPartnershipId, TEST_USER_1_ID, 30);
        }

        @Test
        @DisplayName("POST /api/v1/buddy/partnerships/search - Search partnerships")
        void searchPartnerships_Success() throws Exception {
            PartnershipSearchRequest searchRequest = new PartnershipSearchRequest();
            searchRequest.setStatus("ACTIVE");
            searchRequest.setPage(0);
            searchRequest.setSize(10);

            Page<PartnershipResponseDto> searchResults = new PageImpl<>(
                    Arrays.asList(testResponse),
                    PageRequest.of(0, 10),
                    1
            );

            when(buddyPartnershipService.searchPartnerships(anyMap(), any()))
                    .thenReturn(searchResults);

            mockMvc.perform(post("/api/v1/buddy/partnerships/search")
                    .with(csrf())
                    .header("X-User-ID", TEST_USER_1_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(searchRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content").isArray())
                    .andExpect(jsonPath("$.data.content", hasSize(1)))
                    .andExpect(jsonPath("$.data.totalElements").value(1))
                    .andExpect(jsonPath("$.data.totalPages").value(1))
                    .andExpect(jsonPath("$.data.pageable.pageNumber").value(0));
        }

        @Test
        @DisplayName("POST /api/v1/buddy/partnerships/{id}/dissolve - Initiate dissolution")
        void initiatePartnershipDissolution_Success() throws Exception {
            DissolutionRequestDto dissolutionRequest = DissolutionRequestDto.builder()
                    .partnershipId(testPartnershipId)
                    .initiatorId(TEST_USER_1_ID.toString())
                    .dissolutionType(DissolutionRequestDto.DissolutionType.MUTUAL)
                    .reason("Natural conclusion")
                    .partnerFeedback("Great experience")
                    .build();

            Map<String, Object> dissolutionResult = Map.of(
                    "partnershipId", testPartnershipId,
                    "status", "INITIATED",
                    "requiresConsent", true,
                    "dissolutionType", "MUTUAL"
            );

            when(buddyPartnershipService.initiateDissolution(org.mockito.ArgumentMatchers.any(DissolutionRequestDto.class)))
                    .thenReturn(dissolutionResult);

            mockMvc.perform(post("/api/v1/buddy/partnerships/{id}/dissolve", testPartnershipId)
                    .with(csrf())
                    .header("X-User-ID", TEST_USER_1_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(dissolutionRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.partnershipId").value(testPartnershipId.toString()))
                    .andExpect(jsonPath("$.data.status").value("INITIATED"))
                    .andExpect(jsonPath("$.data.requiresConsent").value(true))
                    .andExpect(jsonPath("$.message").value("Partnership dissolution initiated"));

            verify(buddyPartnershipService).initiateDissolution(org.mockito.ArgumentMatchers.any(DissolutionRequestDto.class));
        }
    }

    @Nested
    @DisplayName("Input Validation and Error Handling")
    class ValidationAndErrorHandlingTests {

        @Test
        
        @DisplayName("All endpoints - Missing user ID header")
        void allEndpoints_MissingUserIdHeader() throws Exception {
            mockMvc.perform(get("/api/v1/buddy/partnerships"))
                    .andExpect(status().isBadRequest());

            mockMvc.perform(post("/api/v1/buddy/partnerships/request").with(csrf()))
                    .andExpect(status().isBadRequest());
        }

        @Test
        
        @DisplayName("Partnership requests - Invalid JSON payload")
        void partnershipRequest_InvalidJson() throws Exception {
            mockMvc.perform(post("/api/v1/buddy/partnerships/request")
                    .with(csrf())
                    .header("X-User-ID", TEST_USER_1_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{invalid json"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value(containsString("Invalid JSON")));
        }

        @Test
        
        @DisplayName("Partnership operations - Invalid UUID")
        void partnershipOperations_InvalidUuid() throws Exception {
            mockMvc.perform(get("/api/v1/buddy/partnerships/{id}", "invalid-uuid")
                    .header("X-User-ID", TEST_USER_1_ID))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value(containsString("Invalid partnership ID format")));
        }

        @Test
        @DisplayName("Service unavailable handling")
        void serviceUnavailable_Handling() throws Exception {
            when(buddyPartnershipService.findActivePartnershipsByUser(any()))
                    .thenThrow(new RuntimeException("Database connection failed"));

            mockMvc.perform(get("/api/v1/buddy/partnerships")
                    .header("X-User-ID", TEST_USER_1_ID))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Internal server error"));
        }

        @Test
        @DisplayName("Validation error handling")
        void validationError_Handling() throws Exception {
            PartnershipRequestDto invalidRequest = PartnershipRequestDto.builder()
                    .requesterId(TEST_USER_1_ID)
                    .recipientId(null) // Missing recipient
                    .durationDays(0) // Invalid duration
                    .build();

            mockMvc.perform(post("/api/v1/buddy/partnerships/request")
                    .with(csrf())
                    .header("X-User-ID", TEST_USER_1_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.validationErrors").exists());
        }
    }

    // Helper request classes for test data
    private static class RejectPartnershipRequest {
        private String reason;

        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }

    private static class EndPartnershipRequest {
        private String reason;

        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }

    private static class PausePartnershipRequest {
        private String reason;

        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }

    private static class RenewPartnershipRequest {
        private Integer extensionDays;

        public Integer getExtensionDays() { return extensionDays; }
        public void setExtensionDays(Integer extensionDays) { this.extensionDays = extensionDays; }
    }

    private static class PartnershipSearchRequest {
        private String status;
        private Integer page;
        private Integer size;

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public Integer getPage() { return page; }
        public void setPage(Integer page) { this.page = page; }
        public Integer getSize() { return size; }
        public void setSize(Integer size) { this.size = size; }
    }
}