package com.focushive.forum;

import com.focushive.forum.config.TestSecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
class ForumServiceApplicationTests {

    @Test
    void contextLoads() {
        // Test that the application context loads successfully
    }
}