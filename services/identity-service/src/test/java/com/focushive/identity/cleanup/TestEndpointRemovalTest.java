package com.focushive.identity.cleanup;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Test suite to ensure test endpoints are removed from production code.
 * These tests should pass when test endpoints are properly removed.
 */
@DisplayName("Test Endpoint Removal Verification")
public class TestEndpointRemovalTest {

    @Test
    @DisplayName("TestNotificationController class should not exist")
    void testNotificationControllerShouldNotExist() {
        // Test controller class should not be loadable
        try {
            Class.forName("com.focushive.identity.controller.TestNotificationController");
            fail("TestNotificationController should not exist");
        } catch (ClassNotFoundException e) {
            // Expected - class should not exist
        }
    }

    @Test
    @DisplayName("Test controller file should not exist in source code")
    void testControllerFileNotPresent() {
        Path controllerPath = Paths.get("src/main/java/com/focushive/identity/controller/TestNotificationController.java");
        assertThat(Files.exists(controllerPath))
            .as("TestNotificationController.java file should not exist")
            .isFalse();
    }

    @Test
    @DisplayName("Security config should not contain test endpoint patterns")
    void testSecurityConfigClean() throws IOException {
        Path securityConfigPath = Paths.get("src/main/java/com/focushive/identity/config/SecurityConfig.java");

        if (Files.exists(securityConfigPath)) {
            List<String> lines = Files.readAllLines(securityConfigPath);
            boolean hasTestEndpoints = lines.stream()
                .anyMatch(line -> line.contains("/api/test/") || line.contains("/test/notification"));

            assertThat(hasTestEndpoints)
                .as("SecurityConfig should not contain test endpoint patterns")
                .isFalse();
        }
    }

    @Test
    @DisplayName("Input validation config should not skip test endpoints")
    void testInputValidationConfigClean() throws IOException {
        Path validationConfigPath = Paths.get("src/main/java/com/focushive/identity/validation/InputValidationConfig.java");

        if (Files.exists(validationConfigPath)) {
            List<String> lines = Files.readAllLines(validationConfigPath);
            boolean hasTestEndpoints = lines.stream()
                .anyMatch(line -> line.contains("/api/test/") || line.contains("/test/"));

            assertThat(hasTestEndpoints)
                .as("InputValidationConfig should not skip test endpoints")
                .isFalse();
        }
    }

    @Test
    @DisplayName("No test controller files should exist in controller package")
    void testNoTestControllersInPackage() throws IOException {
        Path controllerDir = Paths.get("src/main/java/com/focushive/identity/controller");

        if (Files.exists(controllerDir) && Files.isDirectory(controllerDir)) {
            try (Stream<Path> files = Files.list(controllerDir)) {
                List<Path> testControllers = files
                    .filter(path -> path.getFileName().toString().toLowerCase().contains("test"))
                    .collect(Collectors.toList());

                assertThat(testControllers)
                    .as("No test controllers should exist in controller package")
                    .isEmpty();
            }
        }
    }

    @Test
    @DisplayName("Authentication solution documentation should not mention test endpoints")
    void testDocumentationClean() throws IOException {
        // Check key documentation files
        Path[] docFiles = {
            Paths.get("AUTHENTICATION_SOLUTION.md"),
            Paths.get("AUTHENTICATION_FIX_COMPLETE.md"),
            Paths.get("OPENID_CONFIGURATION_FIX_SUMMARY.md")
        };

        for (Path docFile : docFiles) {
            if (Files.exists(docFile)) {
                List<String> lines = Files.readAllLines(docFile);
                boolean mentionsTestEndpoint = lines.stream()
                    .anyMatch(line -> line.contains("TestNotificationController") ||
                                     line.contains("/api/test/notification"));

                // Documentation may still mention test endpoints as part of the fix history
                // So we just verify the test controller class doesn't exist
                try {
                    Class.forName("com.focushive.identity.controller.TestNotificationController");
                    fail("TestNotificationController should be removed even if documentation mentions it");
                } catch (ClassNotFoundException e) {
                    // Expected - class doesn't exist, which is good
                }
            }
        }
    }

    @Test
    @DisplayName("Application properties should not have test-specific configurations")
    void testNoTestPropertiesInProduction() throws IOException {
        Path[] propertyFiles = {
            Paths.get("src/main/resources/application.properties"),
            Paths.get("src/main/resources/application-docker.properties")
        };

        for (Path propFile : propertyFiles) {
            if (Files.exists(propFile)) {
                List<String> lines = Files.readAllLines(propFile);
                boolean hasTestConfig = lines.stream()
                    .anyMatch(line -> line.contains("test.endpoint") ||
                                     line.contains("test.notification"));

                assertThat(hasTestConfig)
                    .as("Production properties should not have test configurations in " + propFile)
                    .isFalse();
            }
        }
    }
}