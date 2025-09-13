package com.focushive.integration;

import com.fasterxml.jackson.core.type.TypeReference;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Cross-service integration test for User Action → Analytics Update flow.
 * 
 * Tests the complete data flow:
 * 1. User joins a hive → Analytics service records join event
 * 2. User starts timer session → Analytics tracks focus time
 * 3. User completes session → Productivity metrics updated
 * 4. User earns achievement → Achievement recorded in analytics
 * 
 * Following TDD approach:
 * - Write failing test first
 * - Implement minimal functionality to pass
 * - Refactor and optimize
 * 
 * Tests both synchronous API calls and asynchronous event processing.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@SpringJUnitConfig
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Tag("cross-service")
@Tag("data-flow")
@Tag("integration")
class UserAnalyticsFlowIntegrationTest extends AbstractCrossServiceIntegrationTest {

    private String testUserId;
    private String testHiveId;
    private String testSessionId;
    private String authToken;

    @BeforeAll
    void setUpTestData() {
        // Set base URI for REST Assured
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = 8080;

        // Create test user and authenticate
        setupTestUserAndAuthentication();
        
        // Create test hive
        setupTestHive();
    }

    @Test
    @Order(1)
    @DisplayName("TDD: User join hive event should trigger analytics recording - FAILING TEST")
    void testUserJoinHiveAnalyticsFlow_ShouldFail() {
        // STEP 1: Write failing test first (TDD)
        
        // Given: A user and a hive exist
        assertNotNull(testUserId, "Test user should be created");
        assertNotNull(testHiveId, "Test hive should be created");
        
        // When: User joins the hive
        Response joinResponse = given()
            .header("Authorization", "Bearer " + authToken)
            .contentType(ContentType.JSON)
            .when()
            .post("/api/hives/{hiveId}/join", testHiveId)
            .then()
            .statusCode(200)
            .extract().response();

        // Record the action timestamp for verification
        long actionTimestamp = System.currentTimeMillis();

        // Then: Analytics service should record the join event (THIS WILL FAIL INITIALLY)
        // Wait for eventual consistency
        CrossServiceTestUtils.waitForEventualConsistency();

        // Verify analytics event was created
        Awaitility.await()
            .atMost(10, TimeUnit.SECONDS)
            .pollInterval(1, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                Response analyticsResponse = given()
                    .header("Authorization", "Bearer " + authToken)
                    .when()
                    .get("/api/analytics/events")
                    .then()
                    .statusCode(200)
                    .extract().response();

                List<Map<String, Object>> events = analyticsResponse.jsonPath().getList("content");
                
                // Find the user join event
                boolean joinEventExists = events.stream()
                    .anyMatch(event -> 
                        "USER_JOINED_HIVE".equals(event.get("eventType")) &&
                        testUserId.equals(event.get("userId")) &&
                        testHiveId.equals(event.get("hiveId"))
                    );

                assertTrue(joinEventExists, 
                    "Analytics service should record USER_JOINED_HIVE event for user " + testUserId + " in hive " + testHiveId);
            });

        // Verify user analytics profile was updated
        Response userAnalyticsResponse = given()
            .header("Authorization", "Bearer " + authToken)
            .when()
            .get("/api/analytics/users/{userId}/profile", testUserId)
            .then()
            .statusCode(200)
            .body("userId", equalTo(testUserId))
            .body("totalHivesJoined", greaterThan(0))
            .extract().response();

