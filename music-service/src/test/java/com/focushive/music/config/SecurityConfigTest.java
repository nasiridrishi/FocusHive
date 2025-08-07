package com.focushive.music.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * Basic test for SecurityConfig to verify Spring Boot context loads.
 * 
 * @author FocusHive Development Team
 * @version 1.0.0
 */
@SpringBootTest
@TestPropertySource(properties = {
    "jwt.secret=thisisaverylongsecretkeythatshouldbeatleast256bitslongforsecurity",
    "cors.allowed-origins=http://localhost:3000",
    "cors.allowed-methods=GET,POST,PUT,DELETE,OPTIONS",
    "cors.allowed-headers=*",
    "cors.allow-credentials=true",
    "cors.max-age=3600"
})
class SecurityConfigTest {

    @Test
    void contextLoads() {
        // This test verifies that the Spring Boot application context loads successfully
        // with our security configuration
    }
}