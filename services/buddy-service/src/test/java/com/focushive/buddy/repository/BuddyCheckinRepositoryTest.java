package com.focushive.buddy.repository;

import com.focushive.buddy.config.TestContainersConfiguration;
import com.focushive.buddy.constant.CheckInType;
import com.focushive.buddy.constant.MoodType;
import com.focushive.buddy.constant.PartnershipStatus;
import com.focushive.buddy.entity.BuddyCheckin;
import com.focushive.buddy.entity.BuddyPartnership;
import com.focushive.buddy.entity.AccountabilityScore;
import com.focushive.buddy.integration.BaseIntegrationTest;
import com.focushive.buddy.integration.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * TDD Repository test for BuddyCheckin entity.
 *
 * RED PHASE - These tests are designed to FAIL initially because:
 * - BuddyCheckin entity doesn't exist yet
 * - BuddyCheckinRepository doesn't exist yet
 * - AccountabilityScore entity doesn't exist yet
 * - Related methods and relationships haven't been implemented
 *
 * This test class defines the expected behavior and API for check-in functionality.
 * Tests MUST be written FIRST, then entities and repositories are implemented to make them pass.
 *
 * Expected compilation failures:
 * - "Cannot resolve symbol 'BuddyCheckin'"
 * - "Cannot resolve symbol 'BuddyCheckinRepository'"
 * - "Cannot resolve symbol 'AccountabilityScore'"
 * - "Cannot resolve symbol 'AccountabilityScoreRepository'"
 *
 * Expected runtime failures (after compilation is fixed):
 * - "Table 'buddy_checkins' doesn't exist"
 * - "Column not found" errors
 * - "Method not found" errors for custom repository methods
 */
