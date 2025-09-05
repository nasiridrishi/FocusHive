package com.focushive.identity.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.junit.jupiter.SpringJUnitExtension;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test cases for DataPermission entity
 */
@ExtendWith(SpringJUnitExtension.class)
@DataJpaTest
class DataPermissionTest {

    @Autowired
    private TestEntityManager entityManager;

    private User testUser;
    private OAuthClient testClient;

    @BeforeEach
    void setUp() {
        // Create test user
        testUser = User.builder()
                .username("testuser")
                .email("test@example.com")
                .password("hashedpassword")
                .displayName("Test User")
                .build();
        entityManager.persistAndFlush(testUser);

        // Create test OAuth client
        testClient = OAuthClient.builder()
                .clientId("test-client-id")
                .clientSecret("secret")
                .clientName("Test Client")
                .user(testUser)
                .build();
        entityManager.persistAndFlush(testClient);
    }

    @Test
    void shouldCreateDataPermissionForClient() {
        // Given
        DataPermission permission = DataPermission.builder()
                .user(testUser)
                .client(testClient)
                .dataType("profile")
                .permissions(Set.of("read", "write"))
                .purpose("Access user profile for authentication")
                .retentionPeriodDays(365)
                .build();

        // When
        DataPermission savedPermission = entityManager.persistAndFlush(permission);

        // Then
        assertThat(savedPermission.getId()).isNotNull();
        assertThat(savedPermission.getUser()).isEqualTo(testUser);
        assertThat(savedPermission.getClient()).isEqualTo(testClient);
        assertThat(savedPermission.getDataType()).isEqualTo("profile");
        assertThat(savedPermission.getPermissions()).containsExactlyInAnyOrder("read", "write");
        assertThat(savedPermission.getPurpose()).isEqualTo("Access user profile for authentication");
        assertThat(savedPermission.getRetentionPeriodDays()).isEqualTo(365);
        assertThat(savedPermission.getCreatedAt()).isNotNull();
        assertThat(savedPermission.getUpdatedAt()).isNotNull();
        assertThat(savedPermission.isActive()).isTrue();
    }

    @Test
    void shouldCreateInternalDataPermission() {
        // Given
        DataPermission permission = DataPermission.builder()
                .user(testUser)
                .dataType("analytics")
                .permissions(Set.of("read", "aggregate"))
                .purpose("Internal analytics and reporting")
                .retentionPeriodDays(1095) // 3 years
                .isInternal(true)
                .build();

        // When
        DataPermission savedPermission = entityManager.persistAndFlush(permission);

        // Then
        assertThat(savedPermission.getClient()).isNull();
        assertThat(savedPermission.isInternal()).isTrue();
        assertThat(savedPermission.getDataType()).isEqualTo("analytics");
        assertThat(savedPermission.getRetentionPeriodDays()).isEqualTo(1095);
    }

    @Test
    void shouldSetDefaultValues() {
        // Given
        DataPermission permission = DataPermission.builder()
                .user(testUser)
                .client(testClient)
                .dataType("activity")
                .permissions(Set.of("read"))
                .purpose("Track user activity")
                .build();

        // When
        DataPermission savedPermission = entityManager.persistAndFlush(permission);

        // Then
        assertThat(savedPermission.isActive()).isTrue();
        assertThat(savedPermission.isInternal()).isFalse();
        assertThat(savedPermission.getRetentionPeriodDays()).isNull(); // Can be null for indefinite
        assertThat(savedPermission.getCreatedAt()).isNotNull();
        assertThat(savedPermission.getUpdatedAt()).isNotNull();
    }

    @Test
    void shouldHandlePermissionRevocation() {
        // Given
        DataPermission permission = DataPermission.builder()
                .user(testUser)
                .client(testClient)
                .dataType("contacts")
                .permissions(Set.of("read", "write"))
                .purpose("Manage user contacts")
                .retentionPeriodDays(180)
                .build();
        DataPermission savedPermission = entityManager.persistAndFlush(permission);

        // When
        savedPermission.setActive(false);
        savedPermission.setRevokedAt(Instant.now());
        savedPermission.setRevocationReason("User requested data removal");
        DataPermission updatedPermission = entityManager.persistAndFlush(savedPermission);

        // Then
        assertThat(updatedPermission.isActive()).isFalse();
        assertThat(updatedPermission.getRevokedAt()).isNotNull();
        assertThat(updatedPermission.getRevocationReason()).isEqualTo("User requested data removal");
    }

