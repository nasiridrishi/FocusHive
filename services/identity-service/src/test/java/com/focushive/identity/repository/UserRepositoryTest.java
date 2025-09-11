package com.focushive.identity.repository;

import com.focushive.identity.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.test.context.TestPropertySource;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for UserRepository using @DataJpaTest.
 * Tests repository methods with H2 in-memory database.
 */
@DataJpaTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@EntityScan(basePackages = "com.focushive.identity.entity")
class UserRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .username("testuser")
                .email("test@example.com")
                .password("encoded-password")
                .firstName("Test")
                .lastName("User")
                .emailVerified(true)
                .enabled(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("Should find user by username")
    void findByUsername_ExistingUser_ShouldReturnUser() {
        // Given
        entityManager.persistAndFlush(testUser);

        // When
        Optional<User> result = userRepository.findByUsername("testuser");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getUsername()).isEqualTo("testuser");
        assertThat(result.get().getEmail()).isEqualTo("test@example.com");
        assertThat(result.get().getFirstName()).isEqualTo("Test");
        assertThat(result.get().getLastName()).isEqualTo("User");
    }

    @Test
    @DisplayName("Should return empty when user not found by username")
    void findByUsername_NonExistentUser_ShouldReturnEmpty() {
        // When
        Optional<User> result = userRepository.findByUsername("nonexistent");

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should find user by email")
    void findByEmail_ExistingUser_ShouldReturnUser() {
        // Given
        entityManager.persistAndFlush(testUser);

        // When
        Optional<User> result = userRepository.findByEmail("test@example.com");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getEmail()).isEqualTo("test@example.com");
        assertThat(result.get().getUsername()).isEqualTo("testuser");
    }

    @Test
    @DisplayName("Should return empty when user not found by email")
    void findByEmail_NonExistentUser_ShouldReturnEmpty() {
        // When
        Optional<User> result = userRepository.findByEmail("nonexistent@example.com");

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should check if username exists")
    void existsByUsername_ExistingUser_ShouldReturnTrue() {
        // Given
        entityManager.persistAndFlush(testUser);

        // When
        boolean exists = userRepository.existsByUsername("testuser");

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("Should return false when username does not exist")
    void existsByUsername_NonExistentUser_ShouldReturnFalse() {
        // When
        boolean exists = userRepository.existsByUsername("nonexistent");

        // Then
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("Should check if email exists")
    void existsByEmail_ExistingUser_ShouldReturnTrue() {
        // Given
        entityManager.persistAndFlush(testUser);

        // When
        boolean exists = userRepository.existsByEmail("test@example.com");

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("Should return false when email does not exist")
    void existsByEmail_NonExistentUser_ShouldReturnFalse() {
        // When
        boolean exists = userRepository.existsByEmail("nonexistent@example.com");

        // Then
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("Should find user by username or email with username")
    void findByUsernameOrEmail_WithUsername_ShouldReturnUser() {
        // Given
        entityManager.persistAndFlush(testUser);

        // When
        Optional<User> result = userRepository.findByUsernameOrEmail("testuser", "testuser");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getUsername()).isEqualTo("testuser");
    }

    @Test
    @DisplayName("Should find user by username or email with email")
    void findByUsernameOrEmail_WithEmail_ShouldReturnUser() {
        // Given
        entityManager.persistAndFlush(testUser);

        // When
        Optional<User> result = userRepository.findByUsernameOrEmail("test@example.com", "test@example.com");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getEmail()).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("Should return empty when neither username nor email exists")
    void findByUsernameOrEmail_NonExistent_ShouldReturnEmpty() {
        // When
        Optional<User> result = userRepository.findByUsernameOrEmail("nonexistent", "nonexistent@example.com");

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should save user successfully")
    void save_NewUser_ShouldPersistUser() {
        // When
        User saved = userRepository.save(testUser);

        // Then
        assertThat(saved).isNotNull();
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getUsername()).isEqualTo("testuser");
        assertThat(saved.getEmail()).isEqualTo("test@example.com");
        assertThat(saved.getCreatedAt()).isNotNull();

        // Verify in database
        User found = entityManager.find(User.class, saved.getId());
        assertThat(found).isNotNull();
        assertThat(found.getUsername()).isEqualTo("testuser");
    }

    @Test
    @DisplayName("Should update user successfully")
    void save_ExistingUser_ShouldUpdateUser() {
        // Given
        User saved = entityManager.persistAndFlush(testUser);
        saved.setFirstName("Updated");
        saved.setLastName("Name");
        saved.setUpdatedAt(Instant.now());

        // When
        User updated = userRepository.save(saved);

        // Then
        assertThat(updated.getId()).isEqualTo(saved.getId());
        assertThat(updated.getFirstName()).isEqualTo("Updated");
        assertThat(updated.getLastName()).isEqualTo("Name");

        // Verify in database
        entityManager.clear();
        User found = entityManager.find(User.class, saved.getId());
        assertThat(found.getFirstName()).isEqualTo("Updated");
        assertThat(found.getLastName()).isEqualTo("Name");
    }

    @Test
    @DisplayName("Should delete user successfully")
    void delete_ExistingUser_ShouldRemoveUser() {
        // Given
        User saved = entityManager.persistAndFlush(testUser);
        entityManager.clear();

        // When
        userRepository.deleteById(saved.getId());

        // Then
        User found = entityManager.find(User.class, saved.getId());
        assertThat(found).isNull();
    }

    @Test
    @DisplayName("Should handle concurrent access gracefully")
    void save_ConcurrentAccess_ShouldHandleGracefully() {
        // Given
        User saved = entityManager.persistAndFlush(testUser);
        
        // Simulate concurrent modification
        User user1 = userRepository.findById(saved.getId()).orElseThrow();
        User user2 = userRepository.findById(saved.getId()).orElseThrow();
        
        user1.setFirstName("First Update");
        user2.setLastName("Second Update");

        // When
        userRepository.save(user1);
        userRepository.save(user2);

        // Then
        entityManager.clear();
        User result = userRepository.findById(saved.getId()).orElseThrow();
        assertThat(result.getLastName()).isEqualTo("Second Update");
    }

    @Test
    @DisplayName("Should enforce username uniqueness constraint")
    void save_DuplicateUsername_ShouldThrowException() {
        // Given
        entityManager.persistAndFlush(testUser);
        
        User duplicateUser = User.builder()
                .username("testuser") // Same username
                .email("different@example.com")
                .password("password")
                .firstName("Different")
                .lastName("User")
                .emailVerified(true)
                .enabled(true)
                .createdAt(Instant.now())
                .build();

        // When & Then
        assertThatThrownBy(() -> {
            entityManager.persistAndFlush(duplicateUser);
        }).isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("Should enforce email uniqueness constraint")
    void save_DuplicateEmail_ShouldThrowException() {
        // Given
        entityManager.persistAndFlush(testUser);
        
        User duplicateUser = User.builder()
                .username("differentuser")
                .email("test@example.com") // Same email
                .password("password")
                .firstName("Different")
                .lastName("User")
                .emailVerified(true)
                .enabled(true)
                .createdAt(Instant.now())
                .build();

        // When & Then
        assertThatThrownBy(() -> {
            entityManager.persistAndFlush(duplicateUser);
        }).isInstanceOf(Exception.class);
    }
}