@DisplayName("BuddyCheckin Repository Tests (TDD - RED Phase)")
class BuddyCheckinRepositoryTest extends AbstractRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private BuddyCheckinRepository checkinRepository;

    @Autowired
    private BuddyPartnershipRepository partnershipRepository;

    @Autowired
    private AccountabilityScoreRepository accountabilityRepository;

    private TestDataBuilder dataBuilder;
    private Clock testClock;
    private BuddyPartnership testPartnership;
    private UUID testUser1Id;
    private UUID testUser2Id;
    private LocalDateTime testTime;

    @BeforeEach
    void setUp() {
        dataBuilder = new TestDataBuilder();
        testTime = LocalDateTime.of(2025, 1, 15, 10, 0, 0);
        testClock = Clock.fixed(testTime.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

        testUser1Id = UUID.randomUUID();
        testUser2Id = UUID.randomUUID();

        // Create test partnership for check-ins
        testPartnership = dataBuilder.buildBuddyPartnership()
                .withUser1Id(testUser1Id)
                .withUser2Id(testUser2Id)
                .withStatus(PartnershipStatus.ACTIVE)
                .withStartedAt(ZonedDateTime.now(testClock).minusDays(7))
                .build();

        entityManager.persistAndFlush(testPartnership);
    }

    @Nested
    @DisplayName("Basic CRUD Operations")
    class BasicCrudOperations {

        @Test
        @DisplayName("Should save and retrieve a check-in successfully")
        void testSaveCheckin() {
            // Given
            BuddyCheckin checkin = BuddyCheckin.builder()
                    .partnershipId(testPartnership.getId())
                    .userId(testUser1Id)
                    .checkinType(CheckInType.DAILY)
                    .content("Had a productive morning session!")
                    .mood(MoodType.FOCUSED)
                    .productivityRating(8)
                    .createdAt(testTime)
                    .build();

            // When
            BuddyCheckin saved = checkinRepository.save(checkin);
            entityManager.flush();

            // Then
            assertThat(saved).isNotNull();
            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getPartnershipId()).isEqualTo(testPartnership.getId());
            assertThat(saved.getUserId()).isEqualTo(testUser1Id);
            assertThat(saved.getCheckinType()).isEqualTo(CheckInType.DAILY);
            assertThat(saved.getContent()).isEqualTo("Had a productive morning session!");
            assertThat(saved.getMood()).isEqualTo(MoodType.FOCUSED);
            assertThat(saved.getProductivityRating()).isEqualTo(8);
            assertThat(saved.getCreatedAt()).isEqualTo(testTime);
        }

        @Test
        @DisplayName("Should find check-in by ID")
        void testFindCheckinById() {
            // Given
            BuddyCheckin checkin = createAndSaveCheckin(testUser1Id, CheckInType.DAILY);

            // When
            Optional<BuddyCheckin> found = checkinRepository.findById(checkin.getId());

            // Then
            assertThat(found).isPresent();
            assertThat(found.get().getId()).isEqualTo(checkin.getId());
            assertThat(found.get().getPartnershipId()).isEqualTo(testPartnership.getId());
            assertThat(found.get().getUserId()).isEqualTo(testUser1Id);
        }

        @Test
        @DisplayName("Should update check-in details")
        void testUpdateCheckin() {
            // Given
            BuddyCheckin checkin = createAndSaveCheckin(testUser1Id, CheckInType.DAILY);

            // When
            checkin.setContent("Updated: Great progress on my goals today!");
            checkin.setMood(MoodType.EXCITED);
            checkin.setProductivityRating(9);

            BuddyCheckin updated = checkinRepository.save(checkin);
            entityManager.flush();
            entityManager.clear();

            // Then
            BuddyCheckin retrieved = checkinRepository.findById(updated.getId()).orElseThrow();
            assertThat(retrieved.getContent()).isEqualTo("Updated: Great progress on my goals today!");
            assertThat(retrieved.getMood()).isEqualTo(MoodType.EXCITED);
            assertThat(retrieved.getProductivityRating()).isEqualTo(9);
        }

        @Test
        @DisplayName("Should delete check-in")
        void testDeleteCheckin() {
            // Given
            BuddyCheckin checkin = createAndSaveCheckin(testUser1Id, CheckInType.DAILY);
            UUID checkinId = checkin.getId();

            // When
            checkinRepository.delete(checkin);
            entityManager.flush();

            // Then
            Optional<BuddyCheckin> deleted = checkinRepository.findById(checkinId);
            assertThat(deleted).isEmpty();
        }

        @Test
        @DisplayName("Should find all check-ins by partnership ID")
        void testFindByPartnershipId() {
            // Given
            createAndSaveCheckin(testUser1Id, CheckInType.DAILY);
            createAndSaveCheckin(testUser2Id, CheckInType.DAILY);
            createAndSaveCheckin(testUser1Id, CheckInType.WEEKLY);

            // When
            List<BuddyCheckin> checkins = checkinRepository.findByPartnershipId(testPartnership.getId());

            // Then
            assertThat(checkins).hasSize(3);
            assertThat(checkins).allMatch(c -> c.getPartnershipId().equals(testPartnership.getId()));
        }
    }

    @Nested
    @DisplayName("Check-in Types and Validation")
    class CheckinTypesAndValidation {

        @Test
        @DisplayName("Should create check-in with default DAILY type")
        void testCreateDailyCheckin() {
            // Given
            BuddyCheckin checkin = BuddyCheckin.builder()
                    .partnershipId(testPartnership.getId())
                    .userId(testUser1Id)
                    .content("Daily progress update")
                    .build();

            // When
            BuddyCheckin saved = checkinRepository.save(checkin);

            // Then
            assertThat(saved.getCheckinType()).isEqualTo(CheckInType.DAILY);
        }

        @Test
        @DisplayName("Should create WEEKLY check-in type")
        void testCreateWeeklyCheckin() {
            // Given
            BuddyCheckin checkin = BuddyCheckin.builder()
                    .partnershipId(testPartnership.getId())
                    .userId(testUser1Id)
                    .checkinType(CheckInType.WEEKLY)
                    .content("Weekly summary of achievements")
                    .build();

            // When
            BuddyCheckin saved = checkinRepository.save(checkin);

            // Then
            assertThat(saved.getCheckinType()).isEqualTo(CheckInType.WEEKLY);
        }

        @Test
        @DisplayName("Should create MILESTONE check-in type")
        void testCreateMilestoneCheckin() {
            // Given
            BuddyCheckin checkin = BuddyCheckin.builder()
                    .partnershipId(testPartnership.getId())
                    .userId(testUser1Id)
                    .checkinType(CheckInType.MILESTONE)
                    .content("Reached major project milestone!")
                    .build();

            // When
            BuddyCheckin saved = checkinRepository.save(checkin);

            // Then
            assertThat(saved.getCheckinType()).isEqualTo(CheckInType.MILESTONE);
        }

        @Test
        @DisplayName("Should reject invalid check-in type")
        void testInvalidCheckinType() {
            // This test will verify database constraint validation
            // Expected to fail with constraint violation when invalid enum value is used

            // Note: This test may need to be implemented at the database level
            // or through custom validation depending on entity implementation
            assertThatThrownBy(() -> {
                // Direct SQL insertion with invalid type to test constraint
                entityManager.getEntityManager()
                    .createNativeQuery("INSERT INTO buddy_checkins (id, partnership_id, user_id, checkin_type) VALUES (?, ?, ?, ?)")
                    .setParameter(1, UUID.randomUUID())
                    .setParameter(2, testPartnership.getId())
                    .setParameter(3, testUser1Id)
                    .setParameter(4, "INVALID_TYPE")
                    .executeUpdate();
                entityManager.flush();
            }).hasCauseInstanceOf(Exception.class); // Database constraint violation expected
        }

        @Test
        @DisplayName("Should validate mood type values")
        void testMoodValidation() {
            // Given - Valid mood types from MoodType enum
            MoodType[] validMoods = {MoodType.MOTIVATED, MoodType.FOCUSED, MoodType.STRESSED,
                                   MoodType.TIRED, MoodType.EXCITED, MoodType.NEUTRAL,
                                   MoodType.FRUSTRATED, MoodType.ACCOMPLISHED};

            // When & Then - All valid mood types should save successfully
            for (MoodType mood : validMoods) {
                BuddyCheckin checkin = BuddyCheckin.builder()
                        .partnershipId(testPartnership.getId())
                        .userId(testUser1Id)
                        .checkinType(CheckInType.DAILY)
                        .mood(mood)
                        .build();

                BuddyCheckin saved = checkinRepository.save(checkin);
                assertThat(saved.getMood()).isEqualTo(mood);
            }
        }

        @Test
        @DisplayName("Should validate productivity rating range (1-10)")
        void testProductivityRatingRange() {
            // Given - Valid rating
            BuddyCheckin validCheckin = BuddyCheckin.builder()
                    .partnershipId(testPartnership.getId())
                    .userId(testUser1Id)
                    .checkinType(CheckInType.DAILY)
                    .productivityRating(5)
                    .build();

            // When & Then - Valid rating should save
            BuddyCheckin saved = checkinRepository.save(validCheckin);
            assertThat(saved.getProductivityRating()).isEqualTo(5);

            // Then - Invalid ratings should be rejected by database constraint
            assertThatThrownBy(() -> {
                entityManager.getEntityManager()
                    .createNativeQuery("INSERT INTO buddy_checkins (id, partnership_id, user_id, productivity_rating) VALUES (?, ?, ?, ?)")
                    .setParameter(1, UUID.randomUUID())
                    .setParameter(2, testPartnership.getId())
                    .setParameter(3, testUser1Id)
                    .setParameter(4, 11) // Invalid: > 10
                    .executeUpdate();
                entityManager.flush();
            }).hasCauseInstanceOf(Exception.class);

            assertThatThrownBy(() -> {
                entityManager.getEntityManager()
                    .createNativeQuery("INSERT INTO buddy_checkins (id, partnership_id, user_id, productivity_rating) VALUES (?, ?, ?, ?)")
                    .setParameter(1, UUID.randomUUID())
                    .setParameter(2, testPartnership.getId())
                    .setParameter(3, testUser1Id)
                    .setParameter(4, 0) // Invalid: < 1
                    .executeUpdate();
                entityManager.flush();
            }).hasCauseInstanceOf(Exception.class);
        }
    }

    @Nested
    @DisplayName("Partnership Relationship Tests")
    class PartnershipRelationshipTests {

        @Test
        @DisplayName("Should maintain foreign key relationship to partnership")
        void testCheckinBelongsToPartnership() {
            // Given
            BuddyCheckin checkin = createAndSaveCheckin(testUser1Id, CheckInType.DAILY);

            // When
            BuddyPartnership partnership = partnershipRepository.findById(testPartnership.getId()).orElseThrow();

            // Then
            assertThat(checkin.getPartnershipId()).isEqualTo(partnership.getId());
        }

        @Test
        @DisplayName("Should cascade delete check-ins when partnership is deleted")
        void testCascadeDeleteWithPartnership() {
            // Given
            BuddyCheckin checkin1 = createAndSaveCheckin(testUser1Id, CheckInType.DAILY);
            BuddyCheckin checkin2 = createAndSaveCheckin(testUser2Id, CheckInType.WEEKLY);

            List<BuddyCheckin> initialCheckins = checkinRepository.findByPartnershipId(testPartnership.getId());
            assertThat(initialCheckins).hasSize(2);

            // When - First delete related checkins, then delete partnership
            checkinRepository.deleteAll(initialCheckins);
            entityManager.flush();
            partnershipRepository.delete(testPartnership);
            entityManager.flush();

            // Then
            List<BuddyCheckin> remainingCheckins = checkinRepository.findByPartnershipId(testPartnership.getId());
            assertThat(remainingCheckins).isEmpty();
        }

        @Test
        @DisplayName("Should find check-ins by user and partnership")
        void testFindCheckinsByUserAndPartnership() {
            // Given
            createAndSaveCheckin(testUser1Id, CheckInType.DAILY);
            createAndSaveCheckin(testUser1Id, CheckInType.WEEKLY);
            createAndSaveCheckin(testUser2Id, CheckInType.DAILY);

            // When
            List<BuddyCheckin> user1Checkins = checkinRepository.findByPartnershipIdAndUserId(
                testPartnership.getId(), testUser1Id);

            // Then
            assertThat(user1Checkins).hasSize(2);
            assertThat(user1Checkins).allMatch(c -> c.getUserId().equals(testUser1Id));
            assertThat(user1Checkins).allMatch(c -> c.getPartnershipId().equals(testPartnership.getId()));
        }

        @Test
        @DisplayName("Should count check-ins by partnership")
        void testCountCheckinsByPartnership() {
            // Given
            createAndSaveCheckin(testUser1Id, CheckInType.DAILY);
            createAndSaveCheckin(testUser2Id, CheckInType.DAILY);
            createAndSaveCheckin(testUser1Id, CheckInType.WEEKLY);

            // When
            long count = checkinRepository.countByPartnershipId(testPartnership.getId());

            // Then
            assertThat(count).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("Streak Calculation Tests")
    class StreakCalculationTests {

        @Test
        @DisplayName("Should calculate current streak for daily check-ins")
        void testCalculateCurrentStreak() {
            // Given - 5 consecutive days of check-ins
            LocalDateTime baseTime = testTime.minusDays(4);
            for (int i = 0; i < 5; i++) {
                BuddyCheckin checkin = BuddyCheckin.builder()
                        .partnershipId(testPartnership.getId())
                        .userId(testUser1Id)
                        .checkinType(CheckInType.DAILY)
                        .content("Day " + (i + 1) + " check-in")
                        .createdAt(baseTime.plusDays(i))
                        .build();
                checkinRepository.save(checkin);
            }
            entityManager.flush();

            // When
            int currentStreak = checkinRepository.calculateCurrentDailyStreak(
                testPartnership.getId(), testUser1Id, testTime.toLocalDate());

            // Then
            assertThat(currentStreak).isEqualTo(5);
        }

        @Test
        @DisplayName("Should reset streak when a day is missed")
        void testStreakBreaksOnMissedDay() {
            // Given - Check-ins with a gap
            LocalDateTime day1 = testTime.minusDays(4);
            LocalDateTime day2 = testTime.minusDays(3);
            // Skip day 3 (missed check-in)
            LocalDateTime day4 = testTime.minusDays(1);
            LocalDateTime day5 = testTime;

            createCheckinAtTime(testUser1Id, day1);
            createCheckinAtTime(testUser1Id, day2);
            createCheckinAtTime(testUser1Id, day4);
            createCheckinAtTime(testUser1Id, day5);

            // When
            int currentStreak = checkinRepository.calculateCurrentDailyStreak(
                testPartnership.getId(), testUser1Id, testTime.toLocalDate());

            // Then
            assertThat(currentStreak).isEqualTo(2); // Only last 2 consecutive days
        }

        @Test
        @DisplayName("Should calculate weekly check-in streaks")
        void testWeeklyStreakCalculation() {
            // Given - 3 consecutive weeks of check-ins
            LocalDateTime baseTime = testTime.minusWeeks(2);
            for (int i = 0; i < 3; i++) {
                BuddyCheckin checkin = BuddyCheckin.builder()
                        .partnershipId(testPartnership.getId())
                        .userId(testUser1Id)
                        .checkinType(CheckInType.WEEKLY)
                        .content("Week " + (i + 1) + " check-in")
                        .createdAt(baseTime.plusWeeks(i))
                        .build();
                checkinRepository.save(checkin);
            }
            entityManager.flush();

            // When
            int weeklyStreak = checkinRepository.calculateCurrentWeeklyStreak(
                testPartnership.getId(), testUser1Id, testTime.toLocalDate());

            // Then
            assertThat(weeklyStreak).isEqualTo(3);
        }

        @Test
        @DisplayName("Should find longest streak for user")
        void testLongestStreak() {
            // Given - Pattern: 3 days, gap, 5 days, gap, 2 days
            createStreakPattern(testUser1Id, new int[]{3, 0, 5, 0, 2});

            // When
            int longestStreak = checkinRepository.findLongestDailyStreak(
                testPartnership.getId(), testUser1Id);

            // Then
            assertThat(longestStreak).isEqualTo(5);
        }

        @Test
        @DisplayName("Should calculate streak specific to partnership")
        void testStreakByPartnership() {
            // Given - Create another partnership
            BuddyPartnership otherPartnership = dataBuilder.buildBuddyPartnership()
                    .withUser1Id(testUser1Id)
                    .withUser2Id(UUID.randomUUID())
                    .withStatus(PartnershipStatus.ACTIVE)
                    .build();
            entityManager.persistAndFlush(otherPartnership);

            // Create streaks in both partnerships
            createCheckinsForStreak(testPartnership.getId(), testUser1Id, 3);
            createCheckinsForStreak(otherPartnership.getId(), testUser1Id, 5);

            // When
            int streak1 = checkinRepository.calculateCurrentDailyStreak(
                testPartnership.getId(), testUser1Id, testTime.toLocalDate());
            int streak2 = checkinRepository.calculateCurrentDailyStreak(
                otherPartnership.getId(), testUser1Id, testTime.toLocalDate());

            // Then
            assertThat(streak1).isEqualTo(3);
            assertThat(streak2).isEqualTo(5);
        }
    }

    @Nested
    @DisplayName("Time-based Queries")
    class TimeBasedQueries {

        @Test
        @DisplayName("Should find today's check-ins")
        void testFindTodaysCheckins() {
            // Given
            LocalDate today = testTime.toLocalDate();
            createCheckinAtTime(testUser1Id, testTime);
            createCheckinAtTime(testUser2Id, testTime.plusHours(2));
            createCheckinAtTime(testUser1Id, testTime.minusDays(1)); // Yesterday

            // When
            List<BuddyCheckin> todaysCheckins = checkinRepository.findTodaysCheckins(
                testPartnership.getId(), today);

            // Then
            assertThat(todaysCheckins).hasSize(2);
            assertThat(todaysCheckins).allMatch(c ->
                c.getCreatedAt().toLocalDate().equals(today));
        }

        @Test
        @DisplayName("Should find check-ins from last N days")
        void testFindCheckinsLastNDays() {
            // Given
            createCheckinAtTime(testUser1Id, testTime); // Today
            createCheckinAtTime(testUser1Id, testTime.minusDays(1)); // 1 day ago
            createCheckinAtTime(testUser1Id, testTime.minusDays(3)); // 3 days ago
            createCheckinAtTime(testUser1Id, testTime.minusDays(8)); // 8 days ago (outside range)

            // When
            List<BuddyCheckin> recentCheckins = checkinRepository.findCheckinsLastNDays(
                testPartnership.getId(), testUser1Id, 7);

            // Then
            assertThat(recentCheckins).hasSize(3);
            assertThat(recentCheckins).allMatch(c ->
                c.getCreatedAt().isAfter(testTime.minusDays(7)));
        }

        @Test
        @DisplayName("Should find check-ins by date range")
        void testFindCheckinsByDateRange() {
            // Given
            LocalDateTime start = testTime.minusDays(5);
            LocalDateTime end = testTime.minusDays(1);

            createCheckinAtTime(testUser1Id, testTime.minusDays(6)); // Before range
            createCheckinAtTime(testUser1Id, testTime.minusDays(3)); // In range
            createCheckinAtTime(testUser1Id, testTime.minusDays(2)); // In range
            createCheckinAtTime(testUser1Id, testTime); // After range

            // When
            List<BuddyCheckin> rangeCheckins = checkinRepository.findCheckinsByDateRange(
                testPartnership.getId(), start, end);

            // Then
            assertThat(rangeCheckins).hasSize(2);
            assertThat(rangeCheckins).allMatch(c ->
                !c.getCreatedAt().isBefore(start) && !c.getCreatedAt().isAfter(end));
        }

        @Test
        @DisplayName("Should check if user has checked in today")
        void testHasCheckedInToday() {
            // Given
            LocalDate today = testTime.toLocalDate();

            // User1 has checked in today, User2 hasn't
            createCheckinAtTime(testUser1Id, testTime);

            // When
            boolean user1HasCheckedIn = checkinRepository.hasCheckedInToday(
                testPartnership.getId(), testUser1Id, today);
            boolean user2HasCheckedIn = checkinRepository.hasCheckedInToday(
                testPartnership.getId(), testUser2Id, today);

            // Then
            assertThat(user1HasCheckedIn).isTrue();
            assertThat(user2HasCheckedIn).isFalse();
        }

        @Test
        @DisplayName("Should count missed check-in days")
        void testMissedCheckinsCount() {
            // Given - Partnership active for 10 days, user checked in 6 times
            ZonedDateTime startDate = ZonedDateTime.now(testClock).minusDays(9);
            testPartnership.setStartedAt(startDate);
            partnershipRepository.save(testPartnership);

            // Create 6 check-ins over 10 days (4 missed days)
            int[] checkinDays = {1, 2, 4, 6, 8, 9}; // Days when user checked in
            for (int day : checkinDays) {
                createCheckinAtTime(testUser1Id, testTime.minusDays(10 - day));
            }

            // When
            int missedDays = checkinRepository.countMissedCheckinDays(
                testPartnership.getId(), testUser1Id, startDate.toLocalDate(), testTime.toLocalDate());

            // Then
            assertThat(missedDays).isEqualTo(4); // Days 3, 5, 7, 10
        }
    }

    @Nested
    @DisplayName("Mood and Productivity Analysis")
    class MoodAndProductivityAnalysis {

        @Test
        @DisplayName("Should calculate average mood by partnership")
        void testAverageMoodByPartnership() {
            // Given - Mix of moods with known emotional scores
            createCheckinWithMood(testUser1Id, MoodType.EXCITED); // Score: 10
            createCheckinWithMood(testUser1Id, MoodType.FOCUSED); // Score: 8
            createCheckinWithMood(testUser2Id, MoodType.NEUTRAL); // Score: 5
            createCheckinWithMood(testUser2Id, MoodType.STRESSED); // Score: 3

            // When
            Double avgMoodScore = checkinRepository.calculateAverageMoodScore(testPartnership.getId());

            // Then
            Double expectedAvg = (10.0 + 8.0 + 5.0 + 3.0) / 4.0; // 6.5
            assertThat(avgMoodScore).isEqualTo(expectedAvg);
        }

        @Test
        @DisplayName("Should calculate average productivity rating")
        void testAverageProductivityRating() {
            // Given
            createCheckinWithProductivity(testUser1Id, 8);
            createCheckinWithProductivity(testUser1Id, 6);
            createCheckinWithProductivity(testUser2Id, 9);
            createCheckinWithProductivity(testUser2Id, 7);

            // When
            Double avgProductivity = checkinRepository.calculateAverageProductivity(testPartnership.getId());

            // Then
            Double expectedAvg = (8.0 + 6.0 + 9.0 + 7.0) / 4.0; // 7.5
            assertThat(avgProductivity).isEqualTo(expectedAvg);
        }

        @Test
        @DisplayName("Should get mood distribution for partnership")
        void testMoodDistribution() {
            // Given
            createCheckinWithMood(testUser1Id, MoodType.EXCITED);
            createCheckinWithMood(testUser1Id, MoodType.EXCITED);
            createCheckinWithMood(testUser2Id, MoodType.FOCUSED);
            createCheckinWithMood(testUser2Id, MoodType.STRESSED);

            // When
            Map<MoodType, Long> moodDistribution = checkinRepository.getMoodDistribution(testPartnership.getId());

            // Then
            assertThat(moodDistribution.get(MoodType.EXCITED)).isEqualTo(2L);
            assertThat(moodDistribution.get(MoodType.FOCUSED)).isEqualTo(1L);
            assertThat(moodDistribution.get(MoodType.STRESSED)).isEqualTo(1L);
            assertThat(moodDistribution.getOrDefault(MoodType.NEUTRAL, 0L)).isEqualTo(0L);
        }

        @Test
        @DisplayName("Should detect productivity trends")
        void testProductivityTrend() {
            // Given - Increasing productivity over time
            LocalDateTime baseTime = testTime.minusDays(4);
            int[] productivityRatings = {4, 5, 7, 8, 9};

            for (int i = 0; i < productivityRatings.length; i++) {
                BuddyCheckin checkin = BuddyCheckin.builder()
                        .partnershipId(testPartnership.getId())
                        .userId(testUser1Id)
                        .checkinType(CheckInType.DAILY)
                        .productivityRating(productivityRatings[i])
                        .createdAt(baseTime.plusDays(i))
                        .build();
                checkinRepository.save(checkin);
            }

            // When
            String trend = checkinRepository.calculateProductivityTrend(
                testPartnership.getId(), testUser1Id, 5);

            // Then
            assertThat(trend).isEqualTo("INCREASING");
        }

        @Test
        @DisplayName("Should find check-ins with low mood for support detection")
        void testFindLowMoodCheckins() {
            // Given
            createCheckinWithMood(testUser1Id, MoodType.FRUSTRATED); // Low mood
            createCheckinWithMood(testUser1Id, MoodType.STRESSED);   // Low mood
            createCheckinWithMood(testUser2Id, MoodType.EXCITED);    // High mood
            createCheckinWithMood(testUser2Id, MoodType.NEUTRAL);    // Neutral mood

            // When
            List<BuddyCheckin> lowMoodCheckins = checkinRepository.findLowMoodCheckins(
                testPartnership.getId(), 7); // Last 7 days

            // Then
            assertThat(lowMoodCheckins).hasSize(2);
            assertThat(lowMoodCheckins).allMatch(c -> c.getMood().isNegative());
        }
    }

    @Nested
    @DisplayName("Accountability Score Integration")
    class AccountabilityScoreIntegration {

        @Test
        @DisplayName("Should update accountability score when check-in is created")
        void testUpdateAccountabilityScore() {
            // Given - Initial accountability score
            AccountabilityScore score = AccountabilityScore.builder()
                    .userId(testUser1Id)
                    .partnershipId(testPartnership.getId())
                    .score(BigDecimal.valueOf(0.5))
                    .checkinsCompleted(0)
                    .streakDays(0)
                    .build();
            accountabilityRepository.save(score);

            // When - Create check-in
            createAndSaveCheckin(testUser1Id, CheckInType.DAILY);

            // Trigger score update (this would normally be done by service layer)
            checkinRepository.updateAccountabilityScoreForCheckin(testPartnership.getId(), testUser1Id);
            entityManager.flush();
            entityManager.clear();

            // Then
            AccountabilityScore updated = accountabilityRepository.findByUserIdAndPartnershipId(
                testUser1Id, testPartnership.getId()).orElseThrow();

            assertThat(updated.getCheckinsCompleted()).isEqualTo(1);
            assertThat(updated.getScore()).isGreaterThan(BigDecimal.valueOf(0.5));
        }

        @Test
        @DisplayName("Should calculate check-in completion rate")
        void testCheckinCompletionRate() {
            // Given - Partnership active for 10 days, user checked in 7 times
            ZonedDateTime startDate = ZonedDateTime.now(testClock).minusDays(9);
            testPartnership.setStartedAt(startDate);
            partnershipRepository.save(testPartnership);

            // Create 7 check-ins over 10 days
            for (int i = 0; i < 7; i++) {
                createCheckinAtTime(testUser1Id, testTime.minusDays(i));
            }

            // When
            Double completionRate = checkinRepository.calculateCheckinCompletionRate(
                testPartnership.getId(), testUser1Id);

            // Then
            assertThat(completionRate).isEqualTo(0.7); // 7/10 = 70%
        }

        @Test
        @DisplayName("Should track response time for check-ins")
        void testResponseTimeTracking() {
            // Given - Check-ins at different times of day
            LocalDateTime morning = testTime.withHour(8);
            LocalDateTime afternoon = testTime.withHour(14);
            LocalDateTime evening = testTime.withHour(20);

            createCheckinAtTime(testUser1Id, morning);
            createCheckinAtTime(testUser1Id, afternoon);
            createCheckinAtTime(testUser1Id, evening);

            // When
            Double avgResponseHour = checkinRepository.calculateAverageResponseTime(
                testPartnership.getId(), testUser1Id);

            // Then
            Double expectedAvg = (8.0 + 14.0 + 20.0) / 3.0; // 14.0 (2 PM average)
            assertThat(avgResponseHour).isEqualTo(expectedAvg);
        }

        @Test
        @DisplayName("Should calculate partnership health impact from check-ins")
        void testPartnershipHealthImpact() {
            // Given - Both users checking in regularly
            createCheckinsForHealthCalculation();

            // When
            BigDecimal healthImpact = checkinRepository.calculatePartnershipHealthFromCheckins(
                testPartnership.getId(), 7); // Last 7 days

            // Then
            assertThat(healthImpact).isBetween(BigDecimal.valueOf(0.7), BigDecimal.valueOf(1.0));
        }

        @Test
        @DisplayName("Should apply streak bonus to accountability score")
        void testStreakBonusPoints() {
            // Given - User with 5-day streak
            createCheckinsForStreak(testPartnership.getId(), testUser1Id, 5);

            // When
            BigDecimal streakBonus = checkinRepository.calculateStreakBonus(
                testPartnership.getId(), testUser1Id);

            // Then
            assertThat(streakBonus).isGreaterThan(BigDecimal.ZERO);
            // Bonus should increase with streak length
            assertThat(streakBonus).isGreaterThan(BigDecimal.valueOf(0.1));
        }
    }

    @Nested
    @DisplayName("Performance and Statistics")
    class PerformanceAndStatistics {

        @Test
        @DisplayName("Should handle bulk check-in operations efficiently")
        void testBulkCheckinOperations() {
            // Given - Large number of check-ins
            List<BuddyCheckin> bulkCheckins = createBulkCheckinsForTest(100);

            // When
            List<BuddyCheckin> saved = checkinRepository.saveAll(bulkCheckins);
            entityManager.flush();

            // Then
            assertThat(saved).hasSize(100);
            assertThat(checkinRepository.countByPartnershipId(testPartnership.getId())).isEqualTo(100);
        }

        @Test
        @DisplayName("Should support paginated check-in queries")
        void testCheckinPagination() {
            // Given - Multiple check-ins
            List<BuddyCheckin> bulkCheckins = createBulkCheckinsForTest(25);
            checkinRepository.saveAll(bulkCheckins);
            entityManager.flush();

            // When
            Pageable pageable = PageRequest.of(0, 10, Sort.by("createdAt").descending());
            Page<BuddyCheckin> page = checkinRepository.findByPartnershipId(
                testPartnership.getId(), pageable);

            // Then
            assertThat(page.getContent()).hasSize(10);
            assertThat(page.getTotalElements()).isEqualTo(25);
            assertThat(page.getTotalPages()).isEqualTo(3);

            // Verify sorting (most recent first)
            List<BuddyCheckin> content = page.getContent();
            for (int i = 1; i < content.size(); i++) {
                assertThat(content.get(i-1).getCreatedAt())
                    .isAfterOrEqualTo(content.get(i).getCreatedAt());
            }
        }

        @Test
        @DisplayName("Should handle concurrent check-ins safely")
        void testConcurrentCheckins() {
            // Given - Simulate concurrent check-ins
            BuddyCheckin checkin1 = BuddyCheckin.builder()
                    .partnershipId(testPartnership.getId())
                    .userId(testUser1Id)
                    .checkinType(CheckInType.DAILY)
                    .content("Concurrent checkin 1")
                    .createdAt(testTime)
                    .build();

            BuddyCheckin checkin2 = BuddyCheckin.builder()
                    .partnershipId(testPartnership.getId())
                    .userId(testUser2Id)
                    .checkinType(CheckInType.DAILY)
                    .content("Concurrent checkin 2")
                    .createdAt(testTime)
                    .build();

            // When - Save concurrently (simulated)
            BuddyCheckin saved1 = checkinRepository.save(checkin1);
            BuddyCheckin saved2 = checkinRepository.save(checkin2);
            entityManager.flush();

            // Then
            assertThat(saved1.getId()).isNotEqualTo(saved2.getId());
            assertThat(checkinRepository.countByPartnershipId(testPartnership.getId())).isEqualTo(2);
        }

        @Test
        @DisplayName("Should aggregate check-in statistics by month")
        void testCheckinStatsByMonth() {
            // Given - Check-ins across different months
            createCheckinAtTime(testUser1Id, testTime.withMonth(1).withDayOfMonth(15)); // January
            createCheckinAtTime(testUser1Id, testTime.withMonth(1).withDayOfMonth(20)); // January
            createCheckinAtTime(testUser2Id, testTime.withMonth(2).withDayOfMonth(10)); // February
            createCheckinAtTime(testUser2Id, testTime.withMonth(3).withDayOfMonth(5));  // March

            // When
            Map<String, Long> monthlyStats = checkinRepository.getCheckinStatsByMonth(
                testPartnership.getId(), testTime.getYear());

            // Then
            assertThat(monthlyStats.get("2025-01")).isEqualTo(2L);
            assertThat(monthlyStats.get("2025-02")).isEqualTo(1L);
            assertThat(monthlyStats.get("2025-03")).isEqualTo(1L);
        }

        @Test
        @DisplayName("Should analyze check-in frequency patterns")
        void testCheckinFrequencyAnalysis() {
            // Given - Pattern: 3 consecutive days, 2 days break, 4 consecutive days
            LocalDateTime baseTime = testTime.minusDays(8);
            int[] pattern = {1, 1, 1, 0, 0, 1, 1, 1, 1}; // 1 = checkin, 0 = no checkin

            for (int i = 0; i < pattern.length; i++) {
                if (pattern[i] == 1) {
                    createCheckinAtTime(testUser1Id, baseTime.plusDays(i));
                }
            }

            // When
            Map<String, Object> frequencyAnalysis = checkinRepository.analyzeCheckinFrequency(
                testPartnership.getId(), testUser1Id, 9);

            // Then
            assertThat(frequencyAnalysis.get("totalCheckins")).isEqualTo(7L);
            assertThat(frequencyAnalysis.get("longestStreak")).isEqualTo(4);
            assertThat(frequencyAnalysis.get("averageGapDays")).isEqualTo(0.4); // (2 gaps of 2 days + 5 gaps of 0 days) / 7 gaps
        }
    }

    // Helper methods for test data creation

    private BuddyCheckin createAndSaveCheckin(UUID userId, CheckInType type) {
        BuddyCheckin checkin = BuddyCheckin.builder()
                .partnershipId(testPartnership.getId())
                .userId(userId)
                .checkinType(type)
                .content("Test check-in content")
                .mood(MoodType.NEUTRAL)
                .productivityRating(5)
                .createdAt(testTime)
                .build();

        return checkinRepository.save(checkin);
    }

    private BuddyCheckin createCheckinAtTime(UUID userId, LocalDateTime createdAt) {
        BuddyCheckin checkin = BuddyCheckin.builder()
                .partnershipId(testPartnership.getId())
                .userId(userId)
                .checkinType(CheckInType.DAILY)
                .content("Check-in at " + createdAt)
                .mood(MoodType.NEUTRAL)
                .productivityRating(5)
                .createdAt(createdAt)
                .build();

        return checkinRepository.save(checkin);
    }

    private BuddyCheckin createCheckinWithMood(UUID userId, MoodType mood) {
        BuddyCheckin checkin = BuddyCheckin.builder()
                .partnershipId(testPartnership.getId())
                .userId(userId)
                .checkinType(CheckInType.DAILY)
                .content("Check-in with mood: " + mood.getDisplayName())
                .mood(mood)
                .productivityRating(5)
                .createdAt(testTime)
                .build();

        return checkinRepository.save(checkin);
    }

    private BuddyCheckin createCheckinWithProductivity(UUID userId, Integer rating) {
        BuddyCheckin checkin = BuddyCheckin.builder()
                .partnershipId(testPartnership.getId())
                .userId(userId)
                .checkinType(CheckInType.DAILY)
                .content("Check-in with productivity: " + rating)
                .mood(MoodType.NEUTRAL)
                .productivityRating(rating)
                .createdAt(testTime)
                .build();

        return checkinRepository.save(checkin);
    }

    private void createStreakPattern(UUID userId, int[] pattern) {
        LocalDateTime baseTime = testTime.minusDays(pattern.length - 1);
        int dayOffset = 0;

        for (int streakLength : pattern) {
            for (int i = 0; i < streakLength; i++) {
                createCheckinAtTime(userId, baseTime.plusDays(dayOffset + i));
            }
            dayOffset += streakLength + 1; // +1 for the gap day
        }
    }

    private void createCheckinsForStreak(UUID partnershipId, UUID userId, int streakDays) {
        LocalDateTime baseTime = testTime.minusDays(streakDays - 1);
        for (int i = 0; i < streakDays; i++) {
            BuddyCheckin checkin = BuddyCheckin.builder()
                    .partnershipId(partnershipId)
                    .userId(userId)
                    .checkinType(CheckInType.DAILY)
                    .content("Streak day " + (i + 1))
                    .createdAt(baseTime.plusDays(i))
                    .build();
            checkinRepository.save(checkin);
        }
    }

    private void createCheckinsForHealthCalculation() {
        // Create realistic check-in pattern for health calculation
        LocalDateTime baseTime = testTime.minusDays(6);

        // User 1: 5 out of 7 days
        for (int i = 0; i < 7; i++) {
            if (i != 2 && i != 4) { // Skip day 2 and 4
                createCheckinAtTime(testUser1Id, baseTime.plusDays(i));
            }
        }

        // User 2: 6 out of 7 days
        for (int i = 0; i < 7; i++) {
            if (i != 3) { // Skip day 3
                createCheckinAtTime(testUser2Id, baseTime.plusDays(i));
            }
        }
    }

    private List<BuddyCheckin> createBulkCheckinsForTest(int count) {
        List<BuddyCheckin> checkins = new ArrayList<>();
        LocalDateTime baseTime = testTime.minusDays(count - 1);

        for (int i = 0; i < count; i++) {
            BuddyCheckin checkin = BuddyCheckin.builder()
                    .partnershipId(testPartnership.getId())
                    .userId(i % 2 == 0 ? testUser1Id : testUser2Id)
                    .checkinType(CheckInType.DAILY)
                    .content("Bulk check-in " + i)
                    .mood(MoodType.NEUTRAL)
                    .productivityRating(5)
                    .createdAt(baseTime.plusDays(i))
                    .build();
            checkins.add(checkin);
        }

        return checkins;
    }
}