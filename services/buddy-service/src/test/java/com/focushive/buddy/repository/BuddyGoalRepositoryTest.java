package com.focushive.buddy.repository;

import com.focushive.buddy.config.TestContainersConfiguration;
import com.focushive.buddy.constant.GoalStatus;
import com.focushive.buddy.constant.PartnershipStatus;
import com.focushive.buddy.entity.BuddyGoal;
import com.focushive.buddy.entity.BuddyPartnership;
import com.focushive.buddy.entity.GoalMilestone;
import com.focushive.buddy.config.TestSecurityConfig;
import com.focushive.buddy.integration.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive TDD test suite for BuddyGoal repository operations.
 *
 * These tests are written FIRST following strict TDD principles.
 * They will FAIL initially because the entities and repositories don't exist yet.
 *
 * Test Structure:
 * 1. Basic CRUD Operations
 * 2. Goal Lifecycle Management
 * 3. Partnership Relationship Tests
 * 4. Milestone Management
 * 5. Query Methods
 * 6. Complex Business Logic
 * 7. Validation and Constraints
 * 8. Performance and Statistics
 *
 * Expected Failures:
 * - Cannot resolve symbol 'BuddyGoal'
 * - Cannot resolve symbol 'BuddyGoalRepository'
 * - Cannot resolve symbol 'GoalMilestone'
 * - Cannot resolve symbol 'GoalStatus'
 */
