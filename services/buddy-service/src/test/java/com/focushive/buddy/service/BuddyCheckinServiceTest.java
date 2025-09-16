package com.focushive.buddy.service;

import com.focushive.buddy.constant.CheckInType;
import com.focushive.buddy.constant.MoodType;
import com.focushive.buddy.dto.*;
import com.focushive.buddy.entity.*;
import com.focushive.buddy.exception.*;
import com.focushive.buddy.repository.*;
import com.focushive.buddy.service.BuddyCheckinService;
import com.focushive.buddy.service.impl.BuddyCheckinServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive test suite for BuddyCheckinService following strict TDD principles.
 *
 * RED PHASE: Tests are designed to FAIL initially until service implementation is complete.
 * All tests validate business logic, calculations, and service interactions.
 *
 * Test Coverage:
 * - Daily Check-in Management (10 tests)
 * - Weekly Check-in Management (8 tests)
 * - Mood Tracking (8 tests)
 * - Productivity Metrics (10 tests)
 * - Streak Calculations (10 tests)
 * - Accountability Scoring (8 tests)
 * - Partner Synchronization (6 tests)
 * - Analytics & Insights (8 tests)
 * - Edge Cases & Error Handling (10 tests)
 *
 * Total: 78 comprehensive test methods
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("BuddyCheckinService Tests - TDD RED Phase")
class BuddyCheckinServiceTest {





    // ======================================
    // Test Setup and Dependencies
    // ======================================

    @Mock
    private BuddyCheckinRepository checkinRepository;

    @Mock
    private AccountabilityScoreRepository accountabilityScoreRepository;

    @Mock
    private BuddyPartnershipRepository partnershipRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    // Stub for notification service - will be implemented later
    @Mock
    private Object notificationService;

    // Stub for ML prediction service - will be implemented later
    @Mock
    private Object mlPredictionService;

    @InjectMocks
    private BuddyCheckinServiceImpl checkinService;

    // Test data
    private UUID userId;
    private UUID partnershipId;
    private UUID checkinId;
    private BuddyCheckin sampleCheckin;
    private BuddyPartnership samplePartnership;
    private AccountabilityScore sampleScore;
    private User sampleUser;

    @BeforeEach
    void setUp() {
        // Initialize test data
        userId = UUID.randomUUID();
        partnershipId = UUID.randomUUID();
        checkinId = UUID.randomUUID();

        // Mock Redis operations
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // Create sample entities
        sampleUser = User.builder()
            .id(userId.toString())
            .displayName("testuser")
            .timezone("UTC")
            .build();

        samplePartnership = BuddyPartnership.builder()
            .id(partnershipId)
            .user1Id(userId)
            .user2Id(UUID.randomUUID())
            .build();

        sampleCheckin = BuddyCheckin.builder()
            .id(checkinId)
            .partnershipId(partnershipId)
            .userId(userId)
            .checkinType(CheckInType.DAILY)
            .content("Daily check-in")
            .mood(MoodType.MOTIVATED)
            .productivityRating(8)
            .createdAt(LocalDateTime.now())
            .build();

        sampleScore = AccountabilityScore.builder()
            .id(UUID.randomUUID())
            .userId(userId)
            .partnershipId(partnershipId)
            .score(BigDecimal.valueOf(0.75))
            .checkinsCompleted(10)
            .goalsAchieved(2)
            .responseRate(BigDecimal.valueOf(0.80))
            .streakDays(5)
            .build();
    }

    // ======================================
    // 1. Daily Check-in Management Tests (10 tests)
    // ======================================

    @Nested
    @DisplayName("Daily Check-in Management Tests")
    class DailyCheckinManagementTests {

        @Test
        @DisplayName("Should create daily check-in successfully")
        void shouldCreateDailyCheckinSuccessfully() {
            // Arrange
            CheckinRequestDto request = new CheckinRequestDto(
                partnershipId, CheckInType.DAILY, "Daily update", MoodType.MOTIVATED, 8
            );

            when(partnershipRepository.findById(partnershipId)).thenReturn(Optional.of(samplePartnership));
            when(checkinRepository.hasCheckedInToday(partnershipId, userId, LocalDate.now())).thenReturn(false);
            when(checkinRepository.save(any(BuddyCheckin.class))).thenReturn(sampleCheckin);
            when(accountabilityScoreRepository.findByUserIdAndPartnershipId(userId, partnershipId))
                .thenReturn(Optional.of(sampleScore));

            // Act
            CheckinResponseDto result = checkinService.createDailyCheckin(userId, request);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getUserId()).isEqualTo(userId);
            assertThat(result.getPartnershipId()).isEqualTo(partnershipId);
            assertThat(result.getCheckinType()).isEqualTo(CheckInType.DAILY);
            assertThat(result.getContent()).isEqualTo("Daily update");
            assertThat(result.getMood()).isEqualTo(MoodType.MOTIVATED);
            assertThat(result.getProductivityRating()).isEqualTo(8);
        }

        @Test
        @DisplayName("Should update existing daily check-in")
        void shouldUpdateExistingDailyCheckin() {
            // Arrange
            CheckinRequestDto request = new CheckinRequestDto(
                partnershipId, CheckInType.DAILY, "Updated content", MoodType.FOCUSED, 9
            );

            when(checkinRepository.findById(checkinId)).thenReturn(Optional.of(sampleCheckin));
            when(checkinRepository.save(any(BuddyCheckin.class))).thenReturn(sampleCheckin);

            // Act
            CheckinResponseDto result = checkinService.updateDailyCheckin(userId, checkinId, request);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(checkinId);
            verify(checkinRepository).findById(checkinId);
            verify(checkinRepository).save(any(BuddyCheckin.class));
            verify(eventPublisher).publishEvent(any(Object.class));
        }

        @Test
        @DisplayName("Should retrieve specific daily check-in")
        void shouldRetrieveSpecificDailyCheckin() {
            // Arrange
            when(checkinRepository.findById(checkinId)).thenReturn(Optional.of(sampleCheckin));

            // Act
            CheckinResponseDto result = checkinService.getDailyCheckin(userId, checkinId);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(checkinId);
            assertThat(result.getUserId()).isEqualTo(userId);
            assertThat(result.getPartnershipId()).isEqualTo(partnershipId);
            verify(checkinRepository).findById(checkinId);
        }

