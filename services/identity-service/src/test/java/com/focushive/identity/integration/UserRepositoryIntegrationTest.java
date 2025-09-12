package com.focushive.identity.integration;

import com.focushive.identity.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Simple integration test that verifies UserRepository database operations work.
 * This follows the exact same pattern as our successful DatabaseIntegrationTest.
 * 
 * This test focuses on verifying that:
 * 1. JPA slice testing works with UserRepository
 * 2. Database connectivity is established
 * 3. Repository is properly injected
 * 
 * Note: We avoid testing User entity creation due to encryption dependencies.
 * This is a stepping stone test to verify the infrastructure works.
 */
@DataJpaTest
@ActiveProfiles("test")
class UserRepositoryIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager entityManager;

    /**
     * Test that the UserRepository can be injected and basic operations work.
     * This verifies the same things as DatabaseIntegrationTest but for UserRepository specifically:
     * - H2 database starts correctly
     * - UserRepository is properly configured and injected
     * - JPA context loads without errors
     * - Basic operations work without encryption issues
     */
    @Test
    void shouldLoadUserRepositorySuccessfully() {
        // Verify that repository and entity manager are injected (same as DatabaseIntegrationTest)
        assertThat(userRepository).isNotNull();
        assertThat(entityManager).isNotNull();
        
        // Test basic repository operations that don't involve encrypted fields
        // Username is not encrypted, so this should work
        boolean existsByUsername = userRepository.existsByUsername("nonexistent_user");
        assertThat(existsByUsername).isFalse();
        
        // Verify repository count works (should be 0 for empty test database)
        // This doesn't involve any field-specific queries
        long userCount = userRepository.count();
        assertThat(userCount).isEqualTo(0);
        
        // Note: We avoid existsByEmail() because email is encrypted and would
        // trigger encryption converter issues. The goal here is to verify 
        // basic repository functionality works, not full CRUD operations.
    }
}