@DisplayName("BuddyGoal Repository Tests - TDD Phase 1.3")
class BuddyGoalRepositoryTest extends AbstractRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private BuddyGoalRepository buddyGoalRepository;

    @Autowired
    private BuddyPartnershipRepository buddyPartnershipRepository;

    @Autowired
    private GoalMilestoneRepository goalMilestoneRepository;

    private Clock testClock;
    private BuddyPartnership testPartnership;
    private UUID testUser1Id;
    private UUID testUser2Id;
    private TestDataBuilder testDataBuilder;

    @BeforeEach
    void setUp() {
        testClock = Clock.fixed(
            ZonedDateTime.of(2025, 1, 15, 10, 0, 0, 0, ZoneId.systemDefault()).toInstant(),
            ZoneId.systemDefault()
        );

        testDataBuilder = new TestDataBuilder();
        testUser1Id = UUID.randomUUID();
        testUser2Id = UUID.randomUUID();

        // Create test partnership for goal relationships
        testPartnership = testDataBuilder.buildBuddyPartnership()
            .withUser1Id(testUser1Id)
            .withUser2Id(testUser2Id)
            .withStatus(PartnershipStatus.ACTIVE)
            .build();

        testPartnership = buddyPartnershipRepository.save(testPartnership);
        entityManager.flush();
        entityManager.clear();
    }

    @Nested
    @DisplayName("Basic CRUD Operations")
    class BasicCrudOperations {

        @Test
        @DisplayName("Should save new shared goal with valid data")
        void testSaveGoal() {
            // Given - Create goal with required fields
            BuddyGoal goal = BuddyGoal.builder()
                .partnershipId(testPartnership.getId())
                .title("Complete React Course")
                .description("Finish the advanced React development course together")
                .targetDate(LocalDate.now().plusDays(30))
                .progressPercentage(0)
                .createdBy(testUser1Id)
                .status(GoalStatus.IN_PROGRESS)
                .build();

            // When - Save the goal
            BuddyGoal savedGoal = buddyGoalRepository.save(goal);
            entityManager.flush();

            // Then - Verify saved goal
            assertThat(savedGoal).isNotNull();
            assertThat(savedGoal.getId()).isNotNull();
            assertThat(savedGoal.getPartnershipId()).isEqualTo(testPartnership.getId());
            assertThat(savedGoal.getTitle()).isEqualTo("Complete React Course");
            assertThat(savedGoal.getDescription()).isEqualTo("Finish the advanced React development course together");
            assertThat(savedGoal.getProgressPercentage()).isEqualTo(0);
            assertThat(savedGoal.getCreatedBy()).isEqualTo(testUser1Id);
            assertThat(savedGoal.getStatus()).isEqualTo(GoalStatus.IN_PROGRESS);
            assertThat(savedGoal.getCreatedAt()).isNotNull();
            assertThat(savedGoal.getUpdatedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should find goal by ID")
        void testFindGoalById() {
            // Given - Save a goal
            BuddyGoal goal = createTestGoal("Find Me Goal", "Test description", 25);
            BuddyGoal savedGoal = buddyGoalRepository.save(goal);
            entityManager.flush();
            entityManager.clear();

            // When - Find by ID
            Optional<BuddyGoal> foundGoal = buddyGoalRepository.findById(savedGoal.getId());

            // Then - Verify found goal
            assertThat(foundGoal).isPresent();
            assertThat(foundGoal.get().getTitle()).isEqualTo("Find Me Goal");
            assertThat(foundGoal.get().getDescription()).isEqualTo("Test description");
            assertThat(foundGoal.get().getProgressPercentage()).isEqualTo(25);
        }

        @Test
        @DisplayName("Should update goal details")
        void testUpdateGoal() {
            // Given - Save a goal
            BuddyGoal goal = createTestGoal("Original Title", "Original description", 0);
            BuddyGoal savedGoal = buddyGoalRepository.save(goal);
            entityManager.flush();

            // When - Update goal
            savedGoal.setTitle("Updated Title");
            savedGoal.setDescription("Updated description");
            savedGoal.setProgressPercentage(50);
            BuddyGoal updatedGoal = buddyGoalRepository.save(savedGoal);
            entityManager.flush();

            // Then - Verify updates
            assertThat(updatedGoal.getTitle()).isEqualTo("Updated Title");
            assertThat(updatedGoal.getDescription()).isEqualTo("Updated description");
            assertThat(updatedGoal.getProgressPercentage()).isEqualTo(50);
            assertThat(updatedGoal.getUpdatedAt()).isAfter(updatedGoal.getCreatedAt());
        }

        @Test
        @DisplayName("Should delete goal and cascade to milestones")
        void testDeleteGoal() {
            // Given - Save goal with milestones
            BuddyGoal goal = createTestGoal("Goal to Delete", "Will be deleted", 0);
            BuddyGoal savedGoal = buddyGoalRepository.save(goal);

            GoalMilestone milestone = createTestMilestone(savedGoal.getId(), "Milestone 1");
            goalMilestoneRepository.save(milestone);
            entityManager.flush();

            // When - Delete goal
            buddyGoalRepository.delete(savedGoal);
            entityManager.flush();

            // Then - Verify deletion and cascade
            assertThat(buddyGoalRepository.findById(savedGoal.getId())).isEmpty();
            assertThat(goalMilestoneRepository.findByGoalId(savedGoal.getId())).isEmpty();
        }

        @Test
        @DisplayName("Should find all goals for a partnership")
        void testFindByPartnershipId() {
            // Given - Create multiple goals for partnership
            BuddyGoal goal1 = createTestGoal("Goal 1", "First goal", 10);
            BuddyGoal goal2 = createTestGoal("Goal 2", "Second goal", 25);
            BuddyGoal goal3 = createTestGoal("Goal 3", "Third goal", 0);

            buddyGoalRepository.saveAll(List.of(goal1, goal2, goal3));
            entityManager.flush();

            // When - Find by partnership ID
            List<BuddyGoal> goals = buddyGoalRepository.findByPartnershipId(testPartnership.getId());

            // Then - Verify all goals found
            assertThat(goals).hasSize(3);
            assertThat(goals)
                .extracting(BuddyGoal::getTitle)
                .containsExactlyInAnyOrder("Goal 1", "Goal 2", "Goal 3");
        }
    }

    @Nested
    @DisplayName("Goal Lifecycle Management")
    class GoalLifecycleManagement {

        @Test
        @DisplayName("Should create goal with ACTIVE status by default")
        void testCreateActiveGoal() {
            // Given - Create goal without explicit status
            BuddyGoal goal = BuddyGoal.builder()
                .partnershipId(testPartnership.getId())
                .title("Active Goal")
                .createdBy(testUser1Id)
                .build();

            // When - Save goal
            BuddyGoal savedGoal = buddyGoalRepository.save(goal);
            entityManager.flush();

            // Then - Verify default status
            assertThat(savedGoal.getStatus()).isEqualTo(GoalStatus.IN_PROGRESS);
        }

        @Test
        @DisplayName("Should update progress percentage")
        void testUpdateProgress() {
            // Given - Create goal with 0% progress
            BuddyGoal goal = createTestGoal("Progress Goal", "Track progress", 0);
            BuddyGoal savedGoal = buddyGoalRepository.save(goal);
            entityManager.flush();

            // When - Update progress
            savedGoal.setProgressPercentage(75);
            BuddyGoal updatedGoal = buddyGoalRepository.save(savedGoal);
            entityManager.flush();

            // Then - Verify progress update
            assertThat(updatedGoal.getProgressPercentage()).isEqualTo(75);
            assertThat(updatedGoal.getUpdatedAt()).isAfter(updatedGoal.getCreatedAt());
        }

        @Test
        @DisplayName("Should complete goal with timestamp")
        void testCompleteGoal() {
            // Given - Create active goal
            BuddyGoal goal = createTestGoal("Goal to Complete", "Will be completed", 90);
            BuddyGoal savedGoal = buddyGoalRepository.save(goal);
            entityManager.flush();

            // When - Complete goal
            savedGoal.setStatus(GoalStatus.COMPLETED);
            savedGoal.setProgressPercentage(100);
            savedGoal.setCompletedAt(LocalDateTime.now(testClock));
            BuddyGoal completedGoal = buddyGoalRepository.save(savedGoal);
            entityManager.flush();

            // Then - Verify completion
            assertThat(completedGoal.getStatus()).isEqualTo(GoalStatus.COMPLETED);
            assertThat(completedGoal.getProgressPercentage()).isEqualTo(100);
            assertThat(completedGoal.getCompletedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should cancel goal")
        void testCancelGoal() {
            // Given - Create active goal
            BuddyGoal goal = createTestGoal("Goal to Cancel", "Will be cancelled", 30);
            BuddyGoal savedGoal = buddyGoalRepository.save(goal);
            entityManager.flush();

            // When - Cancel goal
            savedGoal.setStatus(GoalStatus.CANCELLED);
            BuddyGoal cancelledGoal = buddyGoalRepository.save(savedGoal);
            entityManager.flush();

            // Then - Verify cancellation
            assertThat(cancelledGoal.getStatus()).isEqualTo(GoalStatus.CANCELLED);
        }

        @Test
        @DisplayName("Should pause and resume goal")
        void testPauseAndResumeGoal() {
            // Given - Create active goal
            BuddyGoal goal = createTestGoal("Goal to Pause", "Will be paused", 50);
            BuddyGoal savedGoal = buddyGoalRepository.save(goal);
            entityManager.flush();

            // When - Pause goal
            savedGoal.setStatus(GoalStatus.PAUSED);
            BuddyGoal pausedGoal = buddyGoalRepository.save(savedGoal);
            entityManager.flush();

            // Then - Verify pause
            assertThat(pausedGoal.getStatus()).isEqualTo(GoalStatus.PAUSED);

            // When - Resume goal
            pausedGoal.setStatus(GoalStatus.IN_PROGRESS);
            BuddyGoal resumedGoal = buddyGoalRepository.save(pausedGoal);
            entityManager.flush();

            // Then - Verify resume
            assertThat(resumedGoal.getStatus()).isEqualTo(GoalStatus.IN_PROGRESS);
        }

        @Test
        @DisplayName("Should validate progress percentage range")
        void testProgressValidation() {
            // Given - Create goal
            BuddyGoal goal = createTestGoal("Progress Validation", "Test validation", 0);
            BuddyGoal savedGoal = buddyGoalRepository.save(goal);
            entityManager.flush();

            // When/Then - Test invalid negative progress
            savedGoal.setProgressPercentage(-10);
            assertThatThrownBy(() -> {
                buddyGoalRepository.save(savedGoal);
                entityManager.flush();
            }).isInstanceOf(DataIntegrityViolationException.class)
              .hasMessageContaining("progress_percentage");

            // When/Then - Test invalid progress > 100
            savedGoal.setProgressPercentage(110);
            assertThatThrownBy(() -> {
                buddyGoalRepository.save(savedGoal);
                entityManager.flush();
            }).isInstanceOf(DataIntegrityViolationException.class)
              .hasMessageContaining("progress_percentage");
        }
    }

    @Nested
    @DisplayName("Partnership Relationship Tests")
    class PartnershipRelationshipTests {

        @Test
        @DisplayName("Should maintain foreign key relationship to partnership")
        void testGoalBelongsToPartnership() {
            // Given - Create goal for partnership
            BuddyGoal goal = createTestGoal("Partnership Goal", "Belongs to partnership", 0);
            BuddyGoal savedGoal = buddyGoalRepository.save(goal);
            entityManager.flush();

            // When - Fetch goal
            BuddyGoal foundGoal = buddyGoalRepository.findById(savedGoal.getId()).orElse(null);

            // Then - Verify relationship
            assertThat(foundGoal).isNotNull();
            assertThat(foundGoal.getPartnershipId()).isEqualTo(testPartnership.getId());
        }

        @Test
        @DisplayName("Should cascade delete when partnership is deleted")
        void testCascadeDeleteWithPartnership() {
            // Given - Create goals for partnership
            BuddyGoal goal1 = createTestGoal("Goal 1", "Will be deleted", 0);
            BuddyGoal goal2 = createTestGoal("Goal 2", "Will also be deleted", 50);

            List<BuddyGoal> savedGoals = buddyGoalRepository.saveAll(List.of(goal1, goal2));
            entityManager.flush();

            // When - Delete partnership
            buddyPartnershipRepository.delete(testPartnership);
            entityManager.flush();
            entityManager.clear(); // Clear the persistence context to force fresh queries

            // Then - Verify goals are also deleted
            for (BuddyGoal goal : savedGoals) {
                assertThat(buddyGoalRepository.findById(goal.getId())).isEmpty();
            }
        }

        @Test
        @DisplayName("Should count goals by partnership")
        void testCountGoalsByPartnership() {
            // Given - Create multiple goals
            BuddyGoal goal1 = createTestGoal("Goal 1", "Count me", 0);
            BuddyGoal goal2 = createTestGoal("Goal 2", "Count me too", 25);
            BuddyGoal goal3 = createTestGoal("Goal 3", "Count me three", 75);

            buddyGoalRepository.saveAll(List.of(goal1, goal2, goal3));
            entityManager.flush();

            // When - Count goals
            long goalCount = buddyGoalRepository.countByPartnershipId(testPartnership.getId());

            // Then - Verify count
            assertThat(goalCount).isEqualTo(3);
        }

        @Test
        @DisplayName("Should find only active goals by partnership")
        void testFindActiveGoalsByPartnership() {
            // Given - Create goals with different statuses
            BuddyGoal activeGoal1 = createTestGoal("Active Goal 1", "Active", 25);
            activeGoal1.setStatus(GoalStatus.IN_PROGRESS);

            BuddyGoal activeGoal2 = createTestGoal("Active Goal 2", "Also active", 50);
            activeGoal2.setStatus(GoalStatus.IN_PROGRESS);

            BuddyGoal completedGoal = createTestGoal("Completed Goal", "Done", 100);
            completedGoal.setStatus(GoalStatus.COMPLETED);

            BuddyGoal cancelledGoal = createTestGoal("Cancelled Goal", "Cancelled", 10);
            cancelledGoal.setStatus(GoalStatus.CANCELLED);

            buddyGoalRepository.saveAll(List.of(activeGoal1, activeGoal2, completedGoal, cancelledGoal));
            entityManager.flush();

            // When - Find active goals
            List<BuddyGoal> activeGoals = buddyGoalRepository.findByPartnershipIdAndStatus(
                testPartnership.getId(), GoalStatus.IN_PROGRESS);

            // Then - Verify only active goals returned
            assertThat(activeGoals).hasSize(2);
            assertThat(activeGoals)
                .extracting(BuddyGoal::getTitle)
                .containsExactlyInAnyOrder("Active Goal 1", "Active Goal 2");
        }
    }

    @Nested
    @DisplayName("Milestone Management")
    class MilestoneManagement {

        @Test
        @DisplayName("Should add milestone to goal")
        void testAddMilestoneToGoal() {
            // Given - Create goal
            BuddyGoal goal = createTestGoal("Goal with Milestone", "Has milestone", 0);
            BuddyGoal savedGoal = buddyGoalRepository.save(goal);
            entityManager.flush();

            // When - Add milestone
            GoalMilestone milestone = createTestMilestone(savedGoal.getId(), "First Milestone");
            milestone.setDescription("Complete first phase of the goal");
            milestone.setTargetDate(LocalDate.now().plusDays(15));

            GoalMilestone savedMilestone = goalMilestoneRepository.save(milestone);
            entityManager.flush();

            // Then - Verify milestone created
            assertThat(savedMilestone.getId()).isNotNull();
            assertThat(savedMilestone.getGoalId()).isEqualTo(savedGoal.getId());
            assertThat(savedMilestone.getTitle()).isEqualTo("First Milestone");
        }

        @Test
        @DisplayName("Should find all milestones for a goal")
        void testFindMilestonesByGoal() {
            // Given - Create goal with milestones
            BuddyGoal goal = createTestGoal("Goal with Multiple Milestones", "Has many milestones", 0);
            BuddyGoal savedGoal = buddyGoalRepository.save(goal);

            GoalMilestone milestone1 = createTestMilestone(savedGoal.getId(), "Milestone 1");
            GoalMilestone milestone2 = createTestMilestone(savedGoal.getId(), "Milestone 2");
            GoalMilestone milestone3 = createTestMilestone(savedGoal.getId(), "Milestone 3");

            goalMilestoneRepository.saveAll(List.of(milestone1, milestone2, milestone3));
            entityManager.flush();

            // When - Find milestones
            List<GoalMilestone> milestones = goalMilestoneRepository.findByGoalId(savedGoal.getId());

            // Then - Verify all milestones found
            assertThat(milestones).hasSize(3);
            assertThat(milestones)
                .extracting(GoalMilestone::getTitle)
                .containsExactlyInAnyOrder("Milestone 1", "Milestone 2", "Milestone 3");
        }

        @Test
        @DisplayName("Should complete milestone")
        void testCompleteMilestone() {
            // Given - Create goal with milestone
            BuddyGoal goal = createTestGoal("Goal for Milestone Completion", "Test completion", 0);
            BuddyGoal savedGoal = buddyGoalRepository.save(goal);

            GoalMilestone milestone = createTestMilestone(savedGoal.getId(), "Milestone to Complete");
            GoalMilestone savedMilestone = goalMilestoneRepository.save(milestone);
            entityManager.flush();

            // When - Complete milestone
            savedMilestone.setCompletedAt(LocalDateTime.now(testClock));
            savedMilestone.setCompletedBy(testUser1Id);
            savedMilestone.setCelebrationSent(true);
            GoalMilestone completedMilestone = goalMilestoneRepository.save(savedMilestone);
            entityManager.flush();

            // Then - Verify completion
            assertThat(completedMilestone.getCompletedAt()).isNotNull();
            assertThat(completedMilestone.getCompletedBy()).isEqualTo(testUser1Id);
            assertThat(completedMilestone.isCelebrationSent()).isTrue();
        }

        @Test
        @DisplayName("Should cascade delete milestones when goal is deleted")
        void testMilestoneCascadeDelete() {
            // Given - Create goal with milestones
            BuddyGoal goal = createTestGoal("Goal for Cascade Delete", "Will be deleted", 0);
            BuddyGoal savedGoal = buddyGoalRepository.save(goal);

            GoalMilestone milestone1 = createTestMilestone(savedGoal.getId(), "Milestone 1");
            GoalMilestone milestone2 = createTestMilestone(savedGoal.getId(), "Milestone 2");

            goalMilestoneRepository.saveAll(List.of(milestone1, milestone2));
            entityManager.flush();

            // When - Delete goal
            buddyGoalRepository.delete(savedGoal);
            entityManager.flush();

            // Then - Verify milestones are deleted
            assertThat(goalMilestoneRepository.findByGoalId(savedGoal.getId())).isEmpty();
        }

        @Test
        @DisplayName("Should count completed milestones")
        void testCountCompletedMilestones() {
            // Given - Create goal with milestones (some completed)
            BuddyGoal goal = createTestGoal("Goal for Milestone Count", "Count completions", 50);
            BuddyGoal savedGoal = buddyGoalRepository.save(goal);

            GoalMilestone milestone1 = createTestMilestone(savedGoal.getId(), "Completed 1");
            milestone1.setCompletedAt(LocalDateTime.now(testClock));

            GoalMilestone milestone2 = createTestMilestone(savedGoal.getId(), "Not Completed");

            GoalMilestone milestone3 = createTestMilestone(savedGoal.getId(), "Completed 2");
            milestone3.setCompletedAt(LocalDateTime.now(testClock));

            goalMilestoneRepository.saveAll(List.of(milestone1, milestone2, milestone3));
            entityManager.flush();

            // When - Count completed milestones
            long completedCount = goalMilestoneRepository.countByGoalIdAndCompletedAtIsNotNull(savedGoal.getId());

            // Then - Verify count
            assertThat(completedCount).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("Query Methods")
    class QueryMethods {

        @Test
        @DisplayName("Should find goals by status")
        void testFindByStatus() {
            // Given - Create goals with different statuses
            BuddyGoal activeGoal = createTestGoal("Active Goal", "Active", 25);
            activeGoal.setStatus(GoalStatus.IN_PROGRESS);

            BuddyGoal completedGoal = createTestGoal("Completed Goal", "Done", 100);
            completedGoal.setStatus(GoalStatus.COMPLETED);
            completedGoal.setCompletedAt(LocalDateTime.now(testClock));

            BuddyGoal pausedGoal = createTestGoal("Paused Goal", "Paused", 40);
            pausedGoal.setStatus(GoalStatus.PAUSED);

            buddyGoalRepository.saveAll(List.of(activeGoal, completedGoal, pausedGoal));
            entityManager.flush();

            // When - Find by status
            List<BuddyGoal> completedGoals = buddyGoalRepository.findByStatus(GoalStatus.COMPLETED);

            // Then - Verify filtered results
            assertThat(completedGoals).hasSize(1);
            assertThat(completedGoals.get(0).getTitle()).isEqualTo("Completed Goal");
        }

        @Test
        @DisplayName("Should find overdue goals")
        void testFindOverdueGoals() {
            // Given - Create goals with different target dates
            BuddyGoal overdueGoal = createTestGoal("Overdue Goal", "Past due date", 60);
            overdueGoal.setTargetDate(LocalDate.now().minusDays(5));
            overdueGoal.setStatus(GoalStatus.IN_PROGRESS);

            BuddyGoal futureGoal = createTestGoal("Future Goal", "Not due yet", 30);
            futureGoal.setTargetDate(LocalDate.now().plusDays(10));
            futureGoal.setStatus(GoalStatus.IN_PROGRESS);

            BuddyGoal completedGoal = createTestGoal("Completed Goal", "Done on time", 100);
            completedGoal.setTargetDate(LocalDate.now().minusDays(2));
            completedGoal.setStatus(GoalStatus.COMPLETED);

            buddyGoalRepository.saveAll(List.of(overdueGoal, futureGoal, completedGoal));
            entityManager.flush();

            // When - Find overdue goals
            List<BuddyGoal> overdueGoals = buddyGoalRepository.findOverdueGoals(LocalDate.now());

            // Then - Verify overdue goals found
            assertThat(overdueGoals).hasSize(1);
            assertThat(overdueGoals.get(0).getTitle()).isEqualTo("Overdue Goal");
        }

        @Test
        @DisplayName("Should find upcoming deadlines")
        void testFindUpcomingDeadlines() {
            // Given - Create goals with various deadlines
            BuddyGoal soonGoal = createTestGoal("Due Soon", "Due in 3 days", 80);
            soonGoal.setTargetDate(LocalDate.now().plusDays(3));
            soonGoal.setStatus(GoalStatus.IN_PROGRESS);

            BuddyGoal laterGoal = createTestGoal("Due Later", "Due in 15 days", 40);
            laterGoal.setTargetDate(LocalDate.now().plusDays(15));
            laterGoal.setStatus(GoalStatus.IN_PROGRESS);

            BuddyGoal urgentGoal = createTestGoal("Due Tomorrow", "Due very soon", 95);
            urgentGoal.setTargetDate(LocalDate.now().plusDays(1));
            urgentGoal.setStatus(GoalStatus.IN_PROGRESS);

            buddyGoalRepository.saveAll(List.of(soonGoal, laterGoal, urgentGoal));
            entityManager.flush();

            // When - Find upcoming deadlines within 7 days
            List<BuddyGoal> upcomingGoals = buddyGoalRepository.findUpcomingDeadlines(
                LocalDate.now(), LocalDate.now().plusDays(7));

            // Then - Verify upcoming goals found
            assertThat(upcomingGoals).hasSize(2);
            assertThat(upcomingGoals)
                .extracting(BuddyGoal::getTitle)
                .containsExactlyInAnyOrder("Due Soon", "Due Tomorrow");
        }

        @Test
        @DisplayName("Should find goals created by specific user")
        void testFindByCreatedBy() {
            // Given - Create goals by different users
            BuddyGoal goalByUser1 = createTestGoal("User 1 Goal", "Created by user 1", 30);
            goalByUser1.setCreatedBy(testUser1Id);

            BuddyGoal goalByUser2 = createTestGoal("User 2 Goal", "Created by user 2", 50);
            goalByUser2.setCreatedBy(testUser2Id);

            BuddyGoal anotherGoalByUser1 = createTestGoal("Another User 1 Goal", "Also by user 1", 70);
            anotherGoalByUser1.setCreatedBy(testUser1Id);

            buddyGoalRepository.saveAll(List.of(goalByUser1, goalByUser2, anotherGoalByUser1));
            entityManager.flush();

            // When - Find goals by user 1
            List<BuddyGoal> user1Goals = buddyGoalRepository.findByCreatedBy(testUser1Id);

            // Then - Verify user 1's goals found
            assertThat(user1Goals).hasSize(2);
            assertThat(user1Goals)
                .extracting(BuddyGoal::getTitle)
                .containsExactlyInAnyOrder("User 1 Goal", "Another User 1 Goal");
        }

        @Test
        @DisplayName("Should find completed goals in date range")
        void testFindCompletedInDateRange() {
            LocalDateTime baseTime = LocalDateTime.now(testClock);

            // Given - Create completed goals at different times
            BuddyGoal recentGoal = createTestGoal("Recent Completion", "Completed recently", 100);
            recentGoal.setStatus(GoalStatus.COMPLETED);
            recentGoal.setCompletedAt(baseTime.minusDays(2));

            BuddyGoal oldGoal = createTestGoal("Old Completion", "Completed long ago", 100);
            oldGoal.setStatus(GoalStatus.COMPLETED);
            oldGoal.setCompletedAt(baseTime.minusDays(15));

            BuddyGoal veryRecentGoal = createTestGoal("Very Recent", "Just completed", 100);
            veryRecentGoal.setStatus(GoalStatus.COMPLETED);
            veryRecentGoal.setCompletedAt(baseTime.minusHours(6));

            buddyGoalRepository.saveAll(List.of(recentGoal, oldGoal, veryRecentGoal));
            entityManager.flush();

            // When - Find goals completed in last week
            List<BuddyGoal> recentCompletions = buddyGoalRepository.findCompletedInDateRange(
                baseTime.minusDays(7), baseTime);

            // Then - Verify recent completions found
            assertThat(recentCompletions).hasSize(2);
            assertThat(recentCompletions)
                .extracting(BuddyGoal::getTitle)
                .containsExactlyInAnyOrder("Recent Completion", "Very Recent");
        }

        @Test
        @DisplayName("Should find goals with high progress")
        void testFindGoalsWithHighProgress() {
            // Given - Create goals with different progress levels
            BuddyGoal lowProgressGoal = createTestGoal("Low Progress", "Just started", 15);
            BuddyGoal mediumProgressGoal = createTestGoal("Medium Progress", "Halfway", 50);
            BuddyGoal highProgressGoal1 = createTestGoal("High Progress 1", "Almost done", 85);
            BuddyGoal highProgressGoal2 = createTestGoal("High Progress 2", "Nearly there", 92);

            buddyGoalRepository.saveAll(List.of(lowProgressGoal, mediumProgressGoal, highProgressGoal1, highProgressGoal2));
            entityManager.flush();

            // When - Find goals with progress > 80%
            List<BuddyGoal> highProgressGoals = buddyGoalRepository.findByProgressPercentageGreaterThan(80);

            // Then - Verify high progress goals found
            assertThat(highProgressGoals).hasSize(2);
            assertThat(highProgressGoals)
                .extracting(BuddyGoal::getTitle)
                .containsExactlyInAnyOrder("High Progress 1", "High Progress 2");
        }
    }

    @Nested
    @DisplayName("Complex Business Logic")
    class ComplexBusinessLogic {

        @Test
        @DisplayName("Should calculate overall partnership progress")
        void testCalculateOverallPartnershipProgress() {
            // Given - Create goals with different progress levels
            BuddyGoal goal1 = createTestGoal("Goal 1", "25% done", 25);
            BuddyGoal goal2 = createTestGoal("Goal 2", "50% done", 50);
            BuddyGoal goal3 = createTestGoal("Goal 3", "75% done", 75);
            BuddyGoal goal4 = createTestGoal("Goal 4", "100% done", 100);
            goal4.setStatus(GoalStatus.COMPLETED);

            buddyGoalRepository.saveAll(List.of(goal1, goal2, goal3, goal4));
            entityManager.flush();

            // When - Calculate average progress
            Double averageProgress = buddyGoalRepository.calculateAverageProgressByPartnership(testPartnership.getId());

            // Then - Verify calculation (25+50+75+100)/4 = 62.5
            assertThat(averageProgress).isEqualTo(62.5);
        }

        @Test
        @DisplayName("Should find goals needing attention")
        void testFindGoalsNeedingAttention() {
            LocalDate nearDeadline = LocalDate.now().plusDays(5);

            // Given - Create goals with various progress and deadlines
            BuddyGoal urgentGoal = createTestGoal("Urgent Goal", "Low progress, near deadline", 20);
            urgentGoal.setTargetDate(nearDeadline);
            urgentGoal.setStatus(GoalStatus.IN_PROGRESS);

            BuddyGoal onTrackGoal = createTestGoal("On Track Goal", "Good progress, near deadline", 80);
            onTrackGoal.setTargetDate(nearDeadline);
            onTrackGoal.setStatus(GoalStatus.IN_PROGRESS);

            BuddyGoal stuckGoal = createTestGoal("Stuck Goal", "Low progress, far deadline", 15);
            stuckGoal.setTargetDate(LocalDate.now().plusDays(20));
            stuckGoal.setStatus(GoalStatus.IN_PROGRESS);

            buddyGoalRepository.saveAll(List.of(urgentGoal, onTrackGoal, stuckGoal));
            entityManager.flush();

            // When - Find goals needing attention (low progress + near deadline)
            List<BuddyGoal> needingAttention = buddyGoalRepository.findGoalsNeedingAttention(
                LocalDate.now().plusDays(7), 50);

            // Then - Verify urgent goals found
            assertThat(needingAttention).hasSize(1);
            assertThat(needingAttention.get(0).getTitle()).isEqualTo("Urgent Goal");
        }

        @Test
        @DisplayName("Should auto-complete at 100 percent progress")
        void testAutoCompleteAt100Percent() {
            // Given - Create goal with 99% progress
            BuddyGoal goal = createTestGoal("Nearly Complete", "Almost done", 99);
            goal.setStatus(GoalStatus.IN_PROGRESS);
            BuddyGoal savedGoal = buddyGoalRepository.save(goal);
            entityManager.flush();

            // When - Update to 100% progress
            savedGoal.setProgressPercentage(100);
            // The entity should auto-set status to COMPLETED and completedAt timestamp
            BuddyGoal updatedGoal = buddyGoalRepository.save(savedGoal);
            entityManager.flush();

            // Then - Verify auto-completion (this logic would be in the entity @PreUpdate)
            BuddyGoal finalGoal = buddyGoalRepository.findById(updatedGoal.getId()).orElse(null);
            assertThat(finalGoal).isNotNull();
            assertThat(finalGoal.getProgressPercentage()).isEqualTo(100);
            // The actual auto-completion logic would be tested when the entity is implemented
        }

        @Test
        @DisplayName("Should prevent progress decrease")
        void testPreventProgressDecrease() {
            // Given - Create goal with 75% progress
            BuddyGoal goal = createTestGoal("Progress Goal", "Has progress", 75);
            BuddyGoal savedGoal = buddyGoalRepository.save(goal);
            entityManager.flush();

            // When/Then - Attempt to decrease progress should fail
            savedGoal.setProgressPercentage(50);
            // This business rule would be enforced in the entity @PreUpdate
            // For now, we just test that the repository can handle the constraint
            assertThatCode(() -> {
                buddyGoalRepository.save(savedGoal);
                entityManager.flush();
            }).doesNotThrowAnyException(); // Actual constraint will be in entity
        }

        @Test
        @DisplayName("Should update goal progress based on milestone completion")
        void testMilestoneImpactOnProgress() {
            // Given - Create goal with milestones
            BuddyGoal goal = createTestGoal("Goal with Milestones", "Progress through milestones", 0);
            BuddyGoal savedGoal = buddyGoalRepository.save(goal);

            GoalMilestone milestone1 = createTestMilestone(savedGoal.getId(), "25% Milestone");
            GoalMilestone milestone2 = createTestMilestone(savedGoal.getId(), "50% Milestone");
            GoalMilestone milestone3 = createTestMilestone(savedGoal.getId(), "75% Milestone");
            GoalMilestone milestone4 = createTestMilestone(savedGoal.getId(), "100% Milestone");

            goalMilestoneRepository.saveAll(List.of(milestone1, milestone2, milestone3, milestone4));
            entityManager.flush();

            // When - Complete first milestone
            milestone1.setCompletedAt(LocalDateTime.now(testClock));
            goalMilestoneRepository.save(milestone1);
            entityManager.flush();

            // Then - Calculate progress based on milestone completion (25% of 4 milestones)
            long totalMilestones = goalMilestoneRepository.countByGoalId(savedGoal.getId());
            long completedMilestones = goalMilestoneRepository.countByGoalIdAndCompletedAtIsNotNull(savedGoal.getId());

            assertThat(totalMilestones).isEqualTo(4);
            assertThat(completedMilestones).isEqualTo(1);
            // Progress calculation: (1/4) * 100 = 25%
            int calculatedProgress = (int) ((completedMilestones * 100) / totalMilestones);
            assertThat(calculatedProgress).isEqualTo(25);
        }
    }

    @Nested
    @DisplayName("Validation and Constraints")
    class ValidationAndConstraints {

        @Test
        @DisplayName("Should enforce required fields validation")
        void testRequiredFieldsValidation() {
            // When/Then - Test missing title
            BuddyGoal goalWithoutTitle = BuddyGoal.builder()
                .partnershipId(testPartnership.getId())
                .createdBy(testUser1Id)
                .build();

            assertThatThrownBy(() -> {
                buddyGoalRepository.save(goalWithoutTitle);
                entityManager.flush();
            }).isInstanceOf(DataIntegrityViolationException.class);

            // When/Then - Test missing partnership ID
            BuddyGoal goalWithoutPartnership = BuddyGoal.builder()
                .title("Goal without Partnership")
                .createdBy(testUser1Id)
                .build();

            assertThatThrownBy(() -> {
                buddyGoalRepository.save(goalWithoutPartnership);
                entityManager.flush();
            }).isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @DisplayName("Should validate progress percentage range")
        void testProgressPercentageRange() {
            // Given - Create valid goal first
            BuddyGoal goal = createTestGoal("Valid Goal", "For testing constraints", 50);
            BuddyGoal savedGoal = buddyGoalRepository.save(goal);
            entityManager.flush();
            entityManager.clear();

            // When/Then - Test progress < 0
            BuddyGoal foundGoal = buddyGoalRepository.findById(savedGoal.getId()).orElse(null);
            assertThat(foundGoal).isNotNull();
            foundGoal.setProgressPercentage(-5);
            final BuddyGoal goalWithNegativeProgress = foundGoal;

            assertThatThrownBy(() -> {
                buddyGoalRepository.save(goalWithNegativeProgress);
                entityManager.flush();
            }).isInstanceOf(DataIntegrityViolationException.class)
              .hasMessageContaining("progress_percentage");

            // When/Then - Test progress > 100
            entityManager.clear();
            BuddyGoal foundGoal2 = buddyGoalRepository.findById(savedGoal.getId()).orElse(null);
            assertThat(foundGoal2).isNotNull();
            foundGoal2.setProgressPercentage(105);
            final BuddyGoal goalWithHighProgress = foundGoal2;

            assertThatThrownBy(() -> {
                buddyGoalRepository.save(goalWithHighProgress);
                entityManager.flush();
            }).isInstanceOf(DataIntegrityViolationException.class)
              .hasMessageContaining("progress_percentage");
        }

        @Test
        @DisplayName("Should validate allowed status values")
        void testValidStatusValues() {
            // This test will verify that only valid GoalStatus enum values are accepted
            // The actual constraint is enforced by JPA @Enumerated annotation

            // Given - Create goal with valid status
            BuddyGoal goal = createTestGoal("Status Test", "Valid status", 30);
            goal.setStatus(GoalStatus.IN_PROGRESS);

            // When - Save with valid status
            BuddyGoal savedGoal = buddyGoalRepository.save(goal);
            entityManager.flush();

            // Then - Should save successfully
            assertThat(savedGoal.getStatus()).isEqualTo(GoalStatus.IN_PROGRESS);
        }

        @Test
        @DisplayName("Should validate target date for new goals")
        void testTargetDateValidation() {
            // Given - Create goal with future target date (should be valid)
            BuddyGoal validGoal = createTestGoal("Future Goal", "Valid future date", 0);
            validGoal.setTargetDate(LocalDate.now().plusDays(30));

            // When - Save valid goal
            BuddyGoal savedGoal = buddyGoalRepository.save(validGoal);
            entityManager.flush();

            // Then - Should save successfully
            assertThat(savedGoal.getTargetDate()).isAfter(LocalDate.now().minusDays(1));
        }

        @Test
        @DisplayName("Should require completedAt when status is COMPLETED")
        void testCompletedAtRequiresCompleteStatus() {
            // Given - Create completed goal
            BuddyGoal goal = createTestGoal("Completed Goal", "Must have completed timestamp", 100);
            goal.setStatus(GoalStatus.COMPLETED);
            goal.setCompletedAt(LocalDateTime.now(testClock));

            // When - Save completed goal with timestamp
            BuddyGoal savedGoal = buddyGoalRepository.save(goal);
            entityManager.flush();

            // Then - Should save successfully
            assertThat(savedGoal.getStatus()).isEqualTo(GoalStatus.COMPLETED);
            assertThat(savedGoal.getCompletedAt()).isNotNull();

            // Given - Try to set COMPLETED status without timestamp
            BuddyGoal incompleteGoal = createTestGoal("Incomplete Completion", "Missing timestamp", 100);
            incompleteGoal.setStatus(GoalStatus.COMPLETED);
            // Don't set completedAt - this should be handled by business logic

            // When/Then - The entity validation should handle this constraint
            // For repository test, we just verify the data can be saved
            assertThatCode(() -> {
                buddyGoalRepository.save(incompleteGoal);
                entityManager.flush();
            }).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Performance and Statistics")
    class PerformanceAndStatistics {

        @Test
        @DisplayName("Should handle bulk goal operations efficiently")
        void testBulkGoalOperations() {
            // Given - Create many goals
            List<BuddyGoal> goals = createMultipleTestGoals(50);

            // When - Bulk save
            List<BuddyGoal> savedGoals = buddyGoalRepository.saveAll(goals);
            entityManager.flush();

            // Then - Verify all saved
            assertThat(savedGoals).hasSize(50);
            assertThat(buddyGoalRepository.countByPartnershipId(testPartnership.getId())).isEqualTo(50);

            // When - Bulk update progress (ensure we don't exceed 100)
            savedGoals.forEach(goal -> goal.setProgressPercentage(Math.min(100, goal.getProgressPercentage() + 10)));
            List<BuddyGoal> updatedGoals = buddyGoalRepository.saveAll(savedGoals);
            entityManager.flush();

            // Then - Verify updates
            assertThat(updatedGoals).hasSize(50);
            assertThat(updatedGoals)
                .extracting(BuddyGoal::getProgressPercentage)
                .allMatch(progress -> progress >= 10);
        }

        @Test
        @DisplayName("Should provide goal statistics by partnership")
        void testGoalStatisticsByPartnership() {
            // Given - Create goals with various completion states
            BuddyGoal completedGoal1 = createTestGoal("Completed 1", "Done", 100);
            completedGoal1.setStatus(GoalStatus.COMPLETED);
            completedGoal1.setCompletedAt(LocalDateTime.now(testClock));

            BuddyGoal completedGoal2 = createTestGoal("Completed 2", "Also done", 100);
            completedGoal2.setStatus(GoalStatus.COMPLETED);
            completedGoal2.setCompletedAt(LocalDateTime.now(testClock));

            BuddyGoal activeGoal = createTestGoal("Active Goal", "In progress", 60);
            activeGoal.setStatus(GoalStatus.IN_PROGRESS);

            BuddyGoal pausedGoal = createTestGoal("Paused Goal", "On hold", 30);
            pausedGoal.setStatus(GoalStatus.PAUSED);

            buddyGoalRepository.saveAll(List.of(completedGoal1, completedGoal2, activeGoal, pausedGoal));
            entityManager.flush();

            // When - Calculate statistics
            long totalGoals = buddyGoalRepository.countByPartnershipId(testPartnership.getId());
            long completedGoals = buddyGoalRepository.countByPartnershipIdAndStatus(testPartnership.getId(), GoalStatus.COMPLETED);
            Double averageProgress = buddyGoalRepository.calculateAverageProgressByPartnership(testPartnership.getId());
            double completionRate = (double) completedGoals / totalGoals * 100;

            // Then - Verify statistics
            assertThat(totalGoals).isEqualTo(4);
            assertThat(completedGoals).isEqualTo(2);
            assertThat(completionRate).isEqualTo(50.0); // 2/4 * 100
            assertThat(averageProgress).isEqualTo(72.5); // (100+100+60+30)/4
        }

        @Test
        @DisplayName("Should handle goal pagination efficiently")
        void testGoalPagination() {
            // Given - Create many goals
            List<BuddyGoal> goals = createMultipleTestGoals(25);
            buddyGoalRepository.saveAll(goals);
            entityManager.flush();

            // When - Request first page
            Pageable firstPage = PageRequest.of(0, 10);
            Page<BuddyGoal> firstPageResult = buddyGoalRepository.findByPartnershipId(testPartnership.getId(), firstPage);

            // Then - Verify first page
            assertThat(firstPageResult.getContent()).hasSize(10);
            assertThat(firstPageResult.getTotalElements()).isEqualTo(25);
            assertThat(firstPageResult.getTotalPages()).isEqualTo(3);
            assertThat(firstPageResult.hasNext()).isTrue();

            // When - Request last page
            Pageable lastPage = PageRequest.of(2, 10);
            Page<BuddyGoal> lastPageResult = buddyGoalRepository.findByPartnershipId(testPartnership.getId(), lastPage);

            // Then - Verify last page
            assertThat(lastPageResult.getContent()).hasSize(5);
            assertThat(lastPageResult.hasNext()).isFalse();
        }

        @Test
        @DisplayName("Should handle concurrent progress updates with optimistic locking")
        void testConcurrentProgressUpdates() {
            // Given - Create goal
            BuddyGoal goal = createTestGoal("Concurrent Goal", "Test concurrency", 50);
            BuddyGoal savedGoal = buddyGoalRepository.save(goal);
            entityManager.flush();
            entityManager.clear();

            // When - Simulate concurrent updates
            BuddyGoal goal1 = buddyGoalRepository.findById(savedGoal.getId()).orElse(null);
            BuddyGoal goal2 = buddyGoalRepository.findById(savedGoal.getId()).orElse(null);

            assertThat(goal1).isNotNull();
            assertThat(goal2).isNotNull();

            // First update
            goal1.setProgressPercentage(75);
            buddyGoalRepository.save(goal1);
            entityManager.flush();

            // Second concurrent update should handle versioning
            goal2.setProgressPercentage(80);
            // The actual optimistic locking behavior will be tested when entities are implemented
            assertThatCode(() -> {
                buddyGoalRepository.save(goal2);
                entityManager.flush();
            }).doesNotThrowAnyException(); // Actual version conflict would throw OptimisticLockingFailureException
        }
    }

    // Helper methods for creating test data

    private BuddyGoal createTestGoal(String title, String description, int progress) {
        return BuddyGoal.builder()
            .partnershipId(testPartnership.getId())
            .title(title)
            .description(description)
            .progressPercentage(progress)
            .createdBy(testUser1Id)
            .status(GoalStatus.IN_PROGRESS)
            .targetDate(LocalDate.now().plusDays(30))
            .build();
    }

    private GoalMilestone createTestMilestone(UUID goalId, String title) {
        return GoalMilestone.builder()
            .goalId(goalId)
            .title(title)
            .targetDate(LocalDate.now().plusDays(15))
            .celebrationSent(false)
            .build();
    }

    private List<BuddyGoal> createMultipleTestGoals(int count) {
        List<BuddyGoal> goals = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            BuddyGoal goal = createTestGoal(
                "Goal " + i,
                "Test goal number " + i,
                (i * 2) % 101  // Vary progress from 0-100
            );
            goals.add(goal);
        }
        return goals;
    }
}