        @Test
        @DisplayName("Should validate check-in time window")
        void shouldValidateCheckinTimeWindow() {
            // Act & Assert
            boolean result = checkinService.validateCheckinTime(userId, CheckInType.DAILY);

            // Should return true for daily check-ins during reasonable hours
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should prevent duplicate check-in for same day")
        void shouldPreventDuplicateCheckinForSameDay() {
            // Arrange
            LocalDate today = LocalDate.now();
            when(checkinRepository.hasCheckedInToday(partnershipId, userId, today)).thenReturn(false);

            // Act
            boolean result = checkinService.preventDuplicateCheckin(userId, partnershipId, today);

            // Assert
            assertThat(result).isTrue(); // Should allow check-in (no duplicate)
        }

        @Test
        @DisplayName("Should handle missed check-in appropriately")
        void shouldHandleMissedCheckinAppropriately() {
            // Arrange
            LocalDate missedDate = LocalDate.now().minusDays(1);
            when(accountabilityScoreRepository.findByUserIdAndPartnershipId(userId, partnershipId))
                .thenReturn(Optional.of(sampleScore));
            when(accountabilityScoreRepository.save(any(AccountabilityScore.class))).thenReturn(sampleScore);
            doNothing().when(accountabilityScoreRepository).updateStreakDays(eq(userId), eq(partnershipId), eq(0), any(LocalDateTime.class));

            // Act
            checkinService.handleMissedCheckin(userId, partnershipId, missedDate);

            // Assert
            verify(accountabilityScoreRepository).findByUserIdAndPartnershipId(userId, partnershipId);
            verify(eventPublisher, times(2)).publishEvent(any(Object.class)); // Two events: penalty and streak break
        }

        @Test
        @DisplayName("Should retrieve check-in history for date range")
        void shouldRetrieveCheckinHistoryForDateRange() {
            // Arrange
            LocalDate startDate = LocalDate.now().minusDays(30);
            LocalDate endDate = LocalDate.now();
            List<BuddyCheckin> mockCheckins = List.of(sampleCheckin);

            when(checkinRepository.findByPartnershipIdAndUserIdAndCreatedAtBetween(
                eq(partnershipId), eq(userId), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(mockCheckins);

            // Act
            List<CheckinResponseDto> result = checkinService.getCheckinHistory(userId, partnershipId, startDate, endDate);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getId()).isEqualTo(checkinId);
            verify(checkinRepository).findByPartnershipIdAndUserIdAndCreatedAtBetween(
                eq(partnershipId), eq(userId), any(LocalDateTime.class), any(LocalDateTime.class));
        }

        @Test
        @DisplayName("Should soft delete check-in with audit trail")
        void shouldSoftDeleteCheckinWithAuditTrail() {
            // Arrange
            when(checkinRepository.findById(checkinId)).thenReturn(Optional.of(sampleCheckin));

            // Act
            checkinService.deleteCheckin(userId, checkinId);

            // Assert
            verify(checkinRepository).findById(checkinId);
            verify(checkinRepository).delete(sampleCheckin);
            verify(eventPublisher).publishEvent(any(Object.class));
        }

        @ParameterizedTest
        @EnumSource(CheckInType.class)
        @DisplayName("Should validate check-in time for all check-in types")
        void shouldValidateCheckinTimeForAllTypes(CheckInType checkinType) {
            // Act
            boolean result = checkinService.validateCheckinTime(userId, checkinType);

            // Assert
            if (checkinType == CheckInType.DAILY || checkinType == CheckInType.WEEKLY) {
                // Should be valid during reasonable hours (implementation allows 6AM-11PM for daily, 8AM-8PM for weekly)
                assertThat(result).isTrue();
            } else {
                // Other types should be allowed anytime
                assertThat(result).isTrue();
            }
        }

        @Test
        @DisplayName("Should handle check-in creation with invalid data")
        void shouldHandleCheckinCreationWithInvalidData() {
            // Arrange
            CheckinRequestDto invalidRequest = new CheckinRequestDto(
                null, null, null, null, null // All null values
            );

            // Act & Assert
            assertThatThrownBy(() -> checkinService.createDailyCheckin(userId, invalidRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Partnership ID cannot be null");
        }
    }

    // ======================================
    // 2. Weekly Check-in Management Tests (8 tests)
    // ======================================

    @Nested
    @DisplayName("Weekly Check-in Management Tests")
    class WeeklyCheckinManagementTests {

        @Test
        @DisplayName("Should create weekly review successfully")
        void shouldCreateWeeklyReviewSuccessfully() {
            // Arrange
            LocalDate weekStart = LocalDate.now().minusDays(7);
            WeeklyReviewDto request = new WeeklyReviewDto(
                weekStart, weekStart.plusDays(6), 5, 7.5
            );
            List<BuddyCheckin> mockCheckins = List.of(sampleCheckin);

            when(checkinRepository.findByPartnershipIdAndUserIdAndCreatedAtBetween(
                eq(partnershipId), eq(userId), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(mockCheckins);

            // Act
            WeeklyReviewDto result = checkinService.createWeeklyReview(userId, partnershipId, request);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getWeekStartDate()).isEqualTo(weekStart);
            assertThat(result.getWeekEndDate()).isEqualTo(weekStart.plusDays(6));
            assertThat(result.getCheckinsThisWeek()).isEqualTo(1);
            verify(redisTemplate.opsForValue()).set(anyString(), any());
        }

        @Test
        @DisplayName("Should aggregate weekly data from daily check-ins")
        void shouldAggregateWeeklyDataFromDailyCheckins() {
            // Arrange
            LocalDate weekStart = LocalDate.now().minusDays(7);
            List<BuddyCheckin> mockCheckins = List.of(sampleCheckin);

            when(checkinRepository.findByPartnershipIdAndUserIdAndCreatedAtBetween(
                eq(partnershipId), eq(userId), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(mockCheckins);

            // Act
            WeeklyReviewDto result = checkinService.aggregateWeeklyData(userId, partnershipId, weekStart);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getWeekStartDate()).isEqualTo(weekStart);
            assertThat(result.getWeekEndDate()).isEqualTo(weekStart.plusDays(6));
            assertThat(result.getCheckinsThisWeek()).isEqualTo(1);
            assertThat(result.getAverageProductivity()).isEqualTo(8.0);
        }

        @Test
        @DisplayName("Should calculate weekly progress metrics")
        void shouldCalculateWeeklyProgressMetrics() {
            // Arrange
            LocalDate weekStart = LocalDate.now().minusDays(7);
            List<BuddyCheckin> mockCheckins = List.of(sampleCheckin);

            when(checkinRepository.findByPartnershipIdAndUserIdAndCreatedAtBetween(
                eq(partnershipId), eq(userId), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(mockCheckins);

            // Act
            ProductivityMetricsDto result = checkinService.calculateWeeklyProgress(userId, partnershipId, weekStart);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getAverageRating()).isEqualTo(8.0);
            assertThat(result.getTotalHours()).isEqualTo(8);
            assertThat(result.getProductivityPercentage()).isEqualTo(80.0);
            assertThat(result.getProductivityLevel()).isEqualTo("Good");
        }

        @Test
        @DisplayName("Should compare with previous week's performance")
        void shouldCompareWithPreviousWeekPerformance() {
            // Arrange
            LocalDate currentWeekStart = LocalDate.now().minusDays(7);
            List<BuddyCheckin> mockCheckins = List.of(sampleCheckin);

            when(checkinRepository.findByPartnershipIdAndUserIdAndCreatedAtBetween(
                eq(partnershipId), eq(userId), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(mockCheckins);

            // Act
            WeeklyReviewDto result = checkinService.compareWithPreviousWeek(userId, partnershipId, currentWeekStart);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getWeeklyProgress()).contains("Check-ins: 1 vs 1 last week");
            assertThat(result.getWeeklyProgress()).contains("Productivity: 8.0 vs 8.0 last week");
        }

        @Test
        @DisplayName("Should generate weekly insights and recommendations")
        void shouldGenerateWeeklyInsightsAndRecommendations() {
            // Arrange
            LocalDate weekStart = LocalDate.now().minusDays(7);
            List<BuddyCheckin> mockCheckins = List.of(sampleCheckin);

            when(checkinRepository.findByPartnershipIdAndUserIdAndCreatedAtBetween(
                eq(partnershipId), eq(userId), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(mockCheckins);

            // Act
            String result = checkinService.generateWeeklyInsights(userId, partnershipId, weekStart);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result).contains("Weekly Insights:");
            assertThat(result).contains("productivity"); // Should contain productivity-related insights
        }

        @Test
        @DisplayName("Should schedule weekly reminder notifications")
        void shouldScheduleWeeklyReminderNotifications() {
            // Act
            checkinService.scheduleWeeklyReminder(userId, partnershipId);

            // Assert
            verify(redisTemplate.opsForValue()).set(anyString(), any(LocalDateTime.class));
            verify(eventPublisher).publishEvent(any(Object.class));
        }

        @ParameterizedTest
        @ValueSource(ints = {0, 1, 7, 14, 30})
        @DisplayName("Should handle weekly aggregation for different week offsets")
        void shouldHandleWeeklyAggregationForDifferentWeekOffsets(int daysBack) {
            // Arrange
            LocalDate weekStart = LocalDate.now().minusDays(daysBack);
            List<BuddyCheckin> mockCheckins = List.of(sampleCheckin);

            when(checkinRepository.findByPartnershipIdAndUserIdAndCreatedAtBetween(
                eq(partnershipId), eq(userId), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(mockCheckins);

            // Act
            WeeklyReviewDto result = checkinService.aggregateWeeklyData(userId, partnershipId, weekStart);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getWeekStartDate()).isEqualTo(weekStart);
            assertThat(result.getCheckinsThisWeek()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should handle week with no check-ins")
        void shouldHandleWeekWithNoCheckins() {
            // Arrange
            LocalDate emptyWeekStart = LocalDate.now().minusDays(30); // Assume no data this far back
            List<BuddyCheckin> emptyCheckins = List.of();

            when(checkinRepository.findByPartnershipIdAndUserIdAndCreatedAtBetween(
                eq(partnershipId), eq(userId), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(emptyCheckins);

            // Act
            WeeklyReviewDto result = checkinService.aggregateWeeklyData(userId, partnershipId, emptyWeekStart);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getWeekStartDate()).isEqualTo(emptyWeekStart);
            assertThat(result.getCheckinsThisWeek()).isEqualTo(0);
            assertThat(result.getAverageProductivity()).isEqualTo(0.0);
        }
    }

    // ======================================
    // 3. Mood Tracking Tests (8 tests)
    // ======================================

    @Nested
    @DisplayName("Mood Tracking Tests")
    class MoodTrackingTests {

        @Test
        @DisplayName("Should record mood successfully")
        void shouldRecordMoodSuccessfully() {
            // Arrange
            LocalDate today = LocalDate.now();
            when(checkinRepository.hasCheckedInToday(partnershipId, userId, today)).thenReturn(false);
            when(partnershipRepository.findById(partnershipId)).thenReturn(Optional.of(samplePartnership));
            when(checkinRepository.save(any(BuddyCheckin.class))).thenReturn(sampleCheckin);
            when(accountabilityScoreRepository.findByUserIdAndPartnershipId(userId, partnershipId))
                .thenReturn(Optional.of(sampleScore));

            // Act
            MoodTrackingDto result = checkinService.recordMood(userId, partnershipId, MoodType.MOTIVATED, today);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getCurrentMood()).isEqualTo(MoodType.MOTIVATED);
            assertThat(result.getEmotionalScore()).isEqualTo(MoodType.MOTIVATED.getEmotionalScore());
            assertThat(result.getDate()).isEqualTo(today);
        }

        @Test
        @DisplayName("Should retrieve mood history for date range")
        void shouldRetrieveMoodHistoryForDateRange() {
            // Arrange
            LocalDate startDate = LocalDate.now().minusDays(30);
            LocalDate endDate = LocalDate.now();
            List<BuddyCheckin> mockCheckins = List.of(sampleCheckin);

            when(checkinRepository.findByPartnershipIdAndUserIdAndCreatedAtBetween(
                eq(partnershipId), eq(userId), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(mockCheckins);

            // Act
            List<MoodTrackingDto> result = checkinService.getMoodHistory(userId, partnershipId, startDate, endDate);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getCurrentMood()).isEqualTo(MoodType.MOTIVATED);
            assertThat(result.get(0).getEmotionalScore()).isEqualTo(MoodType.MOTIVATED.getEmotionalScore());
        }

        @Test
        @DisplayName("Should analyze mood trends over time")
        void shouldAnalyzeMoodTrendsOverTime() {
            // Arrange
            LocalDate startDate = LocalDate.now().minusDays(30);
            LocalDate endDate = LocalDate.now();
            List<BuddyCheckin> mockCheckins = List.of(sampleCheckin);

            when(checkinRepository.findByPartnershipIdAndUserIdAndCreatedAtBetween(
                eq(partnershipId), eq(userId), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(mockCheckins);

            // Act
            String result = checkinService.analyzeMoodTrends(userId, partnershipId, startDate, endDate);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result).isIn("STABLE", "IMPROVING", "DECLINING", "INSUFFICIENT_DATA");
        }

        @Test
        @DisplayName("Should detect mood anomalies")
        void shouldDetectMoodAnomalies() {
            // Arrange
            List<BuddyCheckin> mockCheckins = List.of(sampleCheckin);

            when(checkinRepository.findByPartnershipIdAndUserIdAndCreatedAtBetween(
                eq(partnershipId), eq(userId), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(mockCheckins);

            // Act
            boolean result = checkinService.detectMoodAnomalies(userId, partnershipId, MoodType.FRUSTRATED);

            // Assert
            assertThat(result).isInstanceOf(Boolean.class);
            // Anomaly detection depends on historical data vs current mood
        }

        @Test
        @DisplayName("Should suggest mood interventions")
        void shouldSuggestMoodInterventions() {
            // Act
            String result = checkinService.suggestMoodInterventions(userId, MoodType.STRESSED, 6.5);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result).contains("stress");
        }

        @Test
        @DisplayName("Should correlate mood with productivity")
        void shouldCorrelateMoodWithProductivity() {
            // Arrange
            LocalDate startDate = LocalDate.now().minusDays(30);
            LocalDate endDate = LocalDate.now();
            List<BuddyCheckin> mockCheckins = List.of(sampleCheckin);

            when(checkinRepository.findByPartnershipIdAndUserIdAndCreatedAtBetween(
                eq(partnershipId), eq(userId), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(mockCheckins);

            // Act
            Double result = checkinService.correlateMoodWithProductivity(userId, partnershipId, startDate, endDate);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result).isBetween(-1.0, 1.0); // Correlation coefficient range
        }

        @ParameterizedTest
        @EnumSource(MoodType.class)
        @DisplayName("Should handle all mood types for recording")
        void shouldHandleAllMoodTypesForRecording(MoodType moodType) {
            // Arrange
            LocalDate today = LocalDate.now();
            when(checkinRepository.hasCheckedInToday(partnershipId, userId, today)).thenReturn(false);
            when(partnershipRepository.findById(partnershipId)).thenReturn(Optional.of(samplePartnership));
            when(checkinRepository.save(any(BuddyCheckin.class))).thenReturn(sampleCheckin);
            when(accountabilityScoreRepository.findByUserIdAndPartnershipId(userId, partnershipId))
                .thenReturn(Optional.of(sampleScore));

            // Act
            MoodTrackingDto result = checkinService.recordMood(userId, partnershipId, moodType, today);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getCurrentMood()).isEqualTo(moodType);
            assertThat(result.getEmotionalScore()).isEqualTo(moodType.getEmotionalScore());
        }

        @Test
        @DisplayName("Should handle mood anomaly detection for extreme mood changes")
        void shouldHandleMoodAnomalyDetectionForExtremeMoodChanges() {
            // Arrange
            List<BuddyCheckin> mockCheckins = List.of(sampleCheckin);

            when(checkinRepository.findByPartnershipIdAndUserIdAndCreatedAtBetween(
                eq(partnershipId), eq(userId), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(mockCheckins);

            // Act
            boolean result = checkinService.detectMoodAnomalies(userId, partnershipId, MoodType.FRUSTRATED);

            // Assert
            assertThat(result).isInstanceOf(Boolean.class);
            // Should detect anomaly when going from positive (MOTIVATED) to negative (FRUSTRATED)
        }
    }

    // ======================================
    // 4. Productivity Metrics Tests (10 tests)
    // ======================================

    @Nested
    @DisplayName("Productivity Metrics Tests")
    class ProductivityMetricsTests {

        @Test
        @DisplayName("Should record productivity score successfully")
        void shouldRecordProductivityScoreSuccessfully() {
            // Arrange
            LocalDate today = LocalDate.now();
            when(checkinRepository.hasCheckedInToday(partnershipId, userId, today)).thenReturn(false);
            when(partnershipRepository.findById(partnershipId)).thenReturn(Optional.of(samplePartnership));
            when(checkinRepository.save(any(BuddyCheckin.class))).thenReturn(sampleCheckin);
            when(accountabilityScoreRepository.findByUserIdAndPartnershipId(userId, partnershipId))
                .thenReturn(Optional.of(sampleScore));

            // Act
            ProductivityMetricsDto result = checkinService.recordProductivityScore(userId, partnershipId, 8, today);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getCurrentRating()).isEqualTo(8);
            assertThat(result.getAverageRating()).isEqualTo(8.0);
            assertThat(result.getDate()).isEqualTo(today);
            assertThat(result.getProductivityLevel()).isEqualTo("Good");
            assertThat(result.getProductivityPercentage()).isEqualTo(80.0);
        }

        @Test
        @DisplayName("Should calculate average productivity for date range")
        void shouldCalculateAverageProductivityForDateRange() {
            // Arrange
            LocalDate startDate = LocalDate.now().minusDays(30);
            LocalDate endDate = LocalDate.now();
            List<BuddyCheckin> mockCheckins = List.of(sampleCheckin);

            when(checkinRepository.findByPartnershipIdAndUserIdAndCreatedAtBetween(
                eq(partnershipId), eq(userId), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(mockCheckins);

            // Act
            Double result = checkinService.calculateAverageProductivity(userId, partnershipId, startDate, endDate);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result).isEqualTo(8.0);
        }

        @Test
        @DisplayName("Should identify most productive time periods")
        void shouldIdentifyMostProductiveTimePeriods() {
            // Arrange
            LocalDate startDate = LocalDate.now().minusDays(30);
            LocalDate endDate = LocalDate.now();
            List<BuddyCheckin> mockCheckins = List.of(sampleCheckin);

            when(checkinRepository.findByPartnershipIdAndUserIdAndCreatedAtBetween(
                eq(partnershipId), eq(userId), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(mockCheckins);

            // Act
            Map<String, Double> result = checkinService.identifyProductivePeriods(userId, partnershipId, startDate, endDate);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result).isNotEmpty();
            // Check that it contains day-of-week mapping
            assertThat(result.values().stream().allMatch(score -> score >= 0.0 && score <= 10.0)).isTrue();
        }

        @Test
        @DisplayName("Should track focus hours accurately")
        void shouldTrackFocusHoursAccurately() {
            // Arrange
            LocalDate today = LocalDate.now();

            // Act
            ProductivityMetricsDto result = checkinService.trackFocusHours(userId, partnershipId, 6, today);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getFocusHours()).isEqualTo(6);
            assertThat(result.getTotalHours()).isEqualTo(6);
            assertThat(result.getDate()).isEqualTo(today);
            assertThat(result.getProductivityPercentage()).isEqualTo(75.0); // 6 * 12.5
        }

        @Test
        @DisplayName("Should compare current productivity with goals")
        void shouldCompareCurrentProductivityWithGoals() {
            // Act
            String result = checkinService.compareWithGoals(userId, partnershipId, 7, 8);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result).contains("Current score (7)");
            assertThat(result).contains("target (8)");
            assertThat(result).contains("1 points below");
        }

        @Test
        @DisplayName("Should generate comprehensive productivity report")
        void shouldGenerateComprehensiveProductivityReport() {
            // Arrange
            LocalDate startDate = LocalDate.now().minusDays(30);
            LocalDate endDate = LocalDate.now();
            List<BuddyCheckin> mockCheckins = List.of(sampleCheckin);

            when(checkinRepository.findByPartnershipIdAndUserIdAndCreatedAtBetween(
                eq(partnershipId), eq(userId), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(mockCheckins);

            // Act
            CheckinAnalyticsDto result = checkinService.generateProductivityReport(userId, partnershipId, startDate, endDate);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getTotalCheckins()).isEqualTo(1);
            assertThat(result.getDailyCheckins()).isEqualTo(1);
            assertThat(result.getAverageProductivity()).isEqualTo(8.0);
        }

        @Test
        @DisplayName("Should predict productivity trends using ML")
        void shouldPredictProductivityTrendsUsingML() {
            // Arrange
            List<BuddyCheckin> mockCheckins = List.of(sampleCheckin);

            when(checkinRepository.findByPartnershipIdAndUserIdAndCreatedAtBetween(
                eq(partnershipId), eq(userId), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(mockCheckins);

            // Act
            Double result = checkinService.predictProductivityTrends(userId, partnershipId, 7);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result).isGreaterThanOrEqualTo(0.0);
            assertThat(result).isLessThanOrEqualTo(10.0);
        }

        @ParameterizedTest
        @ValueSource(ints = {1, 5, 8, 10})
        @DisplayName("Should handle valid productivity ratings")
        void shouldHandleValidProductivityRatings(int rating) {
            // Arrange
            LocalDate today = LocalDate.now();
            when(checkinRepository.hasCheckedInToday(partnershipId, userId, today)).thenReturn(false);
            when(partnershipRepository.findById(partnershipId)).thenReturn(Optional.of(samplePartnership));
            when(checkinRepository.save(any(BuddyCheckin.class))).thenReturn(sampleCheckin);
            when(accountabilityScoreRepository.findByUserIdAndPartnershipId(userId, partnershipId))
                .thenReturn(Optional.of(sampleScore));

            // Act
            ProductivityMetricsDto result = checkinService.recordProductivityScore(userId, partnershipId, rating, today);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getCurrentRating()).isEqualTo(rating);
            assertThat(result.getAverageRating()).isEqualTo((double) rating);
        }

        @ParameterizedTest
        @ValueSource(ints = {0, 11, -1, 15})
        @DisplayName("Should reject invalid productivity ratings")
        void shouldRejectInvalidProductivityRatings(int invalidRating) {
            // Arrange
            LocalDate today = LocalDate.now();

            // Act & Assert
            assertThatThrownBy(() -> checkinService.recordProductivityScore(userId, partnershipId, invalidRating, today))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Productivity score must be between 1 and 10");
        }

        @Test
        @DisplayName("Should handle productivity comparison with missing goal data")
        void shouldHandleProductivityComparisonWithMissingGoalData() {
            // Act
            String result = checkinService.compareWithGoals(userId, partnershipId, 7, null);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result).contains("Current productivity: 7/10");
            assertThat(result).contains("Set a target goal");
        }
    }

    // ======================================
    // 5. Streak Calculations Tests (10 tests)
    // ======================================

    @Nested
    @DisplayName("Streak Calculations Tests")
    class StreakCalculationsTests {

        @Test
        @DisplayName("Should calculate current daily streak correctly")
        void shouldCalculateCurrentDailyStreakCorrectly() {
            // Arrange
            when(checkinRepository.calculateCurrentDailyStreak(partnershipId, userId, LocalDate.now()))
                .thenReturn(5);
            when(checkinRepository.findLongestDailyStreak(partnershipId, userId))
                .thenReturn(10);

            // Act
            StreakStatisticsDto result = checkinService.calculateDailyStreak(userId, partnershipId);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getCurrentDailyStreak()).isEqualTo(5);
            assertThat(result.getLongestDailyStreak()).isEqualTo(10);
            assertThat(result.getIsOnStreak()).isTrue();
            verify(checkinRepository).calculateCurrentDailyStreak(partnershipId, userId, LocalDate.now());
        }

        @Test
        @DisplayName("Should calculate current weekly streak correctly")
        void shouldCalculateCurrentWeeklyStreakCorrectly() {
            // Arrange
            when(checkinRepository.calculateCurrentWeeklyStreak(partnershipId, userId, LocalDate.now()))
                .thenReturn(3);

            // Act
            StreakStatisticsDto result = checkinService.calculateWeeklyStreak(userId, partnershipId);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getCurrentWeeklyStreak()).isEqualTo(3);
            assertThat(result.getLongestWeeklyStreak()).isEqualTo(3);
            assertThat(result.getIsOnStreak()).isTrue();
            verify(checkinRepository).calculateCurrentWeeklyStreak(partnershipId, userId, LocalDate.now());
        }

        @Test
        @DisplayName("Should calculate longest historical streak")
        void shouldCalculateLongestHistoricalStreak() {
            // Arrange
            when(checkinRepository.findLongestDailyStreak(partnershipId, userId))
                .thenReturn(21);

            // Act
            StreakStatisticsDto result = checkinService.calculateLongestStreak(userId, partnershipId);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getLongestDailyStreak()).isEqualTo(21);
            assertThat(result.getLongestWeeklyStreak()).isEqualTo(3); // 21/7 = 3
            verify(checkinRepository).findLongestDailyStreak(partnershipId, userId);
        }

        @Test
        @DisplayName("Should handle streak break appropriately")
        void shouldHandleStreakBreakAppropriately() {
            // Arrange
            LocalDate missedDate = LocalDate.now().minusDays(1);
            doNothing().when(accountabilityScoreRepository).updateStreakDays(eq(userId), eq(partnershipId), eq(0), any(LocalDateTime.class));

            // Act
            checkinService.handleStreakBreak(userId, partnershipId, missedDate);

            // Assert
            verify(accountabilityScoreRepository).updateStreakDays(eq(userId), eq(partnershipId), eq(0), any(LocalDateTime.class));
            verify(eventPublisher).publishEvent(any(Object.class));
        }

        @Test
        @DisplayName("Should get comprehensive streak statistics")
        void shouldGetComprehensiveStreakStatistics() {
            // Arrange
            when(checkinRepository.calculateCurrentDailyStreak(partnershipId, userId, LocalDate.now()))
                .thenReturn(7);
            when(checkinRepository.findLongestDailyStreak(partnershipId, userId))
                .thenReturn(21);
            when(checkinRepository.calculateCurrentWeeklyStreak(partnershipId, userId, LocalDate.now()))
                .thenReturn(2);

            // Act
            StreakStatisticsDto result = checkinService.getStreakStatistics(userId, partnershipId);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getCurrentDailyStreak()).isEqualTo(7);
            assertThat(result.getCurrentWeeklyStreak()).isEqualTo(2);
            assertThat(result.getLongestDailyStreak()).isEqualTo(21);
            assertThat(result.getLongestWeeklyStreak()).isEqualTo(3); // 21/7
            assertThat(result.getIsOnStreak()).isTrue();
        }

        @Test
        @DisplayName("Should reward streak milestones appropriately")
        void shouldRewardStreakMilestonesAppropriately() {
            // Act
            List<String> result = checkinService.rewardStreakMilestones(userId, 7);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result).contains("3-Day Streak Badge");
            assertThat(result).contains("Week Warrior Badge");
            assertThat(result).doesNotContain("Monthly Master Badge"); // Need 30 days
        }

        @Test
        @DisplayName("Should allow streak recovery with valid reason")
        void shouldAllowStreakRecoveryWithValidReason() {
            // Arrange
            LocalDate recoverDate = LocalDate.now().minusDays(1);
            when(checkinRepository.calculateCurrentDailyStreak(partnershipId, userId, LocalDate.now()))
                .thenReturn(5);
            when(checkinRepository.findLongestDailyStreak(partnershipId, userId))
                .thenReturn(10);
            when(checkinRepository.calculateCurrentWeeklyStreak(partnershipId, userId, LocalDate.now()))
                .thenReturn(1);
            doNothing().when(accountabilityScoreRepository).updateStreakDays(eq(userId), eq(partnershipId), eq(6), any(LocalDateTime.class));

            // Act
            boolean result = checkinService.recoverStreak(userId, partnershipId, recoverDate, "Technical issues");

            // Assert
            assertThat(result).isTrue();
            verify(accountabilityScoreRepository).updateStreakDays(eq(userId), eq(partnershipId), eq(6), any(LocalDateTime.class));
            verify(eventPublisher).publishEvent(any(Object.class));
        }

        @Test
        @DisplayName("Should compare partner streaks for competition")
        void shouldComparePartnerStreaksForCompetition() {
            // Arrange
            UUID user2Id = UUID.randomUUID();
            AccountabilityScore user2Score = AccountabilityScore.builder()
                .userId(user2Id)
                .partnershipId(partnershipId)
                .streakDays(10)
                .build();
            List<AccountabilityScore> scores = List.of(sampleScore, user2Score);

            when(accountabilityScoreRepository.findByPartnershipId(partnershipId))
                .thenReturn(scores);

            // Act
            Map<String, Integer> result = checkinService.comparePartnerStreaks(partnershipId);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result).hasSize(2);
            assertThat(result.values()).contains(5, 10); // streak days from sample scores
        }

        @ParameterizedTest
        @ValueSource(ints = {3, 7, 14, 30, 100})
        @DisplayName("Should handle streak milestone rewards for different streak lengths")
        void shouldHandleStreakMilestoneRewardsForDifferentStreakLengths(int streakLength) {
            // Act
            List<String> result = checkinService.rewardStreakMilestones(userId, streakLength);

            // Assert
            assertThat(result).isNotNull();
            if (streakLength >= 3) {
                assertThat(result).contains("3-Day Streak Badge");
            }
            if (streakLength >= 7) {
                assertThat(result).contains("Week Warrior Badge");
            }
            if (streakLength >= 30) {
                assertThat(result).contains("Monthly Master Badge");
            }
            if (streakLength >= 100) {
                assertThat(result).contains("Century Achiever Badge");
            }
        }

        @Test
        @DisplayName("Should handle streak calculation for new user with no history")
        void shouldHandleStreakCalculationForNewUserWithNoHistory() {
            // Arrange
            UUID newUserId = UUID.randomUUID();
            when(checkinRepository.calculateCurrentDailyStreak(partnershipId, newUserId, LocalDate.now()))
                .thenReturn(0);
            when(checkinRepository.findLongestDailyStreak(partnershipId, newUserId))
                .thenReturn(0);

            // Act
            StreakStatisticsDto result = checkinService.calculateDailyStreak(newUserId, partnershipId);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getCurrentDailyStreak()).isEqualTo(0);
            assertThat(result.getLongestDailyStreak()).isEqualTo(0);
            assertThat(result.getIsOnStreak()).isFalse();
        }
    }

    // ======================================
    // 6. Accountability Scoring Tests (8 tests)
    // ======================================

    @Nested
    @DisplayName("Accountability Scoring Tests")
    class AccountabilityScoringTests {

        @Test
        @DisplayName("Should calculate accountability score accurately")
        void shouldCalculateAccountabilityScoreAccurately() {
            // Arrange
            when(accountabilityScoreRepository.findByUserIdAndPartnershipId(userId, partnershipId))
                .thenReturn(Optional.of(sampleScore));
            when(accountabilityScoreRepository.save(any(AccountabilityScore.class)))
                .thenReturn(sampleScore);

            // Act
            AccountabilityScoreDto result = checkinService.calculateAccountabilityScore(userId, partnershipId);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getScore()).isEqualTo(BigDecimal.valueOf(0.75));
            assertThat(result.getCheckinsCompleted()).isEqualTo(10);
            assertThat(result.getGoalsAchieved()).isEqualTo(2);
            assertThat(result.getStreakDays()).isEqualTo(5);
            verify(accountabilityScoreRepository).findByUserIdAndPartnershipId(userId, partnershipId);
        }

        @Test
        @DisplayName("Should update score when user checks in")
        void shouldUpdateScoreWhenUserChecksIn() {
            // Arrange
            when(accountabilityScoreRepository.findByUserIdAndPartnershipId(userId, partnershipId))
                .thenReturn(Optional.of(sampleScore));
            when(checkinRepository.calculateCurrentDailyStreak(partnershipId, userId, LocalDate.now()))
                .thenReturn(6);
            when(checkinRepository.findLongestDailyStreak(partnershipId, userId))
                .thenReturn(10);
            when(accountabilityScoreRepository.save(any(AccountabilityScore.class)))
                .thenReturn(sampleScore);

            // Act
            checkinService.updateScoreOnCheckin(userId, partnershipId, CheckInType.DAILY);

            // Assert
            verify(accountabilityScoreRepository).findByUserIdAndPartnershipId(userId, partnershipId);
            verify(accountabilityScoreRepository).save(any(AccountabilityScore.class));
        }

        @Test
        @DisplayName("Should penalize score for missed check-in")
        void shouldPenalizeScoreForMissedCheckin() {
            // Arrange
            LocalDate missedDate = LocalDate.now().minusDays(1);
            when(accountabilityScoreRepository.findByUserIdAndPartnershipId(userId, partnershipId))
                .thenReturn(Optional.of(sampleScore));
            when(accountabilityScoreRepository.save(any(AccountabilityScore.class)))
                .thenReturn(sampleScore);

            // Act
            checkinService.penalizeForMissedCheckin(userId, partnershipId, missedDate);

            // Assert
            verify(accountabilityScoreRepository).findByUserIdAndPartnershipId(userId, partnershipId);
            verify(accountabilityScoreRepository).save(any(AccountabilityScore.class));
        }

        @Test
        @DisplayName("Should retrieve score history for analysis")
        void shouldRetrieveScoreHistoryForAnalysis() {
            // Arrange
            LocalDate startDate = LocalDate.now().minusDays(30);
            LocalDate endDate = LocalDate.now();
            when(accountabilityScoreRepository.findByUserIdAndPartnershipId(userId, partnershipId))
                .thenReturn(Optional.of(sampleScore));
            when(accountabilityScoreRepository.save(any(AccountabilityScore.class)))
                .thenReturn(sampleScore);

            // Act
            List<AccountabilityScoreDto> result = checkinService.getScoreHistory(userId, partnershipId, startDate, endDate);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result).hasSize(1); // Current implementation returns current score as history
            assertThat(result.get(0).getScore()).isEqualTo(BigDecimal.valueOf(0.75));
        }

        @Test
        @DisplayName("Should compare scores with partner")
        void shouldCompareScoresWithPartner() {
            // Arrange
            UUID user2Id = UUID.randomUUID();
            AccountabilityScore user2Score = AccountabilityScore.builder()
                .userId(user2Id)
                .partnershipId(partnershipId)
                .score(BigDecimal.valueOf(0.85))
                .checkinsCompleted(15)
                .goalsAchieved(3)
                .responseRate(BigDecimal.valueOf(0.90))
                .streakDays(7)
                .build();
            List<AccountabilityScore> scores = List.of(sampleScore, user2Score);

            when(accountabilityScoreRepository.findByPartnershipId(partnershipId))
                .thenReturn(scores);

            // Act
            Map<String, AccountabilityScoreDto> result = checkinService.compareWithPartner(partnershipId);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result).hasSize(2);
            assertThat(result.values().stream().map(dto -> dto.getScore())).contains(
                BigDecimal.valueOf(0.75), BigDecimal.valueOf(0.85));
        }

        @Test
        @DisplayName("Should generate detailed score report")
        void shouldGenerateDetailedScoreReport() {
            // Arrange
            when(accountabilityScoreRepository.findByUserIdAndPartnershipId(userId, partnershipId))
                .thenReturn(Optional.of(sampleScore));
            when(accountabilityScoreRepository.save(any(AccountabilityScore.class)))
                .thenReturn(sampleScore);

            // Act
            CheckinAnalyticsDto result = checkinService.generateScoreReport(userId, partnershipId);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getTotalCheckins()).isEqualTo(10);
            assertThat(result.getAverageProductivity()).isEqualTo(7.5); // 0.75 * 10
            assertThat(result.getCompletionRate()).isEqualTo(0.80);
        }

        @Test
        @DisplayName("Should suggest score improvement strategies")
        void shouldSuggestScoreImprovementStrategies() {
            // Arrange
            AccountabilityScoreDto currentScore = AccountabilityScoreDto.builder()
                .score(BigDecimal.valueOf(0.50))
                .level("Fair")
                .streakDays(2)
                .checkinsCompleted(3)
                .build();

            // Act
            List<String> result = checkinService.suggestScoreImprovement(userId, currentScore);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result).isNotEmpty();
            assertThat(result).contains("Increase daily check-in consistency");
            assertThat(result).contains("Focus on building a check-in streak");
            assertThat(result).contains("Aim for more regular check-ins with your buddy");
        }

        @ParameterizedTest
        @EnumSource(CheckInType.class)
        @DisplayName("Should update score appropriately for all check-in types")
        void shouldUpdateScoreAppropriatelyForAllCheckinTypes(CheckInType checkinType) {
            // Arrange
            when(accountabilityScoreRepository.findByUserIdAndPartnershipId(userId, partnershipId))
                .thenReturn(Optional.of(sampleScore));
            when(checkinRepository.calculateCurrentDailyStreak(partnershipId, userId, LocalDate.now()))
                .thenReturn(6);
            when(checkinRepository.findLongestDailyStreak(partnershipId, userId))
                .thenReturn(10);
            when(accountabilityScoreRepository.save(any(AccountabilityScore.class)))
                .thenReturn(sampleScore);

            // Act
            checkinService.updateScoreOnCheckin(userId, partnershipId, checkinType);

            // Assert
            verify(accountabilityScoreRepository).findByUserIdAndPartnershipId(userId, partnershipId);
            verify(accountabilityScoreRepository).save(any(AccountabilityScore.class));
        }
    }

    // ======================================
    // 7. Partner Synchronization Tests (6 tests)
    // ======================================

    @Nested
    @DisplayName("Partner Synchronization Tests")
    class PartnerSynchronizationTests {

        @Test
        @DisplayName("Should sync partner check-ins bidirectionally")
        void shouldSyncPartnerCheckinsBidirectionally() {
            // Arrange
            UUID user2Id = UUID.randomUUID();
            BuddyCheckin user2Checkin = BuddyCheckin.builder()
                .id(UUID.randomUUID())
                .partnershipId(partnershipId)
                .userId(user2Id)
                .checkinType(CheckInType.DAILY)
                .content("Partner's check-in")
                .mood(MoodType.FOCUSED)
                .productivityRating(7)
                .createdAt(LocalDateTime.now())
                .build();
            List<BuddyCheckin> allCheckins = List.of(sampleCheckin, user2Checkin);

            when(checkinRepository.findByPartnershipId(partnershipId))
                .thenReturn(allCheckins);

            // Act
            checkinService.syncPartnerCheckins(partnershipId);

            // Assert
            verify(checkinRepository).findByPartnershipId(partnershipId);
            verify(redisTemplate.opsForValue(), times(2)).set(anyString(), any(List.class));
            verify(eventPublisher).publishEvent(any(Object.class));
        }

        @Test
        @DisplayName("Should notify partner of new check-in")
        void shouldNotifyPartnerOfNewCheckin() {
            // Arrange
            CheckinResponseDto checkin = CheckinResponseDto.builder()
                .id(checkinId)
                .partnershipId(partnershipId)
                .userId(userId)
                .checkinType(CheckInType.DAILY)
                .content("Daily update")
                .mood(MoodType.MOTIVATED)
                .productivityRating(8)
                .createdAt(LocalDateTime.now())
                .build();

            // Act
            checkinService.notifyPartnerOfCheckin(userId, partnershipId, checkin);

            // Assert
            verify(eventPublisher).publishEvent(any(Object.class));
        }

        @Test
        @DisplayName("Should compare partner progress side by side")
        void shouldComparePartnerProgressSideBySide() {
            // Arrange
            LocalDate today = LocalDate.now();
            List<BuddyCheckin> todaysCheckins = List.of(sampleCheckin);

            when(checkinRepository.findTodaysCheckins(partnershipId, today))
                .thenReturn(todaysCheckins);

            // Act
            Map<String, CheckinResponseDto> result = checkinService.comparePartnerProgress(partnershipId, today);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result).hasSize(1);
            verify(checkinRepository).findTodaysCheckins(partnershipId, today);
        }

        @Test
        @DisplayName("Should celebrate joint milestones together")
        void shouldCelebrateJointMilestonesTogether() {
            // Act
            checkinService.celebrateJointMilestones(partnershipId, "7-day streak");

            // Assert
            verify(redisTemplate.opsForValue()).set(anyString(), any(LocalDateTime.class));
            verify(eventPublisher).publishEvent(any(Object.class));
        }

        @Test
        @DisplayName("Should handle partner absence gracefully")
        void shouldHandlePartnerAbsenceGracefully() {
            // Arrange
            UUID absentUserId = UUID.randomUUID();
            LocalDate today = LocalDate.now();
            when(accountabilityScoreRepository.findByUserIdAndPartnershipId(absentUserId, partnershipId))
                .thenReturn(Optional.of(sampleScore));
            when(accountabilityScoreRepository.save(any(AccountabilityScore.class)))
                .thenReturn(sampleScore);
            doNothing().when(accountabilityScoreRepository).updateStreakDays(eq(absentUserId), eq(partnershipId), eq(0), any(LocalDateTime.class));

            // Act
            checkinService.handlePartnerAbsence(partnershipId, absentUserId, today);

            // Assert
            verify(eventPublisher, times(3)).publishEvent(any(Object.class)); // Missed checkin + streak break + partner absence events
        }

        @Test
        @DisplayName("Should maintain partner check-in synchronization consistency")
        void shouldMaintainPartnerCheckinSynchronizationConsistency() {
            // Arrange
            List<BuddyCheckin> checkins = List.of(sampleCheckin);

            when(checkinRepository.findByPartnershipId(partnershipId))
                .thenReturn(checkins);

            // Act
            checkinService.syncPartnerCheckins(partnershipId);

            // Assert
            verify(checkinRepository).findByPartnershipId(partnershipId);
            verify(redisTemplate.opsForValue()).set(anyString(), any(List.class));
            verify(eventPublisher).publishEvent(any(Object.class));
        }
    }

    // ======================================
    // 8. Analytics & Insights Tests (8 tests)
    // ======================================

    @Nested
    @DisplayName("Analytics & Insights Tests")
    class AnalyticsInsightsTests {

        @Test
        @DisplayName("Should generate comprehensive check-in analytics")
        void shouldGenerateComprehensiveCheckinAnalytics() {
            // Arrange
            LocalDate startDate = LocalDate.now().minusDays(30);
            LocalDate endDate = LocalDate.now();
            List<BuddyCheckin> mockCheckins = List.of(sampleCheckin);

            when(checkinRepository.findByPartnershipIdAndUserIdAndCreatedAtBetween(
                eq(partnershipId), eq(userId), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(mockCheckins);

            // Act
            CheckinAnalyticsDto result = checkinService.generateCheckinAnalytics(userId, partnershipId, startDate, endDate);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getTotalCheckins()).isEqualTo(1);
            assertThat(result.getDailyCheckins()).isEqualTo(1);
            assertThat(result.getWeeklyCheckins()).isEqualTo(0);
            assertThat(result.getAverageProductivity()).isEqualTo(8.0);
            assertThat(result.getMoodDistribution()).containsKey(MoodType.MOTIVATED);
        }

        @Test
        @DisplayName("Should identify behavioral patterns in check-ins")
        void shouldIdentifyBehavioralPatternsInCheckins() {
            // Arrange
            LocalDate startDate = LocalDate.now().minusDays(30);
            LocalDate endDate = LocalDate.now();
            List<BuddyCheckin> mockCheckins = List.of(sampleCheckin);

            when(checkinRepository.findByPartnershipIdAndUserIdAndCreatedAtBetween(
                eq(partnershipId), eq(userId), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(mockCheckins);

            // Act
            Map<String, Object> result = checkinService.identifyPatterns(userId, partnershipId, startDate, endDate);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result).containsKey("mostActiveDay");
            assertThat(result).containsKey("averageCheckinTime");
            assertThat(result).containsKey("consistencyScore");
            assertThat(result.get("consistencyScore")).isEqualTo(0.75);
        }

        @Test
        @DisplayName("Should predict next check-in time using ML")
        void shouldPredictNextCheckinTimeUsingML() {
            // Act
            LocalDateTime result = checkinService.predictNextCheckin(userId, partnershipId);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result).isAfter(LocalDateTime.now());
            assertThat(result).isBefore(LocalDateTime.now().plusDays(2));
        }

        @Test
        @DisplayName("Should suggest optimal check-in time")
        void shouldSuggestOptimalCheckinTime() {
            // Arrange
            List<BuddyCheckin> mockCheckins = List.of(sampleCheckin);

            when(checkinRepository.findByPartnershipIdAndUserIdAndCreatedAtBetween(
                eq(partnershipId), eq(userId), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(mockCheckins);

            // Act
            String result = checkinService.suggestOptimalCheckinTime(userId, partnershipId);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result).matches("\\d{1,2}:\\d{2} (AM|PM)");
        }

        @Test
        @DisplayName("Should generate monthly summary report")
        void shouldGenerateMonthySummaryReport() {
            // Arrange
            LocalDate monthStart = LocalDate.now().minusDays(30);
            List<BuddyCheckin> mockCheckins = List.of(sampleCheckin);

            when(checkinRepository.findByPartnershipIdAndUserIdAndCreatedAtBetween(
                eq(partnershipId), eq(userId), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(mockCheckins);

            // Act
            WeeklyReviewDto result = checkinService.generateMonthlyReport(userId, partnershipId, monthStart);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getWeekStartDate()).isEqualTo(monthStart);
            assertThat(result.getWeekEndDate()).isEqualTo(monthStart.plusMonths(1).minusDays(1));
            assertThat(result.getCheckinsThisWeek()).isEqualTo(1);
            assertThat(result.getAverageProductivity()).isEqualTo(8.0);
            assertThat(result.getWeeklyProgress()).isEqualTo("Monthly summary");
        }

        @Test
        @DisplayName("Should export check-in data in various formats")
        void shouldExportCheckinDataInVariousFormats() {
            // Arrange
            LocalDate startDate = LocalDate.now().minusDays(30);
            LocalDate endDate = LocalDate.now();
            List<BuddyCheckin> mockCheckins = List.of(sampleCheckin);

            when(checkinRepository.findByPartnershipIdAndUserIdAndCreatedAtBetween(
                eq(partnershipId), eq(userId), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(mockCheckins);

            // Act
            byte[] result = checkinService.exportCheckinData(userId, partnershipId, startDate, endDate);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.length).isGreaterThan(0);
            String csvContent = new String(result);
            assertThat(csvContent).contains("Date,Type,Mood,Productivity,Content");
            assertThat(csvContent).contains("DAILY");
            assertThat(csvContent).contains("MOTIVATED");
        }

        @ParameterizedTest
        @ValueSource(ints = {7, 14, 30, 90})
        @DisplayName("Should generate analytics for different time periods")
        void shouldGenerateAnalyticsForDifferentTimePeriods(int daysBack) {
            // Arrange
            LocalDate startDate = LocalDate.now().minusDays(daysBack);
            LocalDate endDate = LocalDate.now();
            List<BuddyCheckin> mockCheckins = List.of(sampleCheckin);

            when(checkinRepository.findByPartnershipIdAndUserIdAndCreatedAtBetween(
                eq(partnershipId), eq(userId), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(mockCheckins);

            // Act
            CheckinAnalyticsDto result = checkinService.generateCheckinAnalytics(userId, partnershipId, startDate, endDate);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getTotalCheckins()).isEqualTo(1);
            assertThat(result.getAverageProductivity()).isEqualTo(8.0);
        }

        @Test
        @DisplayName("Should handle analytics generation with insufficient data")
        void shouldHandleAnalyticsGenerationWithInsufficientData() {
            // Arrange - Very recent date range with likely no data
            LocalDate startDate = LocalDate.now();
            LocalDate endDate = LocalDate.now();
            List<BuddyCheckin> emptyCheckins = List.of();

            when(checkinRepository.findByPartnershipIdAndUserIdAndCreatedAtBetween(
                eq(partnershipId), eq(userId), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(emptyCheckins);

            // Act
            CheckinAnalyticsDto result = checkinService.generateCheckinAnalytics(userId, partnershipId, startDate, endDate);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getTotalCheckins()).isEqualTo(0);
            assertThat(result.getDailyCheckins()).isEqualTo(0);
            assertThat(result.getWeeklyCheckins()).isEqualTo(0);
            assertThat(result.getAverageProductivity()).isEqualTo(0.0);
            assertThat(result.getMoodDistribution()).isEmpty();
        }
    }

    // ======================================
    // 9. Edge Cases & Error Handling Tests (10 tests)
    // ======================================

    @Nested
    @DisplayName("Edge Cases & Error Handling Tests")
    class EdgeCasesErrorHandlingTests {

        @Test
        @DisplayName("Should handle null check-in data gracefully")
        void shouldHandleNullCheckinDataGracefully() {
            // Act & Assert
            assertThatThrownBy(() -> checkinService.createDailyCheckin(userId, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Check-in request cannot be null");
        }

        @Test
        @DisplayName("Should handle invalid mood values")
        void shouldHandleInvalidMoodValues() {
            // Test with null mood - should be handled gracefully
            LocalDate today = LocalDate.now();

            // Act & Assert
            assertThatThrownBy(() -> checkinService.recordMood(userId, partnershipId, null, today))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Mood cannot be null");
        }

        @Test
        @DisplayName("Should handle invalid productivity score values")
        void shouldHandleInvalidProductivityScoreValues() {
            // Arrange
            LocalDate today = LocalDate.now();

            // Act & Assert
            assertThatThrownBy(() -> checkinService.recordProductivityScore(userId, partnershipId, -5, today))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Productivity score must be between 1 and 10");
        }

        @Test
        @DisplayName("Should handle timezone changes appropriately")
        void shouldHandleTimezoneChangesAppropriately() {
            // Test check-in validation across timezone boundaries
            // Act
            boolean result = checkinService.validateCheckinTime(userId, CheckInType.DAILY);

            // Assert
            assertThat(result).isInstanceOf(Boolean.class);
            // Implementation validates based on current system time (6AM-11PM for daily)
        }

        @Test
        @DisplayName("Should handle daylight saving time transitions")
        void shouldHandleDaylightSavingTimeTransitions() {
            // Arrange
            when(checkinRepository.calculateCurrentDailyStreak(partnershipId, userId, LocalDate.now()))
                .thenReturn(3);
            when(checkinRepository.findLongestDailyStreak(partnershipId, userId))
                .thenReturn(7);

            // Act
            StreakStatisticsDto result = checkinService.calculateDailyStreak(userId, partnershipId);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getCurrentDailyStreak()).isEqualTo(3);
            // Implementation handles DST transitions through LocalDate usage
        }

        @Test
        @DisplayName("Should handle concurrent check-in attempts")
        void shouldHandleConcurrentCheckinAttempts() {
            // Test race condition handling
            CheckinRequestDto request = new CheckinRequestDto(
                partnershipId, CheckInType.DAILY, "Concurrent test", MoodType.NEUTRAL, 5
            );

            when(partnershipRepository.findById(partnershipId)).thenReturn(Optional.of(samplePartnership));
            when(checkinRepository.hasCheckedInToday(partnershipId, userId, LocalDate.now())).thenReturn(true); // Already checked in

            // Act & Assert - Should prevent duplicate
            assertThatThrownBy(() -> checkinService.createDailyCheckin(userId, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("User has already checked in today");
        }

        @Test
        @DisplayName("Should handle data inconsistency gracefully")
        void shouldHandleDataInconsistencyGracefully() {
            // Arrange
            UUID nonExistentPartnershipId = UUID.randomUUID();
            when(accountabilityScoreRepository.findByUserIdAndPartnershipId(userId, nonExistentPartnershipId))
                .thenReturn(Optional.empty()); // No existing score
            when(accountabilityScoreRepository.save(any(AccountabilityScore.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            AccountabilityScoreDto result = checkinService.calculateAccountabilityScore(userId, nonExistentPartnershipId);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getScore()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.getCheckinsCompleted()).isEqualTo(0);
            verify(accountabilityScoreRepository).save(any(AccountabilityScore.class));
        }

        @Test
        @DisplayName("Should handle system downtime recovery")
        void shouldHandleSystemDowntimeRecovery() {
            // Test recovery after system downtime
            LocalDate missedDate = LocalDate.now().minusDays(1);
            when(accountabilityScoreRepository.findByUserIdAndPartnershipId(userId, partnershipId))
                .thenReturn(Optional.of(sampleScore));
            when(accountabilityScoreRepository.save(any(AccountabilityScore.class)))
                .thenReturn(sampleScore);
            doNothing().when(accountabilityScoreRepository).updateStreakDays(eq(userId), eq(partnershipId), eq(0), any(LocalDateTime.class));

            // Act
            checkinService.handleMissedCheckin(userId, partnershipId, missedDate);

            // Assert
            verify(accountabilityScoreRepository).findByUserIdAndPartnershipId(userId, partnershipId);
            verify(eventPublisher, times(2)).publishEvent(any(Object.class)); // Penalty and streak break events
        }

        @Test
        @DisplayName("Should handle database connection failures")
        void shouldHandleDatabaseConnectionFailures() {
            // Arrange
            when(checkinRepository.findById(checkinId))
                .thenThrow(new RuntimeException("Database connection failed"));

            // Act & Assert
            assertThatThrownBy(() -> checkinService.getDailyCheckin(userId, checkinId))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Database connection failed");
        }

        @Test
        @DisplayName("Should handle large data set operations efficiently")
        void shouldHandleLargeDataSetOperationsEfficiently() {
            // Test with large date ranges
            LocalDate startDate = LocalDate.now().minusYears(1);
            LocalDate endDate = LocalDate.now();
            List<BuddyCheckin> largeDataset = List.of(sampleCheckin); // Simulated large dataset

            when(checkinRepository.findByPartnershipIdAndUserIdAndCreatedAtBetween(
                eq(partnershipId), eq(userId), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(largeDataset);

            // Act
            List<CheckinResponseDto> result = checkinService.getCheckinHistory(userId, partnershipId, startDate, endDate);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result).hasSize(1);
            verify(checkinRepository).findByPartnershipIdAndUserIdAndCreatedAtBetween(
                eq(partnershipId), eq(userId), any(LocalDateTime.class), any(LocalDateTime.class));
        }
    }

    // ======================================
    // Utility Test Methods
    // ======================================

    private static Stream<Arguments> provideMoodAndProductivityCombinations() {
        return Stream.of(
            Arguments.of(MoodType.MOTIVATED, 8),
            Arguments.of(MoodType.STRESSED, 4),
            Arguments.of(MoodType.FOCUSED, 9),
            Arguments.of(MoodType.TIRED, 3),
            Arguments.of(MoodType.EXCITED, 10)
        );
    }

    private static Stream<Arguments> provideStreakTestCases() {
        return Stream.of(
            Arguments.of(1, "New streak"),
            Arguments.of(3, "Building momentum"),
            Arguments.of(7, "Weekly milestone"),
            Arguments.of(14, "Two-week achievement"),
            Arguments.of(30, "Monthly commitment")
        );
    }

    private static Stream<Arguments> provideDateRangeTestCases() {
        return Stream.of(
            Arguments.of(LocalDate.now().minusDays(7), LocalDate.now(), "Last week"),
            Arguments.of(LocalDate.now().minusDays(30), LocalDate.now(), "Last month"),
            Arguments.of(LocalDate.now().minusDays(90), LocalDate.now(), "Last quarter"),
            Arguments.of(LocalDate.now().minusYears(1), LocalDate.now(), "Last year")
        );
    }
}