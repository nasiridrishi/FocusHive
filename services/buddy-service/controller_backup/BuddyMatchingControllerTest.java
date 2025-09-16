package com.focushive.buddy.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.focushive.buddy.dto.*;
import com.focushive.buddy.service.BuddyMatchingService;
import com.focushive.buddy.exception.BuddyServiceException;
import com.focushive.buddy.exception.InsufficientCompatibilityException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import com.focushive.buddy.config.TestSecurityConfig;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Comprehensive test suite for BuddyMatchingController.
 * Tests all REST endpoints for matching queue, compatibility calculation, and preferences.
 *
 * TDD RED PHASE: These tests are designed to FAIL initially until controller is implemented.
 */
@WebMvcTest(BuddyMatchingController.class)
@Import(TestSecurityConfig.class)
class BuddyMatchingControllerTest {

    // Test UUIDs - using valid UUID format
    private static final String TEST_USER_ID = "550e8400-e29b-41d4-a716-446655440001";
    private static final String TEST_MATCH_ID = "550e8400-e29b-41d4-a716-446655440002";
    private static final String TEST_USER_1 = "550e8400-e29b-41d4-a716-446655440003";
    private static final String TEST_USER_2 = "550e8400-e29b-41d4-a716-446655440004";
    private static final String TEST_USER_3 = "550e8400-e29b-41d4-a716-446655440005";
    private static final String ADMIN_USER_ID = "550e8400-e29b-41d4-a716-446655440006";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BuddyMatchingService buddyMatchingService;

    private MatchingPreferencesDto testPreferences;
    private PotentialMatchDto testMatch;
    private CompatibilityScoreDto testCompatibility;

    @BeforeEach
    void setUp() {
        testPreferences = MatchingPreferencesDto.builder()
                .userId(TEST_USER_ID)
                .matchingEnabled(true)
                .preferredTimezone("UTC")
                .timezoneFlexibility(2)
                .minCommitmentHours(10)
                .maxPartners(3)
                .language("en")
                .build();

        testMatch = PotentialMatchDto.builder()
                .userId(TEST_MATCH_ID)
                .displayName("Test Match")
                .timezone("UTC")
                .compatibilityScore(0.85)
                .commonInterests(Arrays.asList("coding", "reading"))
                .focusAreas(Arrays.asList("productivity", "learning"))
                .experienceLevel("intermediate")
                .communicationStyle("moderate")
                .personalityType("analytical")
                .timezoneOffsetHours(0)
                .reasonForMatch("High compatibility score")
                .build();

        testCompatibility = CompatibilityScoreDto.builder()
                .overallScore(0.85)
                .timezoneScore(0.9)
                .interestScore(0.8)
                .goalAlignmentScore(0.85)
                .activityPatternScore(0.8)
                .communicationStyleScore(0.9)
                .personalityScore(0.75)
                .explanation("Excellent match with shared interests")
                .build();
    }

    @Nested
    @DisplayName("Matching Queue Operations")
    class MatchingQueueTests {

