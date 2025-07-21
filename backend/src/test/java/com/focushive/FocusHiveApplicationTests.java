package com.focushive;

import com.focushive.test.TestApplication;
import com.focushive.test.config.TestSecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = TestApplication.class)
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
class FocusHiveApplicationTests {

    @Test
    void contextLoads() {
    }

}