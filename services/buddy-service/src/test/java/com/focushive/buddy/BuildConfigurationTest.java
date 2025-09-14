package com.focushive.buddy;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Simple test to validate build configuration and JaCoCo setup
 */
class BuildConfigurationTest {

    @Test
    void testBuildConfiguration() {
        // Simple test to validate that JUnit 5 is working
        assertTrue(true, "Build configuration is working");
    }

    @Test
    void testJavaVersion() {
        String javaVersion = System.getProperty("java.version");
        assertTrue(javaVersion.startsWith("21"),
            "Expected Java 21, but found: " + javaVersion);
    }

    @Test
    void testTestProfile() {
        String activeProfiles = System.getProperty("spring.profiles.active");
        assertTrue("test".equals(activeProfiles),
            "Expected test profile to be active, but found: " + activeProfiles);
    }
}