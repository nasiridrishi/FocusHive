package com.focushive.buddy.repository;

import com.focushive.buddy.constant.PartnershipStatus;
import com.focushive.buddy.entity.BuddyPartnership;
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
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.*;

/**
 * TDD Test Suite for BuddyPartnershipRepository
 *
 * This test class follows strict TDD principles - all tests are written FIRST
 * and MUST fail initially since BuddyPartnership entity and repository don't exist.
 *
 * Expected failures:
 * - Cannot resolve symbol 'BuddyPartnership'
 * - Cannot resolve symbol 'BuddyPartnershipRepository'
 * - Cannot resolve symbol 'PartnershipStatus'
 *
 * Phase 1.2: RED phase - Tests must fail before implementation
 */
@DisplayName("BuddyPartnership Repository Tests")
class BuddyPartnershipRepositoryTest extends AbstractRepositoryTest {

    @Autowired
    private BuddyPartnershipRepository partnershipRepository;

    @Autowired
    private TestEntityManager entityManager;

    private TestDataBuilder testDataBuilder;
    private Clock fixedClock;

    @BeforeEach
    void setUp() {
        testDataBuilder = new TestDataBuilder();
        fixedClock = Clock.fixed(
            ZonedDateTime.of(2025, 1, 15, 10, 0, 0, 0, ZoneId.of("UTC")).toInstant(),
            ZoneId.of("UTC")
        );
    }

    @Nested
    @DisplayName("Basic CRUD Operations")
    class BasicCrudOperations {

