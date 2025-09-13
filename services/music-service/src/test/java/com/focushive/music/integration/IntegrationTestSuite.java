package com.focushive.music.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.platform.suite.api.IncludeClassNamePatterns;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;

/**
 * Integration test suite for Music Service.
 * 
 * This suite runs all integration tests following TDD approach:
 * - Tests define expected behavior before implementation
 * - Comprehensive coverage of music service functionality
 * - Real-world scenarios with TestContainers and WireMock
 * 
 * Test Categories:
 * 1. SpotifyAuthIntegrationTest - OAuth flow and token management
 * 2. PlaylistIntegrationTest - CRUD operations with database persistence
 * 3. SpotifyApiMockIntegrationTest - External API interactions with mocking
 * 4. CollaborativePlaylistIntegrationTest - WebSocket real-time features
 * 5. RateLimitingIntegrationTest - API rate limiting enforcement
 * 
 * To run all integration tests:
 * ./gradlew test --tests "*.integration.*"
 * 
 * To run specific test class:
 * ./gradlew test --tests "SpotifyAuthIntegrationTest"
 * 
 * Test Infrastructure:
 * - TestContainers for PostgreSQL and Redis
 * - WireMock for external API simulation
 * - Spring Boot test framework
 * - WebSocket testing with STOMP client
 */
@Suite
@SuiteDisplayName("Music Service Integration Test Suite")
@SelectPackages("com.focushive.music.integration")
@IncludeClassNamePatterns(".*IntegrationTest")
public class IntegrationTestSuite {
    // Test suite configuration only - no implementation needed
}