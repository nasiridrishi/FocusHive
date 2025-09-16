package com.focushive.hive.repository;

import com.focushive.hive.entity.Hive;
import com.focushive.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprehensive Repository tests for HiveRepository.
 * Following TDD approach - these tests will initially FAIL until implementation is complete.
 */
@DataJpaTest
@ActiveProfiles("test")
class HiveRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private HiveRepository hiveRepository;

    private User testOwner;
    private User otherUser;
    private Hive activePublicHive;
    private Hive activePrivateHive;
    private Hive inactiveHive;
    private Hive deletedHive;

    @BeforeEach
    void setUp() {
        // Create test users
        testOwner = new User();
        testOwner.setId(UUID.randomUUID().toString());
        testOwner.setUsername("owner");
        testOwner.setEmail("owner@test.com");
        testOwner = entityManager.persistAndFlush(testOwner);

        otherUser = new User();
        otherUser.setId(UUID.randomUUID().toString());
        otherUser.setUsername("other");
        otherUser.setEmail("other@test.com");
        otherUser = entityManager.persistAndFlush(otherUser);

        // Create test hives with different states
        activePublicHive = createHive("Active Public Hive", "active-public", testOwner, true, true);
        activePrivateHive = createHive("Active Private Hive", "active-private", testOwner, false, true);
        inactiveHive = createHive("Inactive Hive", "inactive", testOwner, true, false);

        // Create deleted hive
        deletedHive = createHive("Deleted Hive", "deleted", testOwner, true, true);
        deletedHive.setDeletedAt(LocalDateTime.now().minusDays(1));
        deletedHive = entityManager.persistAndFlush(deletedHive);

        entityManager.clear();
    }

    private Hive createHive(String name, String slug, User owner, boolean isPublic, boolean isActive) {
        Hive hive = new Hive();
        hive.setId(UUID.randomUUID().toString());
        hive.setName(name);
        hive.setSlug(slug);
        hive.setDescription(name + " description");
        hive.setOwner(owner);
        hive.setMaxMembers(10);
        hive.setIsPublic(isPublic);
        hive.setIsActive(isActive);
        hive.setType(Hive.HiveType.STUDY);
        hive.setMemberCount(1);
        hive.setTotalFocusMinutes(0L);
        hive.setTags(new String[]{"test", "study"});
        return entityManager.persistAndFlush(hive);
    }

    // ===== BASIC CRUD TESTS =====

    @Test
    void shouldSaveHiveToDatabase() {
        // ARRANGE
        Hive newHive = new Hive();
        newHive.setId(UUID.randomUUID().toString());
        newHive.setName("New Test Hive");
        newHive.setSlug("new-test-hive");
        newHive.setDescription("A new test hive");
        newHive.setOwner(testOwner);
        newHive.setMaxMembers(20);
        newHive.setIsPublic(true);
        newHive.setIsActive(true);
        newHive.setType(Hive.HiveType.WORK);
        newHive.setMemberCount(0);

        // ACT
        Hive saved = hiveRepository.save(newHive);

        // ASSERT
        assertThat(saved).isNotNull();
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getName()).isEqualTo("New Test Hive");
        assertThat(saved.getSlug()).isEqualTo("new-test-hive");
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    void shouldFindHiveById() {
        // ACT
        Optional<Hive> found = hiveRepository.findById(activePublicHive.getId());

        // ASSERT
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Active Public Hive");
        assertThat(found.get().getSlug()).isEqualTo("active-public");
    }

    @Test
    void shouldUpdateHiveDetails() {
        // ARRANGE
        String newName = "Updated Hive Name";
        String newDescription = "Updated description";

        // ACT
        activePublicHive.setName(newName);
        activePublicHive.setDescription(newDescription);
        Hive updated = hiveRepository.save(activePublicHive);

        // ASSERT
        assertThat(updated.getName()).isEqualTo(newName);
        assertThat(updated.getDescription()).isEqualTo(newDescription);
        assertThat(updated.getUpdatedAt()).isAfter(updated.getCreatedAt());
    }

    @Test
    void shouldDeleteHive() {
        // ACT
        hiveRepository.delete(activePublicHive);

        // ASSERT
        Optional<Hive> deleted = hiveRepository.findById(activePublicHive.getId());
        assertThat(deleted).isEmpty();
    }

    // ===== QUERY METHOD TESTS =====

    @Test
    void shouldFindHivesByUserId() {
        // ACT
        Pageable pageable = PageRequest.of(0, 10);
        Page<Hive> userHives = hiveRepository.findByOwnerId(testOwner.getId(), pageable);

        // ASSERT
        assertThat(userHives).isNotNull();
        assertThat(userHives.getContent()).hasSize(4); // Including deleted one
        assertThat(userHives.getContent()).extracting("name")
            .contains("Active Public Hive", "Active Private Hive", "Inactive Hive", "Deleted Hive");
    }

    @Test
    void shouldFindPublicHives() {
        // ACT
        Pageable pageable = PageRequest.of(0, 10);
        Page<Hive> publicHives = hiveRepository.findPublicHives(pageable);

        // ASSERT
        assertThat(publicHives).isNotNull();
        assertThat(publicHives.getContent()).hasSize(2); // Only active public hives, not deleted
        assertThat(publicHives.getContent()).extracting("name")
            .contains("Active Public Hive", "Inactive Hive");
        assertThat(publicHives.getContent()).noneMatch(h -> !h.getIsPublic());
    }

    @Test
    void shouldFindActiveHives() {
        // ACT
        Optional<Hive> found = hiveRepository.findByIdAndActive(activePublicHive.getId());
        Optional<Hive> notFound = hiveRepository.findByIdAndActive(inactiveHive.getId());

        // ASSERT
        assertThat(found).isPresent();
        assertThat(found.get().getIsActive()).isTrue();
        assertThat(notFound).isEmpty();
    }

    @Test
    void shouldFindHivesByStatus() {
        // Test active hives
        long activeCount = hiveRepository.countByIsActiveAndDeletedAtIsNull(true);
        long inactiveCount = hiveRepository.countByIsActiveAndDeletedAtIsNull(false);

        // ASSERT
        assertThat(activeCount).isEqualTo(2); // Two active hives
        assertThat(inactiveCount).isEqualTo(1); // One inactive hive
    }

    @Test
    void shouldHandleConcurrentUpdates() {
        // ARRANGE - Simulate concurrent updates using version
        Hive hive1 = hiveRepository.findById(activePublicHive.getId()).get();
        Hive hive2 = hiveRepository.findById(activePublicHive.getId()).get();

        // ACT - First update
        hive1.setName("First Update");
        hiveRepository.save(hive1);

        // Second update on same version
        hive2.setName("Second Update");

        // ASSERT - This will test optimistic locking if implemented
        // For now, just verify the last save wins
        Hive saved = hiveRepository.save(hive2);
        assertThat(saved.getName()).isEqualTo("Second Update");
    }

    @Test
    void shouldEnforceUniqueConstraints() {
        // ARRANGE
        String existingSlug = activePublicHive.getSlug();

        // ACT & ASSERT
        boolean exists = hiveRepository.existsBySlug(existingSlug);
        assertThat(exists).isTrue();

        boolean notExists = hiveRepository.existsBySlug("non-existent-slug");
        assertThat(notExists).isFalse();
    }

    // ===== ADVANCED QUERY TESTS (Will initially fail until implemented) =====

    @Test
    void shouldFindHivesBySlug() {
        // ACT
        Optional<Hive> found = hiveRepository.findBySlug("active-public");

        // ASSERT
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Active Public Hive");
    }

    @Test
    void shouldFindActiveHivesBySlug() {
        // ACT
        Optional<Hive> activeFound = hiveRepository.findBySlugAndActive("active-public");
        Optional<Hive> inactiveNotFound = hiveRepository.findBySlugAndActive("inactive");

        // ASSERT
        assertThat(activeFound).isPresent();
        assertThat(inactiveNotFound).isEmpty();
    }

    @Test
    void shouldSearchPublicHives() {
        // ACT
        Pageable pageable = PageRequest.of(0, 10);
        Page<Hive> searchResults = hiveRepository.searchPublicHives("Active", pageable);

        // ASSERT
        assertThat(searchResults).isNotNull();
        assertThat(searchResults.getContent()).hasSize(1);
        assertThat(searchResults.getContent().get(0).getName()).contains("Active");
    }

    @Test
    void shouldFindHivesByType() {
        // ACT
        Pageable pageable = PageRequest.of(0, 10);
        Page<Hive> studyHives = hiveRepository.findByType(Hive.HiveType.STUDY, pageable);

        // ASSERT
        assertThat(studyHives).isNotNull();
        assertThat(studyHives.getContent()).allMatch(h -> h.getType() == Hive.HiveType.STUDY);
    }

    @Test
    void shouldFindPopularHives() {
        // ARRANGE - Set different member counts
        activePublicHive.setMemberCount(10);
        activePrivateHive.setMemberCount(5);
        hiveRepository.save(activePublicHive);
        hiveRepository.save(activePrivateHive);

        // ACT
        Pageable pageable = PageRequest.of(0, 10);
        Page<Hive> popularHives = hiveRepository.findPopularHives(pageable);

        // ASSERT
        assertThat(popularHives).isNotNull();
        assertThat(popularHives.getContent()).isNotEmpty();
        // Should be ordered by member count descending
        if (popularHives.getContent().size() > 1) {
            assertThat(popularHives.getContent().get(0).getMemberCount())
                .isGreaterThanOrEqualTo(popularHives.getContent().get(1).getMemberCount());
        }
    }

    @Test
    void shouldFindRecentHives() {
        // ACT
        Pageable pageable = PageRequest.of(0, 10);
        Page<Hive> recentHives = hiveRepository.findRecentHives(pageable);

        // ASSERT
        assertThat(recentHives).isNotNull();
        assertThat(recentHives.getContent()).isNotEmpty();
        // Should be ordered by creation date descending
        if (recentHives.getContent().size() > 1) {
            assertThat(recentHives.getContent().get(0).getCreatedAt())
                .isAfterOrEqualTo(recentHives.getContent().get(1).getCreatedAt());
        }
    }

    @Test
    void shouldUpdateStatistics() {
        // ARRANGE
        String hiveId = activePublicHive.getId();
        Long additionalMinutes = 30L;
        Long originalMinutes = activePublicHive.getTotalFocusMinutes();

        // ACT
        hiveRepository.incrementTotalFocusMinutes(hiveId, additionalMinutes);
        entityManager.flush();
        entityManager.clear();

        // ASSERT
        Hive updated = hiveRepository.findById(hiveId).get();
        assertThat(updated.getTotalFocusMinutes()).isEqualTo(originalMinutes + additionalMinutes);
    }

    @Test
    void shouldCountActiveHives() {
        // ACT
        long activeCount = hiveRepository.countActiveHives();

        // ASSERT
        assertThat(activeCount).isEqualTo(2); // Two active hives
    }

    @Test
    void shouldCountPublicHives() {
        // ACT
        long publicCount = hiveRepository.countPublicHives();

        // ASSERT
        assertThat(publicCount).isEqualTo(2); // Two public hives (one active, one inactive)
    }

    // ===== PERFORMANCE AND EDGE CASE TESTS =====

    @Test
    void shouldHandleEmptyResults() {
        // ACT
        Page<Hive> emptyResults = hiveRepository.searchPublicHives("nonexistent", PageRequest.of(0, 10));

        // ASSERT
        assertThat(emptyResults).isNotNull();
        assertThat(emptyResults.getContent()).isEmpty();
        assertThat(emptyResults.getTotalElements()).isZero();
    }

    @Test
    void shouldHandleLargePages() {
        // ACT
        Pageable largePage = PageRequest.of(0, 1000);
        Page<Hive> results = hiveRepository.findPublicHives(largePage);

        // ASSERT
        assertThat(results).isNotNull();
        assertThat(results.getContent().size()).isLessThanOrEqualTo(1000);
    }

    @Test
    void shouldHandleSpecialCharactersInSearch() {
        // ARRANGE
        Hive specialHive = createHive("Special & <Characters>", "special-chars", testOwner, true, true);

        // ACT
        Page<Hive> results = hiveRepository.searchPublicHives("Special", PageRequest.of(0, 10));

        // ASSERT
        assertThat(results.getContent()).anyMatch(h -> h.getName().contains("Special"));
    }

    // ===== MISSING METHOD TESTS (Will fail until implemented) =====

    @Test
    void shouldCountByIsActiveAndDeletedAtIsNull() {
        // ACT & ASSERT
        assertThat(hiveRepository.countByIsActiveAndDeletedAtIsNull(true)).isEqualTo(2);
        assertThat(hiveRepository.countByIsActiveAndDeletedAtIsNull(false)).isEqualTo(1);
    }
}