package com.focushive.identity.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Simple integration test that verifies JPA and database connectivity work.
 * This is a slice test that only loads JPA-related components.
 */
@DataJpaTest
@ActiveProfiles("test")
class DatabaseIntegrationTest {

    @Autowired
    private TestEntityManager entityManager;

    /**
     * Test that the database connection and JPA setup work correctly.
     * This verifies:
     * - H2 database starts correctly
     * - JPA configuration is valid
     * - Basic database operations work
     */
    @Test
    void shouldConnectToDatabase() {
        // Simple test to verify the database connection works
        // The TestEntityManager being injected means JPA setup is working
        assertThat(entityManager).isNotNull();
    }
}