        @Test
        @DisplayName("Should save partnership successfully")
        void testSavePartnership() {
            // Given
            UUID user1Id = UUID.randomUUID();
            UUID user2Id = UUID.randomUUID();
            BuddyPartnership partnership = testDataBuilder.buildBuddyPartnership()
                .withUser1Id(user1Id)
                .withUser2Id(user2Id)
                .withStatus(PartnershipStatus.PENDING)
                .withAgreementText("We will support each other's goals")
                .withDurationDays(30)
                .withCompatibilityScore(BigDecimal.valueOf(0.85))
                .build();

            // When
            BuddyPartnership savedPartnership = partnershipRepository.save(partnership);

            // Then
            assertThat(savedPartnership).isNotNull();
            assertThat(savedPartnership.getId()).isNotNull();
            assertThat(savedPartnership.getUser1Id()).isEqualTo(user1Id);
            assertThat(savedPartnership.getUser2Id()).isEqualTo(user2Id);
            assertThat(savedPartnership.getStatus()).isEqualTo(PartnershipStatus.PENDING);
            assertThat(savedPartnership.getAgreementText()).isEqualTo("We will support each other's goals");
            assertThat(savedPartnership.getDurationDays()).isEqualTo(30);
            assertThat(savedPartnership.getCompatibilityScore()).isEqualByComparingTo(BigDecimal.valueOf(0.85));
            assertThat(savedPartnership.getHealthScore()).isEqualByComparingTo(BigDecimal.valueOf(1.00));
            assertThat(savedPartnership.getCreatedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should find partnership by ID")
        void testFindPartnershipById() {
            // Given
            BuddyPartnership partnership = testDataBuilder.buildBuddyPartnership()
                .withUser1Id(UUID.randomUUID())
                .withUser2Id(UUID.randomUUID())
                .build();
            BuddyPartnership savedPartnership = partnershipRepository.save(partnership);

            // When
            Optional<BuddyPartnership> foundPartnership = partnershipRepository.findById(savedPartnership.getId());

            // Then
            assertThat(foundPartnership).isPresent();
            assertThat(foundPartnership.get().getId()).isEqualTo(savedPartnership.getId());
            assertThat(foundPartnership.get().getUser1Id()).isEqualTo(savedPartnership.getUser1Id());
            assertThat(foundPartnership.get().getUser2Id()).isEqualTo(savedPartnership.getUser2Id());
        }

        @Test
        @DisplayName("Should update partnership details")
        void testUpdatePartnership() {
            // Given
            BuddyPartnership partnership = testDataBuilder.buildBuddyPartnership()
                .withUser1Id(UUID.randomUUID())
                .withUser2Id(UUID.randomUUID())
                .withStatus(PartnershipStatus.PENDING)
                .withHealthScore(BigDecimal.valueOf(1.00))
                .build();
            BuddyPartnership savedPartnership = partnershipRepository.save(partnership);

            // When
            // Use entity manager to flush changes and ensure timestamp difference
            entityManager.flush();
            entityManager.clear();

            // Add a small delay to ensure updatedAt is different from createdAt
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // Get fresh instance and modify
            BuddyPartnership freshPartnership = partnershipRepository.findById(savedPartnership.getId()).orElseThrow();
            freshPartnership.setStatus(PartnershipStatus.ACTIVE);
            freshPartnership.setStartedAt(ZonedDateTime.now(fixedClock));
            freshPartnership.setHealthScore(BigDecimal.valueOf(0.95));
            BuddyPartnership updatedPartnership = partnershipRepository.save(freshPartnership);

            // Then
            assertThat(updatedPartnership.getStatus()).isEqualTo(PartnershipStatus.ACTIVE);
            assertThat(updatedPartnership.getStartedAt()).isNotNull();
            assertThat(updatedPartnership.getHealthScore()).isEqualByComparingTo(BigDecimal.valueOf(0.95));
            assertThat(updatedPartnership.getUpdatedAt()).isAfterOrEqualTo(updatedPartnership.getCreatedAt());
        }

        @Test
        @DisplayName("Should delete partnership")
        void testDeletePartnership() {
            // Given
            BuddyPartnership partnership = testDataBuilder.buildBuddyPartnership()
                .withUser1Id(UUID.randomUUID())
                .withUser2Id(UUID.randomUUID())
                .build();
            BuddyPartnership savedPartnership = partnershipRepository.save(partnership);
            UUID partnershipId = savedPartnership.getId();

            // When
            partnershipRepository.delete(savedPartnership);

            // Then
            Optional<BuddyPartnership> deletedPartnership = partnershipRepository.findById(partnershipId);
            assertThat(deletedPartnership).isEmpty();
        }

        @Test
        @DisplayName("Should find partnership by user IDs")
        void testFindByUser1IdAndUser2Id() {
            // Given
            UUID user1Id = UUID.randomUUID();
            UUID user2Id = UUID.randomUUID();
            BuddyPartnership partnership = testDataBuilder.buildBuddyPartnership()
                .withUser1Id(user1Id)
                .withUser2Id(user2Id)
                .build();
            partnershipRepository.save(partnership);

            // When
            Optional<BuddyPartnership> foundPartnership = partnershipRepository.findByUser1IdAndUser2Id(user1Id, user2Id);

            // Then
            assertThat(foundPartnership).isPresent();
            assertThat(foundPartnership.get().getUser1Id()).isEqualTo(user1Id);
            assertThat(foundPartnership.get().getUser2Id()).isEqualTo(user2Id);
        }
    }

    @Nested
    @DisplayName("Partnership Lifecycle Tests")
    class PartnershipLifecycleTests {

        @Test
        @DisplayName("Should create partnership with PENDING status by default")
        void testCreatePendingPartnership() {
            // Given
            BuddyPartnership partnership = testDataBuilder.buildBuddyPartnership()
                .withUser1Id(UUID.randomUUID())
                .withUser2Id(UUID.randomUUID())
                .build();

            // When
            BuddyPartnership savedPartnership = partnershipRepository.save(partnership);

            // Then
            assertThat(savedPartnership.getStatus()).isEqualTo(PartnershipStatus.PENDING);
            assertThat(savedPartnership.getStartedAt()).isNull();
            assertThat(savedPartnership.getEndedAt()).isNull();
            assertThat(savedPartnership.getEndReason()).isNull();
        }

        @Test
        @DisplayName("Should activate partnership with started_at timestamp")
        void testActivatePartnership() {
            // Given
            BuddyPartnership partnership = testDataBuilder.buildBuddyPartnership()
                .withUser1Id(UUID.randomUUID())
                .withUser2Id(UUID.randomUUID())
                .withStatus(PartnershipStatus.PENDING)
                .build();
            BuddyPartnership savedPartnership = partnershipRepository.save(partnership);

            // When
            savedPartnership.setStatus(PartnershipStatus.ACTIVE);
            savedPartnership.setStartedAt(ZonedDateTime.now(fixedClock));
            BuddyPartnership activatedPartnership = partnershipRepository.save(savedPartnership);

            // Then
            assertThat(activatedPartnership.getStatus()).isEqualTo(PartnershipStatus.ACTIVE);
            assertThat(activatedPartnership.getStartedAt()).isNotNull();
            assertThat(activatedPartnership.getEndedAt()).isNull();
        }

        @Test
        @DisplayName("Should pause partnership")
        void testPausePartnership() {
            // Given
            BuddyPartnership partnership = testDataBuilder.buildBuddyPartnership()
                .withUser1Id(UUID.randomUUID())
                .withUser2Id(UUID.randomUUID())
                .withStatus(PartnershipStatus.ACTIVE)
                .withStartedAt(ZonedDateTime.now(fixedClock).minusDays(5))
                .build();
            BuddyPartnership savedPartnership = partnershipRepository.save(partnership);

            // When
            savedPartnership.setStatus(PartnershipStatus.PAUSED);
            BuddyPartnership pausedPartnership = partnershipRepository.save(savedPartnership);

            // Then
            assertThat(pausedPartnership.getStatus()).isEqualTo(PartnershipStatus.PAUSED);
            assertThat(pausedPartnership.getStartedAt()).isNotNull();
            assertThat(pausedPartnership.getEndedAt()).isNull();
        }

        @Test
        @DisplayName("Should end partnership with reason")
        void testEndPartnership() {
            // Given
            BuddyPartnership partnership = testDataBuilder.buildBuddyPartnership()
                .withUser1Id(UUID.randomUUID())
                .withUser2Id(UUID.randomUUID())
                .withStatus(PartnershipStatus.ACTIVE)
                .withStartedAt(ZonedDateTime.now(fixedClock).minusDays(15))
                .build();
            BuddyPartnership savedPartnership = partnershipRepository.save(partnership);

            // When
            savedPartnership.setStatus(PartnershipStatus.ENDED);
            savedPartnership.setEndedAt(ZonedDateTime.now(fixedClock));
            savedPartnership.setEndReason("Goals completed successfully");
            BuddyPartnership endedPartnership = partnershipRepository.save(savedPartnership);

            // Then
            assertThat(endedPartnership.getStatus()).isEqualTo(PartnershipStatus.ENDED);
            assertThat(endedPartnership.getEndedAt()).isNotNull();
            assertThat(endedPartnership.getEndReason()).isEqualTo("Goals completed successfully");
        }

        @Test
        @DisplayName("Should prevent invalid status transitions")
        void testInvalidStatusTransition() {
            // Given
            BuddyPartnership partnership = testDataBuilder.buildBuddyPartnership()
                .withUser1Id(UUID.randomUUID())
                .withUser2Id(UUID.randomUUID())
                .withStatus(PartnershipStatus.ENDED)
                .withEndedAt(ZonedDateTime.now(fixedClock).minusDays(1))
                .build();

            BuddyPartnership savedPartnership = partnershipRepository.saveAndFlush(partnership);
            entityManager.clear(); // Clear the persistence context to ensure fresh fetch

            // When/Then - Attempting to reactivate ended partnership should fail
            // This test validates business logic constraints
            assertThatThrownBy(() -> {
                // Reload the entity to ensure we have the persisted state
                BuddyPartnership reloadedPartnership = partnershipRepository.findById(savedPartnership.getId())
                    .orElseThrow(() -> new RuntimeException("Partnership not found"));

                reloadedPartnership.setStatus(PartnershipStatus.ACTIVE);
                reloadedPartnership.setEndedAt(null); // Clear ended timestamp
                partnershipRepository.saveAndFlushWithValidation(reloadedPartnership);
            }).isInstanceOf(DataIntegrityViolationException.class);
        }
    }

    @Nested
    @DisplayName("Bidirectional Relationship Tests")
    class BidirectionalRelationshipTests {

        @Test
        @DisplayName("Should find partnership regardless of user order")
        void testFindPartnershipBidirectional() {
            // Given
            UUID user1Id = UUID.randomUUID();
            UUID user2Id = UUID.randomUUID();
            BuddyPartnership partnership = testDataBuilder.buildBuddyPartnership()
                .withUser1Id(user1Id)
                .withUser2Id(user2Id)
                .build();
            partnershipRepository.save(partnership);

            // When
            Optional<BuddyPartnership> found1 = partnershipRepository.findPartnershipBetweenUsers(user1Id, user2Id);
            Optional<BuddyPartnership> found2 = partnershipRepository.findPartnershipBetweenUsers(user2Id, user1Id);

            // Then
            assertThat(found1).isPresent();
            assertThat(found2).isPresent();
            assertThat(found1.get().getId()).isEqualTo(found2.get().getId());
        }

        @Test
        @DisplayName("Should prevent duplicate partnerships")
        void testPreventDuplicatePartnership() {
            // Given
            UUID user1Id = UUID.randomUUID();
            UUID user2Id = UUID.randomUUID();
            BuddyPartnership partnership1 = testDataBuilder.buildBuddyPartnership()
                .withUser1Id(user1Id)
                .withUser2Id(user2Id)
                .build();
            partnershipRepository.save(partnership1);

            // When/Then - Attempting to create duplicate should fail
            BuddyPartnership partnership2 = testDataBuilder.buildBuddyPartnership()
                .withUser1Id(user1Id)
                .withUser2Id(user2Id)
                .build();

            assertThatThrownBy(() -> {
                partnershipRepository.saveWithDuplicateCheck(partnership2);
            }).isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @DisplayName("Should find all partnerships for a user")
        void testFindAllPartnershipsByUserId() {
            // Given
            UUID userId = UUID.randomUUID();
            UUID partner1Id = UUID.randomUUID();
            UUID partner2Id = UUID.randomUUID();

            partnershipRepository.save(testDataBuilder.buildBuddyPartnership()
                .withUser1Id(userId)
                .withUser2Id(partner1Id)
                .build());

            partnershipRepository.save(testDataBuilder.buildBuddyPartnership()
                .withUser1Id(partner2Id)
                .withUser2Id(userId)
                .build());

            // When
            List<BuddyPartnership> partnerships = partnershipRepository.findAllPartnershipsByUserId(userId);

            // Then
            assertThat(partnerships).hasSize(2);
            assertThat(partnerships).allSatisfy(partnership -> {
                assertThat(partnership.getUser1Id().equals(userId) || partnership.getUser2Id().equals(userId))
                    .isTrue();
            });
        }

        @Test
        @DisplayName("Should count active partnerships by user")
        void testCountActivePartnershipsByUserId() {
            // Given
            UUID userId = UUID.randomUUID();

            // Create 2 active partnerships
            partnershipRepository.save(testDataBuilder.buildBuddyPartnership()
                .withUser1Id(userId)
                .withUser2Id(UUID.randomUUID())
                .withStatus(PartnershipStatus.ACTIVE)
                .build());

            partnershipRepository.save(testDataBuilder.buildBuddyPartnership()
                .withUser1Id(UUID.randomUUID())
                .withUser2Id(userId)
                .withStatus(PartnershipStatus.ACTIVE)
                .build());

            // Create 1 pending partnership (should not be counted)
            partnershipRepository.save(testDataBuilder.buildBuddyPartnership()
                .withUser1Id(userId)
                .withUser2Id(UUID.randomUUID())
                .withStatus(PartnershipStatus.PENDING)
                .build());

            // When
            long activeCount = partnershipRepository.countActivePartnershipsByUserId(userId);

            // Then
            assertThat(activeCount).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("Query Methods for Matching")
    class QueryMethodsForMatching {

        @Test
        @DisplayName("Should find all active partnerships")
        void testFindActivePartnerships() {
            // Given
            partnershipRepository.save(testDataBuilder.buildBuddyPartnership()
                .withUser1Id(UUID.randomUUID())
                .withUser2Id(UUID.randomUUID())
                .withStatus(PartnershipStatus.ACTIVE)
                .build());

            partnershipRepository.save(testDataBuilder.buildBuddyPartnership()
                .withUser1Id(UUID.randomUUID())
                .withUser2Id(UUID.randomUUID())
                .withStatus(PartnershipStatus.PENDING)
                .build());

            // When
            List<BuddyPartnership> activePartnerships = partnershipRepository.findByStatus(PartnershipStatus.ACTIVE);

            // Then
            assertThat(activePartnerships).hasSize(1);
            assertThat(activePartnerships.get(0).getStatus()).isEqualTo(PartnershipStatus.ACTIVE);
        }

        @Test
        @DisplayName("Should find pending partnership requests for user")
        void testFindPendingPartnershipRequests() {
            // Given
            UUID userId = UUID.randomUUID();

            partnershipRepository.save(testDataBuilder.buildBuddyPartnership()
                .withUser1Id(UUID.randomUUID())
                .withUser2Id(userId)
                .withStatus(PartnershipStatus.PENDING)
                .build());

            partnershipRepository.save(testDataBuilder.buildBuddyPartnership()
                .withUser1Id(userId)
                .withUser2Id(UUID.randomUUID())
                .withStatus(PartnershipStatus.ACTIVE)
                .build());

            // When
            List<BuddyPartnership> pendingRequests = partnershipRepository.findPendingPartnershipsByUserId(userId);

            // Then
            assertThat(pendingRequests).hasSize(1);
            assertThat(pendingRequests.get(0).getStatus()).isEqualTo(PartnershipStatus.PENDING);
        }

        @Test
        @DisplayName("Should find ended partnerships with reasons")
        void testFindEndedPartnershipsWithReason() {
            // Given
            partnershipRepository.save(testDataBuilder.buildBuddyPartnership()
                .withUser1Id(UUID.randomUUID())
                .withUser2Id(UUID.randomUUID())
                .withStatus(PartnershipStatus.ENDED)
                .withEndReason("Incompatible schedules")
                .build());

            partnershipRepository.save(testDataBuilder.buildBuddyPartnership()
                .withUser1Id(UUID.randomUUID())
                .withUser2Id(UUID.randomUUID())
                .withStatus(PartnershipStatus.ENDED)
                .withEndReason("Goals completed")
                .build());

            // When
            List<BuddyPartnership> endedWithReasons = partnershipRepository.findEndedPartnershipsWithReasons();

            // Then
            assertThat(endedWithReasons).hasSize(2);
            assertThat(endedWithReasons).allSatisfy(partnership -> {
                assertThat(partnership.getStatus()).isEqualTo(PartnershipStatus.ENDED);
                assertThat(partnership.getEndReason()).isNotBlank();
            });
        }

        @Test
        @DisplayName("Should find partnerships by date range")
        void testFindPartnershipsByDateRange() {
            // Given
            ZonedDateTime startDate = ZonedDateTime.now(fixedClock).minusDays(30);
            ZonedDateTime endDate = ZonedDateTime.now(fixedClock).minusDays(10);

            partnershipRepository.save(testDataBuilder.buildBuddyPartnership()
                .withUser1Id(UUID.randomUUID())
                .withUser2Id(UUID.randomUUID())
                .withStartedAt(ZonedDateTime.now(fixedClock).minusDays(20)) // Within range
                .build());

            partnershipRepository.save(testDataBuilder.buildBuddyPartnership()
                .withUser1Id(UUID.randomUUID())
                .withUser2Id(UUID.randomUUID())
                .withStartedAt(ZonedDateTime.now(fixedClock).minusDays(5)) // Outside range
                .build());

            // When
            List<BuddyPartnership> partnershipsInRange = partnershipRepository.findPartnershipsByStartedAtBetween(startDate, endDate);

            // Then
            assertThat(partnershipsInRange).hasSize(1);
            assertThat(partnershipsInRange.get(0).getStartedAt()).isBetween(startDate, endDate);
        }

        @Test
        @DisplayName("Should find healthy partnerships above threshold")
        void testFindHealthyPartnerships() {
            // Given
            BigDecimal healthThreshold = BigDecimal.valueOf(0.7);

            partnershipRepository.save(testDataBuilder.buildBuddyPartnership()
                .withUser1Id(UUID.randomUUID())
                .withUser2Id(UUID.randomUUID())
                .withHealthScore(BigDecimal.valueOf(0.8))
                .build());

            partnershipRepository.save(testDataBuilder.buildBuddyPartnership()
                .withUser1Id(UUID.randomUUID())
                .withUser2Id(UUID.randomUUID())
                .withHealthScore(BigDecimal.valueOf(0.5))
                .build());

            // When
            List<BuddyPartnership> healthyPartnerships = partnershipRepository.findByHealthScoreGreaterThan(healthThreshold);

            // Then
            assertThat(healthyPartnerships).hasSize(1);
            assertThat(healthyPartnerships.get(0).getHealthScore()).isGreaterThan(healthThreshold);
        }
    }

    @Nested
    @DisplayName("Complex Business Logic")
    class ComplexBusinessLogic {

        @Test
        @DisplayName("Should calculate partnership duration in days")
        void testCalculatePartnershipDuration() {
            // Given
            ZonedDateTime startTime = ZonedDateTime.now(fixedClock).minusDays(15);
            ZonedDateTime endTime = ZonedDateTime.now(fixedClock);

            BuddyPartnership partnership = testDataBuilder.buildBuddyPartnership()
                .withUser1Id(UUID.randomUUID())
                .withUser2Id(UUID.randomUUID())
                .withStatus(PartnershipStatus.ENDED)
                .withStartedAt(startTime)
                .withEndedAt(endTime)
                .build();
            BuddyPartnership savedPartnership = partnershipRepository.save(partnership);

            // When
            Long durationDays = partnershipRepository.calculatePartnershipDurationInDays(savedPartnership.getId());

            // Then
            assertThat(durationDays).isEqualTo(15);
        }

        @Test
        @DisplayName("Should update health scores periodically")
        void testUpdateHealthScore() {
            // Given
            BuddyPartnership partnership = testDataBuilder.buildBuddyPartnership()
                .withUser1Id(UUID.randomUUID())
                .withUser2Id(UUID.randomUUID())
                .withHealthScore(BigDecimal.valueOf(1.00))
                .build();
            BuddyPartnership savedPartnership = partnershipRepository.save(partnership);

            // When
            BigDecimal newHealthScore = BigDecimal.valueOf(0.75);
            partnershipRepository.updateHealthScore(savedPartnership.getId(), newHealthScore);
            entityManager.flush();
            entityManager.clear();

            BuddyPartnership updatedPartnership = partnershipRepository.findById(savedPartnership.getId()).orElseThrow();

            // Then
            assertThat(updatedPartnership.getHealthScore()).isEqualByComparingTo(newHealthScore);
        }

        @Test
        @DisplayName("Should update last interaction timestamp")
        void testUpdateLastInteractionTime() {
            // Given
            BuddyPartnership partnership = testDataBuilder.buildBuddyPartnership()
                .withUser1Id(UUID.randomUUID())
                .withUser2Id(UUID.randomUUID())
                .build();
            BuddyPartnership savedPartnership = partnershipRepository.save(partnership);

            // When
            ZonedDateTime interactionTime = ZonedDateTime.now(fixedClock);
            partnershipRepository.updateLastInteractionAt(savedPartnership.getId(), interactionTime);
            entityManager.flush();
            entityManager.clear();

            BuddyPartnership updatedPartnership = partnershipRepository.findById(savedPartnership.getId()).orElseThrow();

            // Then
            assertThat(updatedPartnership.getLastInteractionAt()).isEqualTo(interactionTime);
        }

        @Test
        @DisplayName("Should find inactive partnerships")
        void testFindInactivePartnerships() {
            // Given
            ZonedDateTime longAgo = ZonedDateTime.now(fixedClock).minusDays(10);
            ZonedDateTime recently = ZonedDateTime.now(fixedClock).minusHours(1);

            // Inactive partnership - save first then update timestamp
            BuddyPartnership inactivePartnership = partnershipRepository.save(testDataBuilder.buildBuddyPartnership()
                .withUser1Id(UUID.randomUUID())
                .withUser2Id(UUID.randomUUID())
                .withStatus(PartnershipStatus.ACTIVE)
                .build());
            partnershipRepository.updateLastInteractionAt(inactivePartnership.getId(), longAgo);

            // Active partnership - save first then update timestamp
            BuddyPartnership activePartnership = partnershipRepository.save(testDataBuilder.buildBuddyPartnership()
                .withUser1Id(UUID.randomUUID())
                .withUser2Id(UUID.randomUUID())
                .withStatus(PartnershipStatus.ACTIVE)
                .build());
            partnershipRepository.updateLastInteractionAt(activePartnership.getId(), recently);

            // Flush and clear the entity manager to ensure changes are persisted
            entityManager.flush();
            entityManager.clear();

            // When
            int inactiveDays = 7;
            ZonedDateTime currentTime = ZonedDateTime.now(fixedClock);
            List<BuddyPartnership> inactivePartnerships = partnershipRepository.findInactivePartnerships(inactiveDays, currentTime);

            // Then
            assertThat(inactivePartnerships).hasSize(1);
            assertThat(inactivePartnerships.get(0).getLastInteractionAt()).isBefore(
                ZonedDateTime.now(fixedClock).minusDays(inactiveDays)
            );
        }

        @Test
        @DisplayName("Should find expired pending requests")
        void testFindExpiredPendingRequests() {
            // Given
            LocalDateTime expiredTime = LocalDateTime.now(fixedClock).minusHours(80); // > 72 hours
            LocalDateTime recentTime = LocalDateTime.now(fixedClock).minusHours(24);  // < 72 hours

            // Expired pending request - save first then update created timestamp
            BuddyPartnership expiredPartnership = partnershipRepository.save(testDataBuilder.buildBuddyPartnership()
                .withUser1Id(UUID.randomUUID())
                .withUser2Id(UUID.randomUUID())
                .withStatus(PartnershipStatus.PENDING)
                .build());

            // Recent pending request - save first then update created timestamp
            BuddyPartnership recentPartnership = partnershipRepository.save(testDataBuilder.buildBuddyPartnership()
                .withUser1Id(UUID.randomUUID())
                .withUser2Id(UUID.randomUUID())
                .withStatus(PartnershipStatus.PENDING)
                .build());

            // Update created timestamps using raw SQL since @CreatedDate overrides manual setting
            entityManager.getEntityManager().createNativeQuery("UPDATE buddy_partnerships SET created_at = ? WHERE id = ?")
                .setParameter(1, expiredTime)
                .setParameter(2, expiredPartnership.getId())
                .executeUpdate();

            entityManager.getEntityManager().createNativeQuery("UPDATE buddy_partnerships SET created_at = ? WHERE id = ?")
                .setParameter(1, recentTime)
                .setParameter(2, recentPartnership.getId())
                .executeUpdate();

            // Flush and clear the entity manager to ensure changes are persisted
            entityManager.flush();
            entityManager.clear();

            // When
            int expirationHours = 72;
            LocalDateTime currentTime = LocalDateTime.now(fixedClock);
            List<BuddyPartnership> expiredRequests = partnershipRepository.findExpiredPendingRequests(expirationHours, currentTime);

            // Then
            assertThat(expiredRequests).hasSize(1);
            assertThat(expiredRequests.get(0).getStatus()).isEqualTo(PartnershipStatus.PENDING);
            assertThat(expiredRequests.get(0).getCreatedAt()).isBefore(
                LocalDateTime.now(fixedClock).minusHours(expirationHours)
            );
        }
    }

    @Nested
    @DisplayName("Validation and Constraints")
    class ValidationAndConstraints {

        @Test
        @DisplayName("Should prevent self-partnership")
        void testPreventSelfPartnership() {
            // Given
            UUID userId = UUID.randomUUID();
            BuddyPartnership selfPartnership = testDataBuilder.buildBuddyPartnership()
                .withUser1Id(userId)
                .withUser2Id(userId) // Same user!
                .build();

            // When/Then
            assertThatThrownBy(() -> {
                partnershipRepository.saveAndFlush(selfPartnership);
            }).isInstanceOf(DataIntegrityViolationException.class)
              .hasMessageContaining("chk_different_users");
        }

        @Test
        @DisplayName("Should enforce valid status values")
        void testEnforceStatusValues() {
            // Given - This test would require custom validation or enum constraints
            BuddyPartnership partnership = testDataBuilder.buildBuddyPartnership()
                .withUser1Id(UUID.randomUUID())
                .withUser2Id(UUID.randomUUID())
                .build();

            // When/Then - Status must be one of the valid enum values
            // This validates that PartnershipStatus enum constraints work
            partnership.setStatus(PartnershipStatus.PENDING);
            BuddyPartnership saved1 = partnershipRepository.save(partnership);
            assertThat(saved1.getStatus()).isEqualTo(PartnershipStatus.PENDING);

            partnership.setStatus(PartnershipStatus.ACTIVE);
            BuddyPartnership saved2 = partnershipRepository.save(partnership);
            assertThat(saved2.getStatus()).isEqualTo(PartnershipStatus.ACTIVE);

            partnership.setStatus(PartnershipStatus.PAUSED);
            BuddyPartnership saved3 = partnershipRepository.save(partnership);
            assertThat(saved3.getStatus()).isEqualTo(PartnershipStatus.PAUSED);

            partnership.setStatus(PartnershipStatus.ENDED);
            BuddyPartnership saved4 = partnershipRepository.save(partnership);
            assertThat(saved4.getStatus()).isEqualTo(PartnershipStatus.ENDED);
        }

        @Test
        @DisplayName("Should validate compatibility score range")
        void testValidateCompatibilityScore() {
            // Given
            BuddyPartnership partnership = testDataBuilder.buildBuddyPartnership()
                .withUser1Id(UUID.randomUUID())
                .withUser2Id(UUID.randomUUID())
                .withCompatibilityScore(BigDecimal.valueOf(1.5)) // Invalid: > 1.0
                .build();

            // When/Then
            assertThatThrownBy(() -> {
                partnershipRepository.saveAndFlush(partnership);
            }).isInstanceOf(DataIntegrityViolationException.class)
              .hasMessageContaining("chk_compatibility_score");
        }

        @Test
        @DisplayName("Should validate health score range")
        void testValidateHealthScore() {
            // Given
            BuddyPartnership partnership = testDataBuilder.buildBuddyPartnership()
                .withUser1Id(UUID.randomUUID())
                .withUser2Id(UUID.randomUUID())
                .withHealthScore(BigDecimal.valueOf(-0.1)) // Invalid: < 0.0
                .build();

            // When/Then
            assertThatThrownBy(() -> {
                partnershipRepository.saveAndFlush(partnership);
            }).isInstanceOf(DataIntegrityViolationException.class)
              .hasMessageContaining("chk_health_score");
        }

        @Test
        @DisplayName("Should validate required fields")
        void testRequiredFieldsValidation() {
            // Given
            BuddyPartnership partnership = testDataBuilder.buildBuddyPartnership()
                .withUser1Id(null) // Required field is null
                .withUser2Id(UUID.randomUUID())
                .build();

            // When/Then
            assertThatThrownBy(() -> {
                partnershipRepository.saveAndFlush(partnership);
            }).isInstanceOf(DataIntegrityViolationException.class);
        }
    }

    @Nested
    @DisplayName("Performance and Concurrency")
    class PerformanceAndConcurrency {

        @Test
        @DisplayName("Should handle bulk partnership operations")
        void testBulkPartnershipOperations() {
            // Given
            List<BuddyPartnership> partnerships = testDataBuilder.buildBuddyPartnerships(100);

            // When
            long startTime = System.currentTimeMillis();
            List<BuddyPartnership> savedPartnerships = partnershipRepository.saveAll(partnerships);
            long endTime = System.currentTimeMillis();

            // Then
            assertThat(savedPartnerships).hasSize(100);
            assertThat(endTime - startTime).isLessThan(5000); // Should complete within 5 seconds

            // Verify all were saved correctly
            long count = partnershipRepository.count();
            assertThat(count).isGreaterThanOrEqualTo(100);
        }

        @Test
        @DisplayName("Should handle concurrent partnership updates")
        void testConcurrentPartnershipUpdates() {
            // Given
            BuddyPartnership partnership = testDataBuilder.buildBuddyPartnership()
                .withUser1Id(UUID.randomUUID())
                .withUser2Id(UUID.randomUUID())
                .withHealthScore(BigDecimal.valueOf(1.00))
                .build();
            BuddyPartnership savedPartnership = partnershipRepository.saveAndFlush(partnership);

            // When - Simulate optimistic locking conflict by ensuring separate entity instances
            BuddyPartnership p1 = partnershipRepository.findById(savedPartnership.getId()).orElseThrow();
            BuddyPartnership p2 = partnershipRepository.findById(savedPartnership.getId()).orElseThrow();

            // Detach p2 to ensure it has a stale version
            entityManager.detach(p2);

            System.out.println("Initial versions - p1: " + p1.getVersion() + ", p2: " + p2.getVersion());

            // Modify first entity and save (this will increment the version)
            p1.setHealthScore(BigDecimal.valueOf(0.8));
            BuddyPartnership savedP1 = partnershipRepository.saveAndFlush(p1);
            System.out.println("After first save - savedP1 version: " + savedP1.getVersion());

            // Now try to save the second entity which should have stale version
            p2.setHealthScore(BigDecimal.valueOf(0.9));
            System.out.println("Before second save - p2 version: " + p2.getVersion());

            // Then - Second save should fail with optimistic locking
            assertThatThrownBy(() -> {
                BuddyPartnership savedP2 = partnershipRepository.saveAndFlush(p2);
                System.out.println("Second save succeeded - savedP2 version: " + savedP2.getVersion());
            }).isInstanceOf(ObjectOptimisticLockingFailureException.class);
        }

        @Test
        @DisplayName("Should support partnership pagination")
        void testPartnershipPagination() {
            // Given
            List<BuddyPartnership> partnerships = testDataBuilder.buildBuddyPartnerships(25);
            partnershipRepository.saveAll(partnerships);

            // When
            Pageable pageable = PageRequest.of(0, 10);
            Page<BuddyPartnership> firstPage = partnershipRepository.findAll(pageable);

            Pageable secondPageable = PageRequest.of(1, 10);
            Page<BuddyPartnership> secondPage = partnershipRepository.findAll(secondPageable);

            // Then
            assertThat(firstPage.getContent()).hasSize(10);
            assertThat(firstPage.getTotalElements()).isGreaterThanOrEqualTo(25);
            assertThat(firstPage.getTotalPages()).isGreaterThanOrEqualTo(3);

            assertThat(secondPage.getContent()).hasSize(10);
            assertThat(secondPage.getNumber()).isEqualTo(1);

            // Verify no overlap between pages
            List<UUID> firstPageIds = firstPage.getContent().stream()
                .map(BuddyPartnership::getId)
                .toList();
            List<UUID> secondPageIds = secondPage.getContent().stream()
                .map(BuddyPartnership::getId)
                .toList();

            assertThat(firstPageIds).doesNotContainAnyElementsOf(secondPageIds);
        }
    }
}