        @Test
        @DisplayName("POST /api/v1/buddy/matching/queue - Successfully join matching queue")
        void joinMatchingQueue_Success() throws Exception {
            when(buddyMatchingService.addToMatchingQueue(TEST_USER_ID)).thenReturn(true);

            mockMvc.perform(post("/api/v1/buddy/matching/queue")
                    .with(csrf())
                    .header("X-User-ID", TEST_USER_ID)
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Successfully joined matching queue"))
                    .andExpect(jsonPath("$.data.inQueue").value(true));

            verify(buddyMatchingService).addToMatchingQueue(TEST_USER_ID);
        }

        @Test
        
        @DisplayName("POST /api/v1/buddy/matching/queue - Missing user header")
        void joinMatchingQueue_MissingUserHeader() throws Exception {
            mockMvc.perform(post("/api/v1/buddy/matching/queue")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("User ID header is required"));

            verifyNoInteractions(buddyMatchingService);
        }

        @Test
        @DisplayName("POST /api/v1/buddy/matching/queue - Service exception")
        void joinMatchingQueue_ServiceException() throws Exception {
            when(buddyMatchingService.addToMatchingQueue(TEST_USER_ID))
                    .thenThrow(new BuddyServiceException("User not eligible for matching"));

            mockMvc.perform(post("/api/v1/buddy/matching/queue")
                    .with(csrf())
                    .header("X-User-ID", TEST_USER_ID)
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("User not eligible for matching"));
        }

        @Test
        @DisplayName("DELETE /api/v1/buddy/matching/queue - Successfully leave matching queue")
        void leaveMatchingQueue_Success() throws Exception {
            when(buddyMatchingService.removeFromMatchingQueue(TEST_USER_ID)).thenReturn(true);

            mockMvc.perform(delete("/api/v1/buddy/matching/queue")
                    .with(csrf())
                    .header("X-User-ID", TEST_USER_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Successfully left matching queue"))
                    .andExpect(jsonPath("$.data.inQueue").value(false));

            verify(buddyMatchingService).removeFromMatchingQueue(TEST_USER_ID);
        }

        @Test
        @DisplayName("DELETE /api/v1/buddy/matching/queue - User not in queue")
        void leaveMatchingQueue_NotInQueue() throws Exception {
            when(buddyMatchingService.removeFromMatchingQueue(TEST_USER_ID)).thenReturn(false);

            mockMvc.perform(delete("/api/v1/buddy/matching/queue")
                    .with(csrf())
                    .header("X-User-ID", TEST_USER_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("User was not in matching queue"))
                    .andExpect(jsonPath("$.data.inQueue").value(false));
        }
    }

    @Nested
    @DisplayName("Match Suggestions")
    class MatchSuggestionsTests {

        @Test
        @DisplayName("GET /api/v1/buddy/matching/suggestions - Get match suggestions with default limit")
        void getMatchSuggestions_DefaultLimit() throws Exception {
            List<PotentialMatchDto> matches = Arrays.asList(testMatch);
            when(buddyMatchingService.findPotentialMatches(eq(TEST_USER_ID), eq(10))).thenReturn(matches);

            mockMvc.perform(get("/api/v1/buddy/matching/suggestions")
                    .header("X-User-ID", TEST_USER_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.matches").isArray())
                    .andExpect(jsonPath("$.data.matches", hasSize(1)))
                    .andExpect(jsonPath("$.data.matches[0].userId").value(TEST_MATCH_ID))
                    .andExpect(jsonPath("$.data.matches[0].compatibilityScore").value(0.85))
                    .andExpect(jsonPath("$.data.matches[0].commonInterests", contains("coding", "reading")))
                    .andExpect(jsonPath("$.data.totalMatches").value(1));

            verify(buddyMatchingService).findPotentialMatches(TEST_USER_ID, 10);
        }

        @Test
        @DisplayName("GET /api/v1/buddy/matching/suggestions - Custom limit and threshold")
        void getMatchSuggestions_CustomParameters() throws Exception {
            List<PotentialMatchDto> matches = Arrays.asList(testMatch);
            when(buddyMatchingService.findPotentialMatchesWithThreshold(eq(TEST_USER_ID), eq(5), eq(0.7)))
                    .thenReturn(matches);

            mockMvc.perform(get("/api/v1/buddy/matching/suggestions")
                    .header("X-User-ID", TEST_USER_ID)
                    .param("limit", "5")
                    .param("threshold", "0.7"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.matches", hasSize(1)))
                    .andExpect(jsonPath("$.data.threshold").value(0.7));

            verify(buddyMatchingService).findPotentialMatchesWithThreshold(TEST_USER_ID, 5, 0.7);
        }

        @Test
        
        @DisplayName("GET /api/v1/buddy/matching/suggestions - Invalid limit parameter")
        void getMatchSuggestions_InvalidLimit() throws Exception {
            mockMvc.perform(get("/api/v1/buddy/matching/suggestions")
                    .header("X-User-ID", TEST_USER_ID)
                    .param("limit", "-1"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Limit must be between 1 and 100"));

            verifyNoInteractions(buddyMatchingService);
        }

        @Test
        @DisplayName("GET /api/v1/buddy/matching/suggestions - No matches found")
        void getMatchSuggestions_NoMatches() throws Exception {
            when(buddyMatchingService.findPotentialMatches(eq(TEST_USER_ID), eq(10)))
                    .thenReturn(Arrays.asList());

            mockMvc.perform(get("/api/v1/buddy/matching/suggestions")
                    .header("X-User-ID", TEST_USER_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.matches").isEmpty())
                    .andExpect(jsonPath("$.data.totalMatches").value(0))
                    .andExpect(jsonPath("$.message").value("No potential matches found"));
        }

        @Test
        @DisplayName("GET /api/v1/buddy/matching/suggestions - Service exception")
        void getMatchSuggestions_ServiceException() throws Exception {
            when(buddyMatchingService.findPotentialMatches(eq(TEST_USER_ID), eq(10)))
                    .thenThrow(new IllegalStateException("Matching preferences not found"));

            mockMvc.perform(get("/api/v1/buddy/matching/suggestions")
                    .header("X-User-ID", TEST_USER_ID))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Matching preferences not found"));
        }
    }

    @Nested
    @DisplayName("Compatibility Calculation")
    class CompatibilityCalculationTests {

        @Test
        @DisplayName("POST /api/v1/buddy/matching/calculate - Calculate compatibility score")
        void calculateCompatibility_Success() throws Exception {
            when(buddyMatchingService.getCompatibilityBreakdown(eq(TEST_USER_ID), eq(TEST_MATCH_ID)))
                    .thenReturn(testCompatibility);

            CompatibilityCalculationRequest request = new CompatibilityCalculationRequest();
            request.setTargetUserId(TEST_MATCH_ID);

            mockMvc.perform(post("/api/v1/buddy/matching/calculate")
                    .with(csrf())
                    .header("X-User-ID", TEST_USER_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.overallScore").value(0.85))
                    .andExpect(jsonPath("$.data.timezoneScore").value(0.9))
                    .andExpect(jsonPath("$.data.interestScore").value(0.8))
                    .andExpect(jsonPath("$.data.explanation").value("Excellent match with shared interests"));

            verify(buddyMatchingService).getCompatibilityBreakdown(TEST_USER_ID, TEST_MATCH_ID);
        }

        @Test
        @DisplayName("POST /api/v1/buddy/matching/calculate - Missing target user ID")
        void calculateCompatibility_MissingTargetUser() throws Exception {
            CompatibilityCalculationRequest request = new CompatibilityCalculationRequest();

            mockMvc.perform(post("/api/v1/buddy/matching/calculate")
                    .with(csrf())
                    .header("X-User-ID", TEST_USER_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Target user ID is required"));

            verifyNoInteractions(buddyMatchingService);
        }

        @Test
        @DisplayName("POST /api/v1/buddy/matching/calculate - Self-compatibility not allowed")
        void calculateCompatibility_SelfMatch() throws Exception {
            CompatibilityCalculationRequest request = new CompatibilityCalculationRequest();
            request.setTargetUserId(TEST_USER_ID);

            mockMvc.perform(post("/api/v1/buddy/matching/calculate")
                    .with(csrf())
                    .header("X-User-ID", TEST_USER_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Cannot calculate compatibility with yourself"));

            verifyNoInteractions(buddyMatchingService);
        }

        @Test
        @DisplayName("POST /api/v1/buddy/matching/calculate - Insufficient compatibility")
        void calculateCompatibility_InsufficientCompatibility() throws Exception {
            when(buddyMatchingService.getCompatibilityBreakdown(eq(TEST_USER_ID), eq(TEST_MATCH_ID)))
                    .thenThrow(new InsufficientCompatibilityException("Compatibility score too low: 0.2"));

            CompatibilityCalculationRequest request = new CompatibilityCalculationRequest();
            request.setTargetUserId(TEST_MATCH_ID);

            mockMvc.perform(post("/api/v1/buddy/matching/calculate")
                    .with(csrf())
                    .header("X-User-ID", TEST_USER_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Compatibility score too low: 0.2"));
        }
    }

    @Nested
    @DisplayName("Matching Preferences Management")
    class PreferencesManagementTests {

        @Test
        @DisplayName("GET /api/v1/buddy/matching/preferences - Get existing preferences")
        void getMatchingPreferences_Existing() throws Exception {
            when(buddyMatchingService.getMatchingPreferences(TEST_USER_ID))
                    .thenReturn(testPreferences);

            mockMvc.perform(get("/api/v1/buddy/matching/preferences")
                    .header("X-User-ID", TEST_USER_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.userId").value(TEST_USER_ID))
                    .andExpect(jsonPath("$.data.matchingEnabled").value(true))
                    .andExpect(jsonPath("$.data.preferredTimezone").value("UTC"))
                    .andExpect(jsonPath("$.data.timezoneFlexibility").value(2))
                    .andExpect(jsonPath("$.data.maxPartners").value(3));

            verify(buddyMatchingService).getMatchingPreferences(TEST_USER_ID);
        }

        @Test
        @DisplayName("GET /api/v1/buddy/matching/preferences - Create default if not exists")
        void getMatchingPreferences_CreateDefault() throws Exception {
            when(buddyMatchingService.getMatchingPreferences(TEST_USER_ID))
                    .thenReturn(null);
            when(buddyMatchingService.getOrCreateMatchingPreferences(TEST_USER_ID))
                    .thenReturn(testPreferences);

            mockMvc.perform(get("/api/v1/buddy/matching/preferences")
                    .header("X-User-ID", TEST_USER_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.userId").value(TEST_USER_ID))
                    .andExpect(jsonPath("$.message").value("Default preferences created"));

            verify(buddyMatchingService).getMatchingPreferences(TEST_USER_ID);
            verify(buddyMatchingService).getOrCreateMatchingPreferences(TEST_USER_ID);
        }

        @Test
        @DisplayName("PUT /api/v1/buddy/matching/preferences - Update preferences")
        void updateMatchingPreferences_Success() throws Exception {
            MatchingPreferencesDto updatedPreferences = MatchingPreferencesDto.builder()
                    .userId(TEST_USER_ID)
                    .matchingEnabled(false)
                    .preferredTimezone("America/New_York")
                    .timezoneFlexibility(4)
                    .minCommitmentHours(20)
                    .maxPartners(2)
                    .language("en")
                    .build();

            when(buddyMatchingService.updateMatchingPreferences(org.mockito.ArgumentMatchers.any(MatchingPreferencesDto.class)))
                    .thenReturn(updatedPreferences);

            mockMvc.perform(put("/api/v1/buddy/matching/preferences")
                    .with(csrf())
                    .header("X-User-ID", TEST_USER_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(updatedPreferences)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.matchingEnabled").value(false))
                    .andExpect(jsonPath("$.data.preferredTimezone").value("America/New_York"))
                    .andExpect(jsonPath("$.data.timezoneFlexibility").value(4))
                    .andExpect(jsonPath("$.message").value("Matching preferences updated successfully"));

            verify(buddyMatchingService).updateMatchingPreferences(org.mockito.ArgumentMatchers.any(MatchingPreferencesDto.class));
        }

        @Test
        @DisplayName("PUT /api/v1/buddy/matching/preferences - Invalid timezone")
        void updateMatchingPreferences_InvalidTimezone() throws Exception {
            MatchingPreferencesDto invalidPreferences = MatchingPreferencesDto.builder()
                    .userId(TEST_USER_ID)
                    .preferredTimezone("Invalid/Timezone")
                    .build();

            when(buddyMatchingService.updateMatchingPreferences(org.mockito.ArgumentMatchers.any(MatchingPreferencesDto.class)))
                    .thenThrow(new IllegalArgumentException("Invalid timezone: Invalid/Timezone"));

            mockMvc.perform(put("/api/v1/buddy/matching/preferences")
                    .with(csrf())
                    .header("X-User-ID", TEST_USER_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidPreferences)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Invalid timezone: Invalid/Timezone"));
        }

        @Test
        @DisplayName("PUT /api/v1/buddy/matching/preferences - User ID mismatch")
        void updateMatchingPreferences_UserIdMismatch() throws Exception {
            MatchingPreferencesDto mismatchedPreferences = MatchingPreferencesDto.builder()
                    .userId("different123")
                    .matchingEnabled(true)
                    .build();

            mockMvc.perform(put("/api/v1/buddy/matching/preferences")
                    .with(csrf())
                    .header("X-User-ID", TEST_USER_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(mismatchedPreferences)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("User ID in request body must match header"));

            verifyNoInteractions(buddyMatchingService);
        }
    }

    @Nested
    @DisplayName("Queue Status and Management")
    class QueueStatusTests {

        @Test
        @DisplayName("GET /api/v1/buddy/matching/queue/status - Get queue status")
        void getQueueStatus_Success() throws Exception {
            when(buddyMatchingService.isUserInMatchingQueue(TEST_USER_ID)).thenReturn(true);

            mockMvc.perform(get("/api/v1/buddy/matching/queue/status")
                    .header("X-User-ID", TEST_USER_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.inQueue").value(true))
                    .andExpect(jsonPath("$.data.userId").value(TEST_USER_ID));

            verify(buddyMatchingService).isUserInMatchingQueue(TEST_USER_ID);
        }

        @Test
        @DisplayName("GET /api/v1/buddy/matching/queue/size - Get queue size (admin only)")
        void getQueueSize_AdminAccess() throws Exception {
            when(buddyMatchingService.getUsersInMatchingQueue())
                    .thenReturn(java.util.Set.of(TEST_USER_1, TEST_USER_2, TEST_USER_3));

            mockMvc.perform(get("/api/v1/buddy/matching/queue/size")
                    .header("X-User-ID", ADMIN_USER_ID)
                    .header("X-User-Role", "ADMIN"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.queueSize").value(3));

            verify(buddyMatchingService).getUsersInMatchingQueue();
        }

        @Test
        @DisplayName("GET /api/v1/buddy/matching/queue/size - Forbidden for regular users")
        void getQueueSize_RegularUserForbidden() throws Exception {
            mockMvc.perform(get("/api/v1/buddy/matching/queue/size")
                    .header("X-User-ID", TEST_USER_ID))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Admin access required"));

            verifyNoInteractions(buddyMatchingService);
        }
    }

    @Nested
    @DisplayName("Input Validation and Error Handling")
    class ValidationAndErrorHandlingTests {

        @Test
        
        @DisplayName("All endpoints - Missing user ID header")
        void allEndpoints_MissingUserIdHeader() throws Exception {
            // Test multiple endpoints without user ID header
            mockMvc.perform(get("/api/v1/buddy/matching/suggestions"))
                    .andExpect(status().isBadRequest());

            mockMvc.perform(post("/api/v1/buddy/matching/queue").with(csrf()))
                    .andExpect(status().isBadRequest());

            mockMvc.perform(delete("/api/v1/buddy/matching/queue").with(csrf()))
                    .andExpect(status().isBadRequest());
        }

        @Test
        
        @DisplayName("Compatibility calculation - Invalid JSON payload")
        void calculateCompatibility_InvalidJson() throws Exception {
            mockMvc.perform(post("/api/v1/buddy/matching/calculate")
                    .with(csrf())
                    .header("X-User-ID", TEST_USER_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{invalid json"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value(containsString("Invalid JSON")));
        }

        @Test
        
        @DisplayName("Suggestions - Invalid threshold parameter")
        void getMatchSuggestions_InvalidThreshold() throws Exception {
            mockMvc.perform(get("/api/v1/buddy/matching/suggestions")
                    .header("X-User-ID", TEST_USER_ID)
                    .param("threshold", "1.5"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Threshold must be between 0.0 and 1.0"));
        }

        @Test
        @DisplayName("All endpoints - Service unavailable")
        void allEndpoints_ServiceUnavailable() throws Exception {
            when(buddyMatchingService.findPotentialMatches(anyString(), anyInt()))
                    .thenThrow(new RuntimeException("Database connection failed"));

            mockMvc.perform(get("/api/v1/buddy/matching/suggestions")
                    .header("X-User-ID", TEST_USER_ID))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Internal server error"));
        }
    }

    /**
     * Helper class for compatibility calculation request body
     */
    private static class CompatibilityCalculationRequest {
        private String targetUserId;

        public String getTargetUserId() {
            return targetUserId;
        }

        public void setTargetUserId(String targetUserId) {
            this.targetUserId = targetUserId;
        }
    }
}