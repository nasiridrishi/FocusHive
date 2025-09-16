package com.focushive.buddy;

import com.focushive.buddy.config.TestSecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

/**
 * Basic application context loading test.
 * Extends AbstractTestContainersTest to get proper database and cache setup.
 */
@Import(TestSecurityConfig.class)
class BuddyServiceApplicationTests extends AbstractTestContainersTest {

    @Test
    void contextLoads() {
        // Test that the application context loads successfully with containers
    }
}