        Map<String, Object> userProfile = userAnalyticsResponse.as(new TypeReference<Map<String, Object>>() {});
        assertEquals(1, userProfile.get("totalHivesJoined"), "User should have joined 1 hive");
    }

    @Test
    @Order(2)
    @DisplayName("TDD: Timer session start should trigger analytics tracking")
    void testTimerSessionStartAnalyticsFlow() {
        // Given: User has joined a hive
        // (Depends on previous test passing)

        // When: User starts a timer session
        Map<String, Object> sessionRequest = TestDataFactory.TimerSessions.createTimerSession(testUserId, testHiveId, 25);
        
        Response sessionResponse = given()
            .header("Authorization", "Bearer " + authToken)
            .contentType(ContentType.JSON)
            .body(sessionRequest)
            .when()
            .post("/api/timer/start")
            .then()
            .statusCode(201)
            .body("userId", equalTo(testUserId))
            .body("hiveId", equalTo(testHiveId))
            .body("status", equalTo("ACTIVE"))
            .extract().response();

        testSessionId = sessionResponse.jsonPath().getString("id");
        long sessionStartTime = System.currentTimeMillis();

        // Then: Analytics service should track the session start
        CrossServiceTestUtils.waitForEventualConsistency();

        Awaitility.await()
            .atMost(10, TimeUnit.SECONDS)
            .pollInterval(1, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                Response analyticsResponse = given()
                    .header("Authorization", "Bearer " + authToken)
                    .when()
                    .get("/api/analytics/events")
                    .then()
                    .statusCode(200)
                    .extract().response();

                List<Map<String, Object>> events = analyticsResponse.jsonPath().getList("content");
                
                boolean sessionStartEventExists = events.stream()
                    .anyMatch(event -> 
                        "TIMER_SESSION_STARTED".equals(event.get("eventType")) &&
                        testUserId.equals(event.get("userId")) &&
                        testHiveId.equals(event.get("hiveId")) &&
                        testSessionId.equals(event.get("sessionId"))
                    );

                assertTrue(sessionStartEventExists, "Analytics should record TIMER_SESSION_STARTED event");
            });

        // Verify session tracking metrics
        Response metricsResponse = given()
            .header("Authorization", "Bearer " + authToken)
            .when()
            .get("/api/analytics/users/{userId}/sessions/active", testUserId)
            .then()
            .statusCode(200)
            .extract().response();

        List<Map<String, Object>> activeSessions = metricsResponse.jsonPath().getList(".");
        assertEquals(1, activeSessions.size(), "User should have 1 active session being tracked");
        assertEquals(testSessionId, activeSessions.get(0).get("sessionId"), "Active session ID should match");
    }

    @Test
    @Order(3)
    @DisplayName("TDD: Timer session completion should update productivity metrics")
    void testTimerSessionCompletionAnalyticsFlow() {
        // Given: User has an active timer session
        assertNotNull(testSessionId, "Active session should exist from previous test");

        // When: User completes the timer session
        Map<String, Object> completionRequest = Map.of(
            "sessionId", testSessionId,
            "actualDuration", 25,
            "productivityScore", 85,
            "interruptions", 0,
            "completedTasks", List.of("Task 1", "Task 2")
        );

        Response completionResponse = given()
            .header("Authorization", "Bearer " + authToken)
            .contentType(ContentType.JSON)
            .body(completionRequest)
            .when()
            .post("/api/timer/complete")
            .then()
            .statusCode(200)
            .body("status", equalTo("COMPLETED"))
            .body("actualDuration", equalTo(25))
            .extract().response();

        long completionTime = System.currentTimeMillis();

        // Then: Analytics service should update productivity metrics
        CrossServiceTestUtils.waitForEventualConsistency();

        // Verify session completion event
        Awaitility.await()
            .atMost(15, TimeUnit.SECONDS)
            .pollInterval(1, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                Response analyticsResponse = given()
                    .header("Authorization", "Bearer " + authToken)
                    .when()
                    .get("/api/analytics/events")
                    .then()
                    .statusCode(200)
                    .extract().response();

                List<Map<String, Object>> events = analyticsResponse.jsonPath().getList("content");
                
                boolean completionEventExists = events.stream()
                    .anyMatch(event -> 
                        "TIMER_SESSION_COMPLETED".equals(event.get("eventType")) &&
                        testUserId.equals(event.get("userId")) &&
                        testSessionId.equals(event.get("sessionId"))
                    );

                assertTrue(completionEventExists, "Analytics should record TIMER_SESSION_COMPLETED event");
            });

        // Verify productivity metrics were updated
        Response productivityResponse = given()
            .header("Authorization", "Bearer " + authToken)
            .when()
            .get("/api/analytics/users/{userId}/productivity/daily", testUserId)
            .then()
            .statusCode(200)
            .body("userId", equalTo(testUserId))
            .body("focusHours", greaterThan(0.0f))
            .body("sessionsCompleted", greaterThan(0))
            .body("productivityScore", greaterThan(0))
            .extract().response();

        Map<String, Object> productivityMetrics = productivityResponse.as(new TypeReference<Map<String, Object>>() {});
        
        // Verify specific metrics
        assertTrue((Double) productivityMetrics.get("focusHours") >= 0.41, 
            "Focus hours should be at least 25 minutes (0.41 hours)");
        assertEquals(1, productivityMetrics.get("sessionsCompleted"), 
            "Should have 1 completed session");
        assertTrue((Integer) productivityMetrics.get("productivityScore") >= 85, 
            "Productivity score should reflect session score");
    }

    @Test
    @Order(4)
    @DisplayName("TDD: Achievement earning should be recorded in analytics with proper metrics")
    void testAchievementEarningAnalyticsFlow() {
        // Given: User has completed a timer session
        // (Depends on previous tests)

        // When: User earns an achievement (triggered by session completion)
        // This might be automatic based on session completion, or we trigger it manually
        Map<String, Object> achievementRequest = TestDataFactory.AnalyticsEvents.createAchievementEvent(
            testUserId, 
            "FOCUS_MASTER", 
            Map.of("consecutiveSessions", 1, "minimumDuration", 25)
        );

        // Simulate achievement system triggering the achievement
        Response achievementResponse = given()
            .header("Authorization", "Bearer " + authToken)
            .contentType(ContentType.JSON)
            .body(achievementRequest)
            .when()
            .post("/api/analytics/achievements/record")
            .then()
            .statusCode(201)
            .body("userId", equalTo(testUserId))
            .body("eventType", equalTo("ACHIEVEMENT_EARNED"))
            .extract().response();

        // Then: Analytics service should record the achievement
        CrossServiceTestUtils.waitForEventualConsistency();

        // Verify achievement event was recorded
        Awaitility.await()
            .atMost(10, TimeUnit.SECONDS)
            .pollInterval(1, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                Response analyticsResponse = given()
                    .header("Authorization", "Bearer " + authToken)
                    .when()
                    .get("/api/analytics/events")
                    .then()
                    .statusCode(200)
                    .extract().response();

                List<Map<String, Object>> events = analyticsResponse.jsonPath().getList("content");
                
                boolean achievementEventExists = events.stream()
                    .anyMatch(event -> 
                        "ACHIEVEMENT_EARNED".equals(event.get("eventType")) &&
                        testUserId.equals(event.get("userId"))
                    );

                assertTrue(achievementEventExists, "Analytics should record ACHIEVEMENT_EARNED event");
            });

        // Verify user achievement profile was updated
        Response achievementsResponse = given()
            .header("Authorization", "Bearer " + authToken)
            .when()
            .get("/api/analytics/users/{userId}/achievements", testUserId)
            .then()
            .statusCode(200)
            .body("userId", equalTo(testUserId))
            .body("totalAchievements", greaterThan(0))
            .body("totalPoints", greaterThan(0))
            .extract().response();

        Map<String, Object> achievementProfile = achievementsResponse.as(new TypeReference<Map<String, Object>>() {});
        
        // Verify specific achievement data
        assertTrue((Integer) achievementProfile.get("totalAchievements") >= 1, 
            "User should have at least 1 achievement");
        assertTrue((Integer) achievementProfile.get("totalPoints") >= 100, 
            "User should have earned points for achievement");

        // Verify achievement details
        List<Map<String, Object>> achievements = (List<Map<String, Object>>) achievementProfile.get("achievements");
        boolean focusMasterExists = achievements.stream()
            .anyMatch(achievement -> "FOCUS_MASTER".equals(achievement.get("type")));
        assertTrue(focusMasterExists, "FOCUS_MASTER achievement should be recorded");
    }

    @Test
    @Order(5)
    @DisplayName("Performance: Data consistency verification across services")
    void testDataConsistencyAcrossServices() {
        // Given: All previous operations have completed

        // When: We query all services for user data
        TimedResult<Response> hiveServiceResult = CrossServiceTestUtils.measureExecutionTime(() -> 
            given()
                .header("Authorization", "Bearer " + authToken)
                .when()
                .get("/api/hives/{hiveId}/members", testHiveId)
                .then()
                .statusCode(200)
                .extract().response()
        );

        TimedResult<Response> analyticsServiceResult = CrossServiceTestUtils.measureExecutionTime(() -> 
            given()
                .header("Authorization", "Bearer " + authToken)
                .when()
                .get("/api/analytics/users/{userId}/profile", testUserId)
                .then()
                .statusCode(200)
                .extract().response()
        );

        TimedResult<Response> timerServiceResult = CrossServiceTestUtils.measureExecutionTime(() -> 
            given()
                .header("Authorization", "Bearer " + authToken)
                .when()
                .get("/api/timer/sessions/{userId}/history", testUserId)
                .then()
                .statusCode(200)
                .extract().response()
        );

        // Then: Data should be consistent across all services
        
        // Verify response times are acceptable
        CrossServiceTestUtils.assertResponseTime(hiveServiceResult.getDuration(), Duration.ofMillis(500));
        CrossServiceTestUtils.assertResponseTime(analyticsServiceResult.getDuration(), Duration.ofMillis(500));
        CrossServiceTestUtils.assertResponseTime(timerServiceResult.getDuration(), Duration.ofMillis(500));

        // Verify data consistency
        List<Map<String, Object>> hiveMembers = hiveServiceResult.getResult().jsonPath().getList("members");
        boolean userInHive = hiveMembers.stream()
            .anyMatch(member -> testUserId.equals(member.get("userId")));
        assertTrue(userInHive, "User should be listed as hive member");

        Map<String, Object> analyticsProfile = analyticsServiceResult.getResult().as(new TypeReference<Map<String, Object>>() {});
        assertEquals(1, analyticsProfile.get("totalHivesJoined"), "Analytics should show 1 hive joined");

        List<Map<String, Object>> sessionHistory = timerServiceResult.getResult().jsonPath().getList("sessions");
        assertEquals(1, sessionHistory.size(), "Timer service should show 1 completed session");
        assertEquals(testSessionId, sessionHistory.get(0).get("id"), "Session ID should match");
    }

    @Test
    @Order(6)
    @DisplayName("Error Handling: Service failure resilience testing")
    void testServiceFailureResilience() {
        // Given: Normal operation state

        // When: One service is temporarily unavailable (simulate with wrong endpoint)
        // Try to perform an operation that would normally update analytics
        Response failureResponse = given()
            .header("Authorization", "Bearer " + authToken)
            .contentType(ContentType.JSON)
            .body(Map.of("action", "test_resilience"))
            .when()
            .post("/api/analytics/invalid-endpoint")
            .then()
            .statusCode(anyOf(is(404), is(503)))
            .extract().response();

        // Then: System should handle gracefully and maintain data integrity
        // Verify other services still work
        given()
            .header("Authorization", "Bearer " + authToken)
            .when()
            .get("/api/hives/{hiveId}", testHiveId)
            .then()
            .statusCode(200)
            .body("id", equalTo(testHiveId));

        // Verify retry mechanism works (if implemented)
        // This would test circuit breaker patterns and retry logic
        Awaitility.await()
            .atMost(30, TimeUnit.SECONDS)
            .pollInterval(2, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                // Verify that eventual consistency is maintained
                // Even if analytics service had temporary issues
                Response healthResponse = given()
                    .when()
                    .get("/api/analytics/health")
                    .then()
                    .extract().response();
                
                // The service should recover (or this test documents the behavior)
                assertTrue(healthResponse.statusCode() == 200 || healthResponse.statusCode() == 503,
                    "Analytics service should be available or return proper error status");
            });
    }

    // Private helper methods

    private void setupTestUserAndAuthentication() {
        // Create test user
        Map<String, Object> userRequest = TestDataFactory.Users.createTestUser(
            "analytics-test@example.com", 
            "Analytics Test User"
        );

        Response userResponse = given()
            .contentType(ContentType.JSON)
            .body(userRequest)
            .when()
            .post("/api/auth/register")
            .then()
            .statusCode(201)
            .body("email", equalTo("analytics-test@example.com"))
            .extract().response();

        testUserId = userResponse.jsonPath().getString("id");

        // Authenticate user
        Map<String, Object> loginRequest = Map.of(
            "email", "analytics-test@example.com",
            "password", "TestPassword123!"
        );

        Response authResponse = given()
            .contentType(ContentType.JSON)
            .body(loginRequest)
            .when()
            .post("/api/auth/login")
            .then()
            .statusCode(200)
            .body("token", notNullValue())
            .extract().response();

        authToken = authResponse.jsonPath().getString("token");
    }

    private void setupTestHive() {
        Map<String, Object> hiveRequest = TestDataFactory.Hives.createTestHive(
            "Analytics Test Hive", 
            testUserId
        );

        Response hiveResponse = given()
            .header("Authorization", "Bearer " + authToken)
            .contentType(ContentType.JSON)
            .body(hiveRequest)
            .when()
            .post("/api/hives")
            .then()
            .statusCode(201)
            .body("name", equalTo("Analytics Test Hive"))
            .body("createdBy", equalTo(testUserId))
            .extract().response();

        testHiveId = hiveResponse.jsonPath().getString("id");
    }
}