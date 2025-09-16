package com.focushive.buddy;

import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

/**
 * Base class for integration tests that need MockMvc support.
 * Extends AbstractTestContainersTest to get PostgreSQL and Redis containers.
 */
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class BaseIntegrationTest extends AbstractTestContainersTest {
    // Containers are managed by AbstractTestContainersTest
}