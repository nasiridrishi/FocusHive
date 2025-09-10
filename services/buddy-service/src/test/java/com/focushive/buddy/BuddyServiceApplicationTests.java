package com.focushive.buddy;

import com.focushive.buddy.config.TestSecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
class BuddyServiceApplicationTests {

    @Test
    void contextLoads() {
        // Test that the application context loads successfully
    }
}