    @Test
    void shouldCheckDataRetentionExpiry() {
        // Given
        DataPermission permission = DataPermission.builder()
                .user(testUser)
                .client(testClient)
                .dataType("temporary_data")
                .permissions(Set.of("read"))
                .purpose("Temporary access")
                .retentionPeriodDays(30)
                .build();
        permission.setCreatedAt(Instant.now().minus(40, ChronoUnit.DAYS)); // 40 days ago
        
        // When
        DataPermission savedPermission = entityManager.persistAndFlush(permission);

        // Then
        assertThat(savedPermission.isRetentionExpired()).isTrue();
    }

    @Test
    void shouldNotExpireWhenNoRetentionPeriod() {
        // Given
        DataPermission permission = DataPermission.builder()
                .user(testUser)
                .client(testClient)
                .dataType("profile")
                .permissions(Set.of("read"))
                .purpose("Profile access")
                .retentionPeriodDays(null) // No retention limit
                .build();
        permission.setCreatedAt(Instant.now().minus(1000, ChronoUnit.DAYS)); // Very old

        // When
        DataPermission savedPermission = entityManager.persistAndFlush(permission);

        // Then
        assertThat(savedPermission.isRetentionExpired()).isFalse();
    }

    @Test
    void shouldHandleGranularPermissions() {
        // Given
        DataPermission permission = DataPermission.builder()
                .user(testUser)
                .client(testClient)
                .dataType("activity_data")
                .permissions(Set.of("read", "aggregate", "export"))
                .purpose("Activity analytics with export capability")
                .conditions(Map.of(
                    "anonymize", "true",
                    "geo_restrict", "EU",
                    "min_aggregation", "10"
                ))
                .retentionPeriodDays(730) // 2 years
                .build();

        // When
        DataPermission savedPermission = entityManager.persistAndFlush(permission);

        // Then
        assertThat(savedPermission.getPermissions()).containsExactlyInAnyOrder("read", "aggregate", "export");
        assertThat(savedPermission.getConditions()).containsEntry("anonymize", "true");
        assertThat(savedPermission.getConditions()).containsEntry("geo_restrict", "EU");
        assertThat(savedPermission.getConditions()).containsEntry("min_aggregation", "10");
    }

    @Test
    void shouldHandleAutomaticExpiry() {
        // Given
        DataPermission permission = DataPermission.builder()
                .user(testUser)
                .client(testClient)
                .dataType("session_data")
                .permissions(Set.of("read"))
                .purpose("Session tracking")
                .expiresAt(Instant.now().plus(1, ChronoUnit.HOURS))
                .build();

        // When
        DataPermission savedPermission = entityManager.persistAndFlush(permission);

        // Then
        assertThat(savedPermission.getExpiresAt()).isAfter(Instant.now());
        assertThat(savedPermission.isExpired()).isFalse();
    }

    @Test
    void shouldDetectExpiredPermissions() {
        // Given
        DataPermission permission = DataPermission.builder()
                .user(testUser)
                .client(testClient)
                .dataType("temporary_access")
                .permissions(Set.of("read"))
                .purpose("Temporary access")
                .expiresAt(Instant.now().minus(1, ChronoUnit.HOURS))
                .build();

        // When
        DataPermission savedPermission = entityManager.persistAndFlush(permission);

        // Then
        assertThat(savedPermission.isExpired()).isTrue();
    }

    @Test
    void shouldHandlePermissionInheritance() {
        // Given
        DataPermission parentPermission = DataPermission.builder()
                .user(testUser)
                .client(testClient)
                .dataType("user_data")
                .permissions(Set.of("read"))
                .purpose("Base user data access")
                .build();
        DataPermission savedParent = entityManager.persistAndFlush(parentPermission);

        DataPermission childPermission = DataPermission.builder()
                .user(testUser)
                .client(testClient)
                .dataType("user_activity")
                .permissions(Set.of("read", "write"))
                .purpose("Extended activity access")
                .parentPermission(savedParent)
                .build();

        // When
        DataPermission savedChild = entityManager.persistAndFlush(childPermission);

        // Then
        assertThat(savedChild.getParentPermission()).isEqualTo(savedParent);
        assertThat(savedChild.getPermissions()).containsExactlyInAnyOrder("read", "write");
    }
}