package com.focushive.buddy.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.focushive.buddy.config.TestSecurityConfig;
import com.focushive.buddy.constant.CheckInType;
import com.focushive.buddy.constant.MoodType;
import com.focushive.buddy.controller.BuddyCheckinController;
import com.focushive.buddy.dto.*;
import com.focushive.buddy.service.BuddyCheckinService;
import com.focushive.buddy.exception.BuddyServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Comprehensive test suite for BuddyCheckinController.
 * Tests all REST endpoints for check-ins, analytics, streaks, and accountability.
 *
 * TDD RED PHASE: These tests are designed to FAIL initially until controller is implemented.
 */
@WebMvcTest(value = BuddyCheckinController.class, excludeAutoConfiguration = {
    org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
    org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration.class,
    org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration.class
}, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = {
    com.focushive.buddy.validation.UserIdHeaderValidator.class,
    com.focushive.buddy.security.SimpleJwtAuthenticationFilter.class,
    com.focushive.buddy.config.SecurityConfig.class,
    com.focushive.buddy.config.SimpleSecurityConfig.class
}))
@Import(TestSecurityConfig.class)
class BuddyCheckinControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BuddyCheckinService buddyCheckinService;

    private UUID testUserId;
    private UUID testPartnershipId;
    private UUID testCheckinId;
    private CheckinRequestDto testCheckinRequest;
    private CheckinResponseDto testCheckinResponse;
    private WeeklyReviewDto testWeeklyReview;
    private StreakStatisticsDto testStreakStats;
    private AccountabilityScoreDto testAccountabilityScore;
    private CheckinAnalyticsDto testAnalytics;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testPartnershipId = UUID.randomUUID();
        testCheckinId = UUID.randomUUID();

        testCheckinRequest = CheckinRequestDto.builder()
                .partnershipId(testPartnershipId)
                .checkinType(CheckInType.DAILY)
                .mood(MoodType.MOTIVATED)
                .productivityRating(8)
                .focusHours(6)
                .content("Good productive day overall")
                .notes("Good productive day overall")
                .build();

        testCheckinResponse = CheckinResponseDto.builder()
                .id(testCheckinId)
                .userId(testUserId)
                .partnershipId(testPartnershipId)
                .checkinType(CheckInType.DAILY)
                .mood(MoodType.MOTIVATED)
                .productivityRating(7)
                .content("Good productive day overall")
                .createdAt(LocalDateTime.now())
                .summary("Daily check-in completed")
                .build();

        testWeeklyReview = WeeklyReviewDto.builder()
                .partnershipId(testPartnershipId)
                .weekStartDate(LocalDate.now().minusDays(6))
                .weekEndDate(LocalDate.now())
                .checkinsThisWeek(6)
                .averageProductivity(7.5)
                .weeklyProgress("Good progress")
                .accomplishments("Launched new feature, Improved test coverage")
                .challengesFaced("Deadline pressure, Technical complexity")
                .nextWeekGoals("Complete testing, Deploy to staging")
                .build();

        testStreakStats = StreakStatisticsDto.builder()
                .currentDailyStreak(15)
                .currentWeeklyStreak(3)
                .longestDailyStreak(23)
                .longestWeeklyStreak(8)
                .lastCheckinDate(LocalDate.now())
                .isOnStreak(true)
                .daysUntilStreakBreak(1)
                .build();

        testAccountabilityScore = AccountabilityScoreDto.builder()
                .score(BigDecimal.valueOf(8.5))
                .level("EXCELLENT")
                .percentage("85%")
                .checkinsCompleted(45)
                .goalsAchieved(12)
                .responseRate(BigDecimal.valueOf(0.9))
                .streakDays(15)
                .calculatedAt(LocalDateTime.now())
                .improvement("POSITIVE")
                .build();

        testAnalytics = CheckinAnalyticsDto.builder()
                .totalCheckins(25)
                .dailyCheckins(15)
                .weeklyCheckins(10)
                .averageProductivity(7.2)
                .completionRate(0.83)
                .mostProductiveDay("Wednesday")
                .recommendations("Consider maintaining current routine and implementing time blocking")
                .build();
    }

    @Nested
    @DisplayName("Daily Check-in Management")
    class DailyCheckinTests {

        @Test
        @DisplayName("POST /api/v1/buddy/checkins/daily - Create daily check-in")
        void createDailyCheckin_Success() throws Exception {
            when(buddyCheckinService.createDailyCheckin(testUserId, testCheckinRequest))
                    .thenReturn(testCheckinResponse);

            mockMvc.perform(post("/api/v1/buddy/checkins/daily")
                    .with(csrf())
                    .header("X-User-ID", testUserId.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(testCheckinRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(testCheckinId.toString()))
                    .andExpect(jsonPath("$.data.userId").value(testUserId.toString()))
                    .andExpect(jsonPath("$.data.partnershipId").value(testPartnershipId.toString()))
                    .andExpect(jsonPath("$.data.checkinType").value("DAILY"))
                    .andExpect(jsonPath("$.data.mood").value("MOTIVATED"))
                    .andExpect(jsonPath("$.data.productivityRating").value(7))
                    .andExpect(jsonPath("$.data.content").value("Good productive day overall"))
                    .andExpect(jsonPath("$.message").value("Daily check-in created successfully"));

            verify(buddyCheckinService).createDailyCheckin(testUserId, testCheckinRequest);
        }

        @Test
        @DisplayName("POST /api/v1/buddy/checkins/daily - Duplicate check-in prevention")
        void createDailyCheckin_DuplicatePrevention() throws Exception {
            when(buddyCheckinService.createDailyCheckin(testUserId, testCheckinRequest))
                    .thenThrow(new BuddyServiceException("Daily check-in already exists for today"));

            mockMvc.perform(post("/api/v1/buddy/checkins/daily")
                    .with(csrf())
                    .header("X-User-ID", testUserId.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(testCheckinRequest)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Daily check-in already exists for today"));
        }

        @Test
        @DisplayName("GET /api/v1/buddy/checkins/{id} - Get check-in details")
        void getCheckinDetails_Success() throws Exception {
            when(buddyCheckinService.getDailyCheckin(testUserId, testCheckinId))
                    .thenReturn(testCheckinResponse);

            mockMvc.perform(get("/api/v1/buddy/checkins/{id}", testCheckinId)
                    .header("X-User-ID", testUserId.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(testCheckinId.toString()))
                    .andExpect(jsonPath("$.data.mood").value("MOTIVATED"))
                    .andExpect(jsonPath("$.data.productivityRating").value(7));

            verify(buddyCheckinService).getDailyCheckin(testUserId, testCheckinId);
        }

        @Test
        @DisplayName("PUT /api/v1/buddy/checkins/{id} - Update check-in")
        void updateCheckin_Success() throws Exception {
            CheckinRequestDto updateRequest = testCheckinRequest.toBuilder()
                    .productivityRating(8)
                    .notes("Updated notes after evening reflection")
                    .build();

            CheckinResponseDto updatedResponse = testCheckinResponse.toBuilder()
                    .productivityRating(8)
                    .summary("Updated notes after evening reflection")
                    .build();

            when(buddyCheckinService.updateDailyCheckin(testUserId, testCheckinId, updateRequest))
                    .thenReturn(updatedResponse);

            mockMvc.perform(put("/api/v1/buddy/checkins/{id}", testCheckinId)
                    .with(csrf())
                    .header("X-User-ID", testUserId.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(updateRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.productivityRating").value(8))
                    .andExpect(jsonPath("$.data.summary").value("Updated notes after evening reflection"))
                    .andExpect(jsonPath("$.message").value("Check-in updated successfully"));

            verify(buddyCheckinService).updateDailyCheckin(testUserId, testCheckinId, updateRequest);
        }

        @Test
        @DisplayName("DELETE /api/v1/buddy/checkins/{id} - Delete check-in")
        void deleteCheckin_Success() throws Exception {
            doNothing().when(buddyCheckinService).deleteCheckin(testUserId, testCheckinId);

            mockMvc.perform(delete("/api/v1/buddy/checkins/{id}", testCheckinId)
                    .with(csrf())
                    .header("X-User-ID", testUserId.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Check-in deleted successfully"));

            verify(buddyCheckinService).deleteCheckin(testUserId, testCheckinId);
        }

        @Test
        @DisplayName("GET /api/v1/buddy/checkins - Get check-in history")
        void getCheckinHistory_Success() throws Exception {
            LocalDate startDate = LocalDate.now().minusDays(7);
            LocalDate endDate = LocalDate.now();
            List<CheckinResponseDto> history = Arrays.asList(testCheckinResponse);

            when(buddyCheckinService.getCheckinHistory(testUserId, testPartnershipId, startDate, endDate))
                    .thenReturn(history);

            mockMvc.perform(get("/api/v1/buddy/checkins")
                    .header("X-User-ID", testUserId.toString())
                    .param("partnershipId", testPartnershipId.toString())
                    .param("startDate", startDate.toString())
                    .param("endDate", endDate.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.checkins").isArray())
                    .andExpect(jsonPath("$.data.checkins", hasSize(1)))
                    .andExpect(jsonPath("$.data.totalCount").value(1))
                    .andExpect(jsonPath("$.data.period.startDate").value(startDate.toString()))
                    .andExpect(jsonPath("$.data.period.endDate").value(endDate.toString()));

            verify(buddyCheckinService).getCheckinHistory(testUserId, testPartnershipId, startDate, endDate);
        }
    }

    @Nested
    @DisplayName("Weekly Review Management")
    class WeeklyReviewTests {

        @Test
        @DisplayName("POST /api/v1/buddy/checkins/weekly - Create weekly review")
        void createWeeklyReview_Success() throws Exception {
            when(buddyCheckinService.createWeeklyReview(testUserId, testPartnershipId, testWeeklyReview))
                    .thenReturn(testWeeklyReview);

            mockMvc.perform(post("/api/v1/buddy/checkins/weekly")
                    .with(csrf())
                    .header("X-User-ID", testUserId.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(testWeeklyReview)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.weekStartDate").exists())
                    .andExpect(jsonPath("$.data.weekEndDate").exists())
                    .andExpect(jsonPath("$.data.checkinsThisWeek").value(6))
                    .andExpect(jsonPath("$.data.averageProductivity").value(7.5))
                    .andExpect(jsonPath("$.data.accomplishments").exists())
                    .andExpect(jsonPath("$.data.nextWeekGoals").exists())
                    .andExpect(jsonPath("$.message").value("Weekly review created successfully"));

            verify(buddyCheckinService).createWeeklyReview(testUserId, testPartnershipId, testWeeklyReview);
        }

        @Test
        @DisplayName("GET /api/v1/buddy/checkins/weekly/aggregate - Get weekly data aggregation")
        void getWeeklyAggregation_Success() throws Exception {
            LocalDate weekStart = LocalDate.now().minusDays(6);

            when(buddyCheckinService.aggregateWeeklyData(testUserId, testPartnershipId, weekStart))
                    .thenReturn(testWeeklyReview);

            mockMvc.perform(get("/api/v1/buddy/checkins/weekly/aggregate")
                    .header("X-User-ID", testUserId.toString())
                    .param("partnershipId", testPartnershipId.toString())
                    .param("weekStart", weekStart.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.weekStartDate").value(weekStart.toString()))
                    .andExpect(jsonPath("$.data.checkinsThisWeek").value(6))
                    .andExpect(jsonPath("$.data.averageProductivity").value(7.5));

            verify(buddyCheckinService).aggregateWeeklyData(testUserId, testPartnershipId, weekStart);
        }

        @Test
        @DisplayName("GET /api/v1/buddy/checkins/weekly/progress - Calculate weekly progress")
        void calculateWeeklyProgress_Success() throws Exception {
            LocalDate weekStart = LocalDate.now().minusDays(6);
            ProductivityMetricsDto metrics = ProductivityMetricsDto.builder()
                    .date(LocalDate.now())
                    .currentRating(7)
                    .averageRating(7.2)
                    .focusHours(6)
                    .totalHours(8)
                    .productivityPercentage(75.0)
                    .productivityLevel("HIGH")
                    .build();

            when(buddyCheckinService.calculateWeeklyProgress(testUserId, testPartnershipId, weekStart))
                    .thenReturn(metrics);

            mockMvc.perform(get("/api/v1/buddy/checkins/weekly/progress")
                    .header("X-User-ID", testUserId.toString())
                    .param("partnershipId", testPartnershipId.toString())
                    .param("weekStart", weekStart.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.currentRating").value(7))
                    .andExpect(jsonPath("$.data.focusHours").value(6))
                    .andExpect(jsonPath("$.data.productivityPercentage").value(75.0));

            verify(buddyCheckinService).calculateWeeklyProgress(testUserId, testPartnershipId, weekStart);
        }

        @Test
        @DisplayName("GET /api/v1/buddy/checkins/weekly/insights - Get weekly insights")
        void getWeeklyInsights_Success() throws Exception {
            LocalDate weekStart = LocalDate.now().minusDays(6);
            String insights = "Your productivity improved by 15% this week. Consider maintaining the current routine.";

            when(buddyCheckinService.generateWeeklyInsights(testUserId, testPartnershipId, weekStart))
                    .thenReturn(insights);

            mockMvc.perform(get("/api/v1/buddy/checkins/weekly/insights")
                    .header("X-User-ID", testUserId.toString())
                    .param("partnershipId", testPartnershipId.toString())
                    .param("weekStart", weekStart.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.insights").value(insights))
                    .andExpect(jsonPath("$.data.weekStart").value(weekStart.toString()));

            verify(buddyCheckinService).generateWeeklyInsights(testUserId, testPartnershipId, weekStart);
        }
    }

    @Nested
    @DisplayName("Streak Management")
    class StreakTests {

        @Test
        @DisplayName("GET /api/v1/buddy/checkins/streaks - Get streak statistics")
        void getStreakStatistics_Success() throws Exception {
            when(buddyCheckinService.getStreakStatistics(testUserId, testPartnershipId))
                    .thenReturn(testStreakStats);

            mockMvc.perform(get("/api/v1/buddy/checkins/streaks")
                    .header("X-User-ID", testUserId.toString())
                    .param("partnershipId", testPartnershipId.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.currentDailyStreak").value(15))
                    .andExpect(jsonPath("$.data.longestDailyStreak").value(23))
                    .andExpect(jsonPath("$.data.currentWeeklyStreak").value(3))
                    .andExpect(jsonPath("$.data.longestWeeklyStreak").value(8))
                    .andExpect(jsonPath("$.data.isOnStreak").value(true))
                    .andExpect(jsonPath("$.data.daysUntilStreakBreak").value(1));

            verify(buddyCheckinService).getStreakStatistics(testUserId, testPartnershipId);
        }

        @Test
        @DisplayName("GET /api/v1/buddy/checkins/streaks/daily - Calculate daily streak")
        void calculateDailyStreak_Success() throws Exception {
            when(buddyCheckinService.calculateDailyStreak(testUserId, testPartnershipId))
                    .thenReturn(testStreakStats);

            mockMvc.perform(get("/api/v1/buddy/checkins/streaks/daily")
                    .header("X-User-ID", testUserId.toString())
                    .param("partnershipId", testPartnershipId.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.currentDailyStreak").value(15))
                    .andExpect(jsonPath("$.data.isOnStreak").value(true));

            verify(buddyCheckinService).calculateDailyStreak(testUserId, testPartnershipId);
        }

        @Test
        @DisplayName("GET /api/v1/buddy/checkins/streaks/weekly - Calculate weekly streak")
        void calculateWeeklyStreak_Success() throws Exception {
            StreakStatisticsDto weeklyStreak = testStreakStats.toBuilder()
                    .currentDailyStreak(4)
                    .currentWeeklyStreak(2)
                    .build();

            when(buddyCheckinService.calculateWeeklyStreak(testUserId, testPartnershipId))
                    .thenReturn(weeklyStreak);

            mockMvc.perform(get("/api/v1/buddy/checkins/streaks/weekly")
                    .header("X-User-ID", testUserId.toString())
                    .param("partnershipId", testPartnershipId.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.currentDailyStreak").value(4))
                    .andExpect(jsonPath("$.data.currentWeeklyStreak").value(2));

            verify(buddyCheckinService).calculateWeeklyStreak(testUserId, testPartnershipId);
        }

        @Test
        @DisplayName("GET /api/v1/buddy/checkins/streaks/longest - Get longest streak")
        void getLongestStreak_Success() throws Exception {
            when(buddyCheckinService.calculateLongestStreak(testUserId, testPartnershipId))
                    .thenReturn(testStreakStats);

            mockMvc.perform(get("/api/v1/buddy/checkins/streaks/longest")
                    .header("X-User-ID", testUserId.toString())
                    .param("partnershipId", testPartnershipId.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.longestDailyStreak").value(23));

            verify(buddyCheckinService).calculateLongestStreak(testUserId, testPartnershipId);
        }

        @Test
        @DisplayName("POST /api/v1/buddy/checkins/streaks/recover - Recover broken streak")
        void recoverStreak_Success() throws Exception {
            StreakRecoveryRequest recoveryRequest = new StreakRecoveryRequest();
            recoveryRequest.setPartnershipId(testPartnershipId);
            recoveryRequest.setRecoverDate(LocalDate.now().minusDays(1));
            recoveryRequest.setReason("Technical issue prevented check-in");

            when(buddyCheckinService.recoverStreak(testUserId, testPartnershipId,
                    recoveryRequest.getRecoverDate(), recoveryRequest.getReason()))
                    .thenReturn(true);

            mockMvc.perform(post("/api/v1/buddy/checkins/streaks/recover")
                    .with(csrf())
                    .header("X-User-ID", testUserId.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(recoveryRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.recovered").value(true))
                    .andExpect(jsonPath("$.message").value("Streak recovered successfully"));

            verify(buddyCheckinService).recoverStreak(testUserId, testPartnershipId,
                    recoveryRequest.getRecoverDate(), recoveryRequest.getReason());
        }
    }

    @Nested
    @DisplayName("Accountability Scoring")
    class AccountabilityTests {

        @Test
        @DisplayName("GET /api/v1/buddy/checkins/accountability - Get accountability score")
        void getAccountabilityScore_Success() throws Exception {
            when(buddyCheckinService.calculateAccountabilityScore(testUserId, testPartnershipId))
                    .thenReturn(testAccountabilityScore);

            mockMvc.perform(get("/api/v1/buddy/checkins/accountability")
                    .header("X-User-ID", testUserId.toString())
                    .param("partnershipId", testPartnershipId.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.score").value(8.5))
                    .andExpect(jsonPath("$.data.level").value("EXCELLENT"))
                    .andExpect(jsonPath("$.data.percentage").value("85%"))
                    .andExpect(jsonPath("$.data.checkinsCompleted").value(45))
                    .andExpect(jsonPath("$.data.goalsAchieved").value(12))
                    .andExpect(jsonPath("$.data.responseRate").value(0.9));

            verify(buddyCheckinService).calculateAccountabilityScore(testUserId, testPartnershipId);
        }

        @Test
        @DisplayName("GET /api/v1/buddy/checkins/accountability/history - Get score history")
        void getAccountabilityHistory_Success() throws Exception {
            LocalDate startDate = LocalDate.now().minusDays(30);
            LocalDate endDate = LocalDate.now();
            List<AccountabilityScoreDto> history = Arrays.asList(testAccountabilityScore);

            when(buddyCheckinService.getScoreHistory(testUserId, testPartnershipId, startDate, endDate))
                    .thenReturn(history);

            mockMvc.perform(get("/api/v1/buddy/checkins/accountability/history")
                    .header("X-User-ID", testUserId.toString())
                    .param("partnershipId", testPartnershipId.toString())
                    .param("startDate", startDate.toString())
                    .param("endDate", endDate.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.scores").isArray())
                    .andExpect(jsonPath("$.data.scores", hasSize(1)))
                    .andExpect(jsonPath("$.data.period.startDate").value(startDate.toString()))
                    .andExpect(jsonPath("$.data.period.endDate").value(endDate.toString()));

            verify(buddyCheckinService).getScoreHistory(testUserId, testPartnershipId, startDate, endDate);
        }

        @Test
        @DisplayName("GET /api/v1/buddy/checkins/accountability/compare - Compare with partner")
        void compareAccountabilityWithPartner_Success() throws Exception {
            Map<String, AccountabilityScoreDto> comparison = Map.of(
                    "user1", testAccountabilityScore,
                    "user2", testAccountabilityScore.toBuilder().score(BigDecimal.valueOf(7.8)).build()
            );

            when(buddyCheckinService.compareWithPartner(testPartnershipId))
                    .thenReturn(comparison);

            mockMvc.perform(get("/api/v1/buddy/checkins/accountability/compare")
                    .header("X-User-ID", testUserId.toString())
                    .param("partnershipId", testPartnershipId.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.comparison").exists())
                    .andExpect(jsonPath("$.data.partnershipId").value(testPartnershipId.toString()));

            verify(buddyCheckinService).compareWithPartner(testPartnershipId);
        }

        @Test
        @DisplayName("GET /api/v1/buddy/checkins/accountability/suggestions - Get improvement suggestions")
        void getAccountabilityImprovementSuggestions_Success() throws Exception {
            List<String> suggestions = Arrays.asList(
                    "Maintain consistent daily check-ins",
                    "Improve response time to partner messages",
                    "Set more specific daily goals"
            );

            when(buddyCheckinService.calculateAccountabilityScore(testUserId, testPartnershipId))
                    .thenReturn(testAccountabilityScore);
            when(buddyCheckinService.suggestScoreImprovement(testUserId, testAccountabilityScore))
                    .thenReturn(suggestions);

            mockMvc.perform(get("/api/v1/buddy/checkins/accountability/suggestions")
                    .header("X-User-ID", testUserId.toString())
                    .param("partnershipId", testPartnershipId.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.suggestions").isArray())
                    .andExpect(jsonPath("$.data.suggestions", hasSize(3)))
                    .andExpect(jsonPath("$.data.suggestions[0]").value("Maintain consistent daily check-ins"));

            verify(buddyCheckinService).calculateAccountabilityScore(testUserId, testPartnershipId);
            verify(buddyCheckinService).suggestScoreImprovement(testUserId, testAccountabilityScore);
        }
    }

    @Nested
    @DisplayName("Analytics and Insights")
    class AnalyticsTests {

        @Test
        @DisplayName("GET /api/v1/buddy/checkins/analytics - Get comprehensive analytics")
        void getCheckinAnalytics_Success() throws Exception {
            LocalDate startDate = LocalDate.now().minusDays(30);
            LocalDate endDate = LocalDate.now();

            when(buddyCheckinService.generateCheckinAnalytics(testUserId, testPartnershipId, startDate, endDate))
                    .thenReturn(testAnalytics);

            mockMvc.perform(get("/api/v1/buddy/checkins/analytics")
                    .header("X-User-ID", testUserId.toString())
                    .param("partnershipId", testPartnershipId.toString())
                    .param("startDate", startDate.toString())
                    .param("endDate", endDate.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.totalCheckins").value(25))
                    .andExpect(jsonPath("$.data.dailyCheckins").value(15))
                    .andExpect(jsonPath("$.data.weeklyCheckins").value(10))
                    .andExpect(jsonPath("$.data.averageProductivity").value(7.2))
                    .andExpect(jsonPath("$.data.completionRate").value(0.83))
                    .andExpect(jsonPath("$.data.mostProductiveDay").value("Wednesday"));

            verify(buddyCheckinService).generateCheckinAnalytics(testUserId, testPartnershipId, startDate, endDate);
        }

        @Test
        @DisplayName("GET /api/v1/buddy/checkins/patterns - Identify behavior patterns")
        void identifyPatterns_Success() throws Exception {
            LocalDate startDate = LocalDate.now().minusDays(30);
            LocalDate endDate = LocalDate.now();
            Map<String, Object> patterns = Map.of(
                    "bestProductivityDay", "Tuesday",
                    "averageCheckinTime", "09:30",
                    "moodCorrelation", "Strong positive correlation with productivity",
                    "consistencyPattern", "More consistent in weekdays"
            );

            when(buddyCheckinService.identifyPatterns(testUserId, testPartnershipId, startDate, endDate))
                    .thenReturn(patterns);

            mockMvc.perform(get("/api/v1/buddy/checkins/patterns")
                    .header("X-User-ID", testUserId.toString())
                    .param("partnershipId", testPartnershipId.toString())
                    .param("startDate", startDate.toString())
                    .param("endDate", endDate.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.patterns").exists())
                    .andExpect(jsonPath("$.data.patterns.bestProductivityDay").value("Tuesday"))
                    .andExpect(jsonPath("$.data.patterns.averageCheckinTime").value("09:30"));

            verify(buddyCheckinService).identifyPatterns(testUserId, testPartnershipId, startDate, endDate);
        }

        @Test
        @DisplayName("GET /api/v1/buddy/checkins/predictions - Get predictive analytics")
        void getPredictiveAnalytics_Success() throws Exception {
            LocalDateTime nextCheckin = LocalDateTime.now().plusDays(1).withHour(9).withMinute(30);
            String optimalTime = "09:30";

            when(buddyCheckinService.predictNextCheckin(testUserId, testPartnershipId))
                    .thenReturn(nextCheckin);
            when(buddyCheckinService.suggestOptimalCheckinTime(testUserId, testPartnershipId))
                    .thenReturn(optimalTime);

            mockMvc.perform(get("/api/v1/buddy/checkins/predictions")
                    .header("X-User-ID", testUserId.toString())
                    .param("partnershipId", testPartnershipId.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.predictedNextCheckin").value(nextCheckin.toString()))
                    .andExpect(jsonPath("$.data.optimalCheckinTime").value(optimalTime));

            verify(buddyCheckinService).predictNextCheckin(testUserId, testPartnershipId);
            verify(buddyCheckinService).suggestOptimalCheckinTime(testUserId, testPartnershipId);
        }

        @Test
        @DisplayName("GET /api/v1/buddy/checkins/export - Export check-in data")
        void exportCheckinData_Success() throws Exception {
            LocalDate startDate = LocalDate.now().minusDays(30);
            LocalDate endDate = LocalDate.now();
            byte[] exportData = "CSV,Export,Data".getBytes();

            when(buddyCheckinService.exportCheckinData(testUserId, testPartnershipId, startDate, endDate))
                    .thenReturn(exportData);

            mockMvc.perform(get("/api/v1/buddy/checkins/export")
                    .header("X-User-ID", testUserId.toString())
                    .param("partnershipId", testPartnershipId.toString())
                    .param("startDate", startDate.toString())
                    .param("endDate", endDate.toString())
                    .param("format", "csv"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Content-Type", "application/octet-stream"))
                    .andExpect(header().string("Content-Disposition",
                            "attachment; filename=checkin-data-" + testUserId + ".csv"));

            verify(buddyCheckinService).exportCheckinData(testUserId, testPartnershipId, startDate, endDate);
        }
    }

    @Nested
    @DisplayName("Input Validation and Error Handling")
    class ValidationAndErrorHandlingTests {

        @Test
        @DisplayName("All endpoints - Missing user ID header")
        void allEndpoints_MissingUserIdHeader() throws Exception {
            mockMvc.perform(get("/api/v1/buddy/checkins"))
                    .andExpect(status().isBadRequest());

            mockMvc.perform(post("/api/v1/buddy/checkins/daily").with(csrf()))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Check-in creation - Invalid data")
        void createCheckin_InvalidData() throws Exception {
            CheckinRequestDto invalidRequest = testCheckinRequest.toBuilder()
                    .productivityRating(-1) // Invalid productivity rating
                    .productivityRating(11) // Out of range
                    .focusHours(-2) // Negative hours
                    .build();

            mockMvc.perform(post("/api/v1/buddy/checkins/daily")
                    .with(csrf())
                    .header("X-User-ID", testUserId.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors").isArray());
        }

        @Test
        @DisplayName("Check-in operations - Invalid UUID")
        void checkinOperations_InvalidUuid() throws Exception {
            mockMvc.perform(get("/api/v1/buddy/checkins/{id}", "invalid-uuid")
                    .header("X-User-ID", testUserId.toString()))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value(containsString("Invalid check-in ID format")));
        }

        @Test
        @DisplayName("Date range validation")
        void dateRange_Validation() throws Exception {
            LocalDate startDate = LocalDate.now();
            LocalDate endDate = LocalDate.now().minusDays(1); // End before start

            mockMvc.perform(get("/api/v1/buddy/checkins")
                    .header("X-User-ID", testUserId.toString())
                    .param("partnershipId", testPartnershipId.toString())
                    .param("startDate", startDate.toString())
                    .param("endDate", endDate.toString()))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("End date must be after start date"));
        }

        @Test
        @DisplayName("Service unavailable handling")
        void serviceUnavailable_Handling() throws Exception {
            when(buddyCheckinService.createDailyCheckin(any(UUID.class), any(CheckinRequestDto.class)))
                    .thenThrow(new RuntimeException("Database connection failed"));

            mockMvc.perform(post("/api/v1/buddy/checkins/daily")
                    .with(csrf())
                    .header("X-User-ID", testUserId.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(testCheckinRequest)))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Internal server error"));
        }

        @Test
        @DisplayName("Check-in time validation")
        void checkinTime_Validation() throws Exception {
            when(buddyCheckinService.validateCheckinTime(testUserId, CheckInType.DAILY))
                    .thenReturn(false);

            when(buddyCheckinService.createDailyCheckin(testUserId, testCheckinRequest))
                    .thenThrow(new BuddyServiceException("Check-in not allowed at this time"));

            mockMvc.perform(post("/api/v1/buddy/checkins/daily")
                    .with(csrf())
                    .header("X-User-ID", testUserId.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(testCheckinRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Check-in not allowed at this time"));
        }
    }

    // Helper request classes for test data
    private static class StreakRecoveryRequest {
        private UUID partnershipId;
        private LocalDate recoverDate;
        private String reason;

        public UUID getPartnershipId() { return partnershipId; }
        public void setPartnershipId(UUID partnershipId) { this.partnershipId = partnershipId; }
        public LocalDate getRecoverDate() { return recoverDate; }
        public void setRecoverDate(LocalDate recoverDate) { this.recoverDate = recoverDate; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }
}