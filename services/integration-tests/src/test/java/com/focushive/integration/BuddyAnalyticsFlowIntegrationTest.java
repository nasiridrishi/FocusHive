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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Cross-service integration test for Buddy System → Analytics Update flow.
 * 
 * Tests the complete buddy analytics flow:
 * 1. Buddy matching → Analytics records partnership
 * 2. Buddy session completed → Joint metrics updated
 * 3. Accountability check-in → Progress tracked
 * 4. Buddy achievement → Shared celebration notification
 * 
 * Verifies:
 * - Cross-service data synchronization between Buddy and Analytics services
 * - Joint productivity metrics calculation
 * - Accountability tracking and reporting
 * - Shared achievement and celebration systems
 * - Performance under concurrent buddy operations
 * 
 * Following TDD approach with focus on buddy system reliability.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@SpringJUnitConfig
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Tag("cross-service")
@Tag("data-flow")
@Tag("integration")
@Tag("buddy-system")
class BuddyAnalyticsFlowIntegrationTest extends AbstractCrossServiceIntegrationTest {

    private String user1Id;
    private String user1Token;
    private String user2Id;
    private String user2Token;
    private String buddyPairId;
    private String jointSessionId;

    @BeforeAll
    void setUpTestData() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = 8080;

        // Create test users with buddy preferences
        setupBuddyUser1();
        setupBuddyUser2();
        
        // Set up buddy preferences for matching
        setupBuddyPreferences();
    }

    @Test
    @Order(1)
    @DisplayName("TDD: Buddy matching should record partnership in analytics - FAILING TEST")
    void testBuddyMatchingAnalyticsFlow_ShouldFail() {
        // STEP 1: Write failing test first (TDD)
        
        // Given: Two users with compatible buddy preferences
        assertNotNull(user1Id, "User 1 should be created");
        assertNotNull(user2Id, "User 2 should be created");

        // When: Buddy matching system pairs the users
        Map<String, Object> matchingRequest = Map.of(
            "user1Id", user1Id,
            "user2Id", user2Id,
            "matchingAlgorithm", "COMPATIBILITY_BASED",
            "compatibilityScore", 0.85
        );

        Response matchingResponse = given()
            .header("Authorization", "Bearer " + user1Token)
            .contentType(ContentType.JSON)
            .body(matchingRequest)
            .when()
            .post("/api/buddy/match")
            .then()
            .statusCode(201)
            .body("user1Id", equalTo(user1Id))
            .body("user2Id", equalTo(user2Id))
            .body("status", equalTo("ACTIVE"))
            .extract().response();

        buddyPairId = matchingResponse.jsonPath().getString("id");
        long matchingTimestamp = System.currentTimeMillis();

        // Then: Analytics service should record the partnership (THIS WILL FAIL INITIALLY)
        CrossServiceTestUtils.waitForEventualConsistency();

        // Verify analytics event was created for buddy partnership
        Awaitility.await()
            .atMost(15, TimeUnit.SECONDS)
            .pollInterval(1, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                Response analyticsResponse = given()
                    .header("Authorization", "Bearer " + user1Token)
                    .when()
                    .get("/api/analytics/events")
                    .then()
                    .statusCode(200)
                    .extract().response();

                List<Map<String, Object>> events = analyticsResponse.jsonPath().getList("content");
                
                // Find the buddy partnership event
                boolean partnershipEventExists = events.stream()
                    .anyMatch(event -> 
                        "BUDDY_PARTNERSHIP_CREATED".equals(event.get("eventType")) &&
                        buddyPairId.equals(event.get("buddyPairId"))
                    );

                assertTrue(partnershipEventExists, 
                    "Analytics service should record BUDDY_PARTNERSHIP_CREATED event for pair " + buddyPairId);
            });

        // Verify both users' analytics profiles show buddy partnership
        Response user1Analytics = given()
            .header("Authorization", "Bearer " + user1Token)
            .when()
            .get("/api/analytics/users/{userId}/buddy-metrics", user1Id)
            .then()
            .statusCode(200)
            .body("userId", equalTo(user1Id))
            .body("activeBuddyPartnerships", greaterThan(0))
            .extract().response();

        Response user2Analytics = given()
            .header("Authorization", "Bearer " + user2Token)
            .when()
            .get("/api/analytics/users/{userId}/buddy-metrics", user2Id)
            .then()
            .statusCode(200)
            .body("userId", equalTo(user2Id))
            .body("activeBuddyPartnerships", greaterThan(0))
            .extract().response();

        // Verify partnership metrics
        Map<String, Object> user1Metrics = user1Analytics.as(new TypeReference<Map<String, Object>>() {});
        Map<String, Object> user2Metrics = user2Analytics.as(new TypeReference<Map<String, Object>>() {});

        assertEquals(1, user1Metrics.get("activeBuddyPartnerships"), "User 1 should have 1 active buddy partnership");
        assertEquals(1, user2Metrics.get("activeBuddyPartnerships"), "User 2 should have 1 active buddy partnership");
        
        // Verify compatibility score is recorded
        assertTrue((Double) user1Metrics.get("averageCompatibilityScore") >= 0.8, 
            "User 1 should have high compatibility score");
        assertTrue((Double) user2Metrics.get("averageCompatibilityScore") >= 0.8, 
            "User 2 should have high compatibility score");
    }

    @Test
    @Order(2)
    @DisplayName("TDD: Joint buddy session should update shared metrics")
    void testJointBuddySessionAnalytics() {
        // Given: Active buddy partnership exists
        assertNotNull(buddyPairId, "Buddy pair should exist from previous test");

        // When: Buddies start a joint focus session
        Map<String, Object> jointSessionRequest = TestDataFactory.BuddyData.createBuddySession(
            buddyPairId, user1Id, user2Id, 50
        );

        Response sessionResponse = given()
            .header("Authorization", "Bearer " + user1Token)
            .contentType(ContentType.JSON)
            .body(jointSessionRequest)
            .when()
            .post("/api/buddy/sessions/start")
            .then()
            .statusCode(201)
            .body("buddyPairId", equalTo(buddyPairId))
            .body("status", equalTo("ACTIVE"))
            .body("type", equalTo("JOINT_FOCUS"))
            .extract().response();

        jointSessionId = sessionResponse.jsonPath().getString("id");
        
        // Complete the session after some time
        CrossServiceTestUtils.waitForEventualConsistency();

        Map<String, Object> completionRequest = Map.of(
            "sessionId", jointSessionId,
            "actualDuration", 50,
            "user1Productivity", 88,
            "user2Productivity", 92,
            "collaborationScore", 85,
            "goalsAchieved", List.of(
                Map.of("userId", user1Id, "goal", "Complete project proposal", "achieved", true),
                Map.of("userId", user2Id, "goal", "Study for exam", "achieved", true)
            )
        );

        Response completionResponse = given()
            .header("Authorization", "Bearer " + user1Token)
            .contentType(ContentType.JSON)
            .body(completionRequest)
            .when()
            .post("/api/buddy/sessions/complete")
            .then()
            .statusCode(200)
            .body("status", equalTo("COMPLETED"))
            .extract().response();

        // Then: Analytics service should update joint metrics
        CrossServiceTestUtils.waitForEventualConsistency();

        // Verify buddy session completion event
        Awaitility.await()
            .atMost(15, TimeUnit.SECONDS)
            .pollInterval(1, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                Response analyticsResponse = given()
                    .header("Authorization", "Bearer " + user1Token)
                    .when()
                    .get("/api/analytics/events")
                    .then()
                    .statusCode(200)
                    .extract().response();

                List<Map<String, Object>> events = analyticsResponse.jsonPath().getList("content");
                
                boolean sessionCompletionExists = events.stream()
                    .anyMatch(event -> 
                        "BUDDY_SESSION_COMPLETED".equals(event.get("eventType")) &&
                        jointSessionId.equals(event.get("sessionId")) &&
                        buddyPairId.equals(event.get("buddyPairId"))
                    );

                assertTrue(sessionCompletionExists, 
                    "Analytics should record BUDDY_SESSION_COMPLETED event");
            });

        // Verify joint productivity metrics were updated for both users
        Response user1SessionMetrics = given()
            .header("Authorization", "Bearer " + user1Token)
            .when()
            .get("/api/analytics/users/{userId}/buddy-sessions", user1Id)
            .then()
            .statusCode(200)
            .body("userId", equalTo(user1Id))
            .body("totalJointSessions", greaterThan(0))
            .extract().response();

        Response user2SessionMetrics = given()
            .header("Authorization", "Bearer " + user2Token)
            .when()
            .get("/api/analytics/users/{userId}/buddy-sessions", user2Id)
            .then()
            .statusCode(200)
            .body("userId", equalTo(user2Id))
            .body("totalJointSessions", greaterThan(0))
            .extract().response();

        // Verify session metrics details
        Map<String, Object> user1Sessions = user1SessionMetrics.as(new TypeReference<Map<String, Object>>() {});
        Map<String, Object> user2Sessions = user2SessionMetrics.as(new TypeReference<Map<String, Object>>() {});

        assertEquals(1, user1Sessions.get("totalJointSessions"), "User 1 should have 1 joint session");
        assertEquals(1, user2Sessions.get("totalJointSessions"), "User 2 should have 1 joint session");

        // Verify collaboration metrics
        assertTrue((Double) user1Sessions.get("averageCollaborationScore") >= 80.0, 
            "User 1 should have good collaboration score");
        assertTrue((Double) user2Sessions.get("averageCollaborationScore") >= 80.0, 
            "User 2 should have good collaboration score");

        // Verify joint focus time is tracked
        assertTrue((Double) user1Sessions.get("totalJointFocusHours") >= 0.8, 
            "User 1 should have joint focus time recorded");
        assertTrue((Double) user2Sessions.get("totalJointFocusHours") >= 0.8, 
            "User 2 should have joint focus time recorded");
    }

    @Test
    @Order(3)
    @DisplayName("TDD: Accountability check-in should track progress metrics")
    void testAccountabilityCheckInAnalytics() {
        // Given: Active buddy partnership with completed session
        assertNotNull(buddyPairId, "Buddy pair should exist");

        // When: User 1 sends accountability check-in to User 2
        Map<String, Object> checkInRequest = TestDataFactory.BuddyData.createAccountabilityCheckIn(
            buddyPairId, user1Id, user2Id
        );

        Response checkInResponse = given()
            .header("Authorization", "Bearer " + user1Token)
            .contentType(ContentType.JSON)
            .body(checkInRequest)
            .when()
            .post("/api/buddy/accountability/check-in")
            .then()
            .statusCode(201)
            .body("buddyPairId", equalTo(buddyPairId))
            .body("fromUserId", equalTo(user1Id))
            .body("toUserId", equalTo(user2Id))
            .extract().response();

        String checkInId = checkInResponse.jsonPath().getString("id");

        // User 2 responds to the check-in
        CrossServiceTestUtils.waitForEventualConsistency();

        Map<String, Object> responseRequest = Map.of(
            "checkInId", checkInId,
            "responseMessage", "Great progress! Completed 3 tasks today.",
            "progressScore", 85,
            "challengesFaced", List.of("Time management", "Distractions"),
            "goalsForTomorrow", List.of("Finish chapter 5", "Review notes"),
            "supportNeeded", false
        );

        Response responseResponse = given()
            .header("Authorization", "Bearer " + user2Token)
            .contentType(ContentType.JSON)
            .body(responseRequest)
            .when()
            .post("/api/buddy/accountability/respond")
            .then()
            .statusCode(200)
            .body("status", equalTo("RESPONDED"))
            .extract().response();

        // Then: Analytics should track accountability metrics
        CrossServiceTestUtils.waitForEventualConsistency();

        // Verify accountability events were recorded
        Awaitility.await()
            .atMost(15, TimeUnit.SECONDS)
            .pollInterval(1, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                Response analyticsResponse = given()
                    .header("Authorization", "Bearer " + user1Token)
                    .when()
                    .get("/api/analytics/events")
                    .then()
                    .statusCode(200)
                    .extract().response();

                List<Map<String, Object>> events = analyticsResponse.jsonPath().getList("content");
                
                boolean checkInEventExists = events.stream()
                    .anyMatch(event -> 
                        "ACCOUNTABILITY_CHECK_IN".equals(event.get("eventType")) &&
                        checkInId.equals(event.get("checkInId"))
                    );

                boolean responseEventExists = events.stream()
                    .anyMatch(event -> 
                        "ACCOUNTABILITY_RESPONSE".equals(event.get("eventType")) &&
                        checkInId.equals(event.get("checkInId"))
                    );

                assertTrue(checkInEventExists, "Analytics should record accountability check-in event");
                assertTrue(responseEventExists, "Analytics should record accountability response event");
            });

        // Verify accountability metrics for both users
        Response user1AccountabilityMetrics = given()
            .header("Authorization", "Bearer " + user1Token)
            .when()
            .get("/api/analytics/users/{userId}/accountability", user1Id)
            .then()
            .statusCode(200)
            .body("userId", equalTo(user1Id))
            .extract().response();

        Response user2AccountabilityMetrics = given()
            .header("Authorization", "Bearer " + user2Token)
            .when()
            .get("/api/analytics/users/{userId}/accountability", user2Id)
            .then()
            .statusCode(200)
            .body("userId", equalTo(user2Id))
            .extract().response();

        // Verify accountability tracking details
        Map<String, Object> user1Accountability = user1AccountabilityMetrics.as(new TypeReference<Map<String, Object>>() {});
        Map<String, Object> user2Accountability = user2AccountabilityMetrics.as(new TypeReference<Map<String, Object>>() {});

        // User 1 sent a check-in
        assertTrue((Integer) user1Accountability.get("checkInsSent") >= 1, 
            "User 1 should have sent at least 1 check-in");
        
        // User 2 received and responded to a check-in
        assertTrue((Integer) user2Accountability.get("checkInsReceived") >= 1, 
            "User 2 should have received at least 1 check-in");
        assertTrue((Integer) user2Accountability.get("responseRate") >= 100, 
            "User 2 should have 100% response rate");

        // Verify progress tracking
        assertTrue((Double) user2Accountability.get("averageProgressScore") >= 80.0, 
            "User 2 should have high average progress score");
    }

    @Test
    @Order(4)
    @DisplayName("TDD: Buddy achievements should trigger shared celebration")
    void testBuddyAchievementSharedCelebration() {
        // Given: Active buddy partnership with tracked activities
        assertNotNull(buddyPairId, "Buddy pair should exist");

        // When: Buddy pair achieves a milestone together
        Map<String, Object> achievementRequest = Map.of(
            "buddyPairId", buddyPairId,
            "achievementType", "JOINT_FOCUS_STREAK",
            "criteria", Map.of(
                "consecutiveDays", 7,
                "totalJointHours", 10,
                "consistencyScore", 0.9
            ),
            "pointsEarned", 300,
            "celebrationLevel", "GOLD",
            "achievedBy", List.of(user1Id, user2Id)
        );

        Response achievementResponse = given()
            .header("Authorization", "Bearer " + user1Token)
            .contentType(ContentType.JSON)
            .body(achievementRequest)
            .when()
            .post("/api/buddy/achievements/record")
            .then()
            .statusCode(201)
            .body("buddyPairId", equalTo(buddyPairId))
            .body("achievementType", equalTo("JOINT_FOCUS_STREAK"))
            .extract().response();

        String achievementId = achievementResponse.jsonPath().getString("id");

        // Then: Both users should receive shared celebration and analytics updates
        CrossServiceTestUtils.waitForEventualConsistency();

        // Verify achievement event was recorded in analytics
        Awaitility.await()
            .atMost(15, TimeUnit.SECONDS)
            .pollInterval(1, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                Response analyticsResponse = given()
                    .header("Authorization", "Bearer " + user1Token)
                    .when()
                    .get("/api/analytics/events")
                    .then()
                    .statusCode(200)
                    .extract().response();

                List<Map<String, Object>> events = analyticsResponse.jsonPath().getList("content");
                
                boolean achievementEventExists = events.stream()
                    .anyMatch(event -> 
                        "BUDDY_ACHIEVEMENT_EARNED".equals(event.get("eventType")) &&
                        achievementId.equals(event.get("achievementId")) &&
                        buddyPairId.equals(event.get("buddyPairId"))
                    );

                assertTrue(achievementEventExists, 
                    "Analytics should record buddy achievement event");
            });

        // Verify both users received celebration notifications
        Response user1Notifications = given()
            .header("Authorization", "Bearer " + user1Token)
            .when()
            .get("/api/notifications/inbox")
            .then()
            .statusCode(200)
            .extract().response();

        Response user2Notifications = given()
            .header("Authorization", "Bearer " + user2Token)
            .when()
            .get("/api/notifications/inbox")
            .then()
            .statusCode(200)
            .extract().response();

        // Verify celebration notifications
        List<Map<String, Object>> user1NotificationsList = user1Notifications.jsonPath().getList("notifications");
        List<Map<String, Object>> user2NotificationsList = user2Notifications.jsonPath().getList("notifications");

        boolean user1HasCelebration = user1NotificationsList.stream()
            .anyMatch(n -> "BUDDY_ACHIEVEMENT_CELEBRATION".equals(n.get("type")));
        boolean user2HasCelebration = user2NotificationsList.stream()
            .anyMatch(n -> "BUDDY_ACHIEVEMENT_CELEBRATION".equals(n.get("type")));

        assertTrue(user1HasCelebration, "User 1 should receive buddy achievement celebration");
        assertTrue(user2HasCelebration, "User 2 should receive buddy achievement celebration");

        // Verify achievement metrics were updated
        Response user1AchievementMetrics = given()
            .header("Authorization", "Bearer " + user1Token)
            .when()
            .get("/api/analytics/users/{userId}/buddy-achievements", user1Id)
            .then()
            .statusCode(200)
            .body("userId", equalTo(user1Id))
            .extract().response();

        Response user2AchievementMetrics = given()
            .header("Authorization", "Bearer " + user2Token)
            .when()
            .get("/api/analytics/users/{userId}/buddy-achievements", user2Id)
            .then()
            .statusCode(200)
            .body("userId", equalTo(user2Id))
            .extract().response();

        // Verify shared achievement details
        Map<String, Object> user1Achievements = user1AchievementMetrics.as(new TypeReference<Map<String, Object>>() {});
        Map<String, Object> user2Achievements = user2AchievementMetrics.as(new TypeReference<Map<String, Object>>() {});

        assertTrue((Integer) user1Achievements.get("totalBuddyAchievements") >= 1, 
            "User 1 should have buddy achievements recorded");
        assertTrue((Integer) user2Achievements.get("totalBuddyAchievements") >= 1, 
            "User 2 should have buddy achievements recorded");

        // Verify shared points
        assertTrue((Integer) user1Achievements.get("buddyPointsEarned") >= 300, 
            "User 1 should have earned buddy points");
        assertTrue((Integer) user2Achievements.get("buddyPointsEarned") >= 300, 
            "User 2 should have earned buddy points");
    }

    @Test
    @Order(5)
    @DisplayName("Performance: Concurrent buddy operations and data synchronization")
    void testConcurrentBuddyOperationsPerformance() {
        // Given: Multiple buddy pairs operating simultaneously
        
        // Create additional users for concurrent testing
        String[] additionalUserIds = new String[4];
        String[] additionalTokens = new String[4];
        
        for (int i = 0; i < 4; i++) {
            Map<String, Object> userRequest = TestDataFactory.Users.createTestUser(
                "concurrent-user-" + i + "@example.com", 
                "Concurrent User " + i
            );

            Response userResponse = given()
                .contentType(ContentType.JSON)
                .body(userRequest)
                .when()
                .post("/api/auth/register")
                .then()
                .statusCode(201)
                .extract().response();

            additionalUserIds[i] = userResponse.jsonPath().getString("id");

            // Authenticate
            Map<String, Object> loginRequest = Map.of(
                "email", "concurrent-user-" + i + "@example.com",
                "password", "TestPassword123!"
            );

            Response authResponse = given()
                .contentType(ContentType.JSON)
                .body(loginRequest)
                .when()
                .post("/api/auth/login")
                .then()
                .statusCode(200)
                .extract().response();

            additionalTokens[i] = authResponse.jsonPath().getString("token");
        }

        // When: Multiple buddy operations occur simultaneously
        long startTime = System.currentTimeMillis();

        // Create multiple buddy pairs simultaneously
        String[] buddyPairIds = new String[2];
        
        for (int i = 0; i < 2; i++) {
            Map<String, Object> matchingRequest = Map.of(
                "user1Id", additionalUserIds[i * 2],
                "user2Id", additionalUserIds[i * 2 + 1],
                "matchingAlgorithm", "COMPATIBILITY_BASED",
                "compatibilityScore", 0.80 + (i * 0.05)
            );

            Response matchingResponse = given()
                .header("Authorization", "Bearer " + additionalTokens[i * 2])
                .contentType(ContentType.JSON)
                .body(matchingRequest)
                .when()
                .post("/api/buddy/match")
                .then()
                .statusCode(201)
                .extract().response();

            buddyPairIds[i] = matchingResponse.jsonPath().getString("id");
        }

        // Start multiple sessions simultaneously
        for (int i = 0; i < 2; i++) {
            Map<String, Object> sessionRequest = TestDataFactory.BuddyData.createBuddySession(
                buddyPairIds[i], additionalUserIds[i * 2], additionalUserIds[i * 2 + 1], 25
            );

            given()
                .header("Authorization", "Bearer " + additionalTokens[i * 2])
                .contentType(ContentType.JSON)
                .body(sessionRequest)
                .when()
                .post("/api/buddy/sessions/start")
                .then()
                .statusCode(201);
        }

        long operationTime = System.currentTimeMillis() - startTime;

        // Then: All operations should complete within acceptable time and maintain data consistency
        CrossServiceTestUtils.waitForEventualConsistency();

        // Verify performance benchmarks
        assertTrue(operationTime < 5000, 
            "Concurrent buddy operations should complete within 5 seconds (actual: " + operationTime + "ms)");

        // Verify data consistency across all services
        Awaitility.await()
            .atMost(20, TimeUnit.SECONDS)
            .pollInterval(2, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                for (int i = 0; i < 2; i++) {
                    // Check buddy service consistency
                    Response buddyPairResponse = given()
                        .header("Authorization", "Bearer " + additionalTokens[i * 2])
                        .when()
                        .get("/api/buddy/pairs/{pairId}", buddyPairIds[i])
                        .then()
                        .statusCode(200)
                        .body("id", equalTo(buddyPairIds[i]))
                        .extract().response();

                    // Check analytics service consistency
                    Response analyticsResponse = given()
                        .header("Authorization", "Bearer " + additionalTokens[i * 2])
                        .when()
                        .get("/api/analytics/users/{userId}/buddy-metrics", additionalUserIds[i * 2])
                        .then()
                        .statusCode(200)
                        .body("activeBuddyPartnerships", greaterThan(0))
                        .extract().response();

                    // Verify session tracking
                    Response sessionResponse = given()
                        .header("Authorization", "Bearer " + additionalTokens[i * 2])
                        .when()
                        .get("/api/buddy/sessions/active")
                        .then()
                        .statusCode(200)
                        .extract().response();

                    List<Map<String, Object>> activeSessions = sessionResponse.jsonPath().getList("sessions");
                    boolean hasActiveSession = activeSessions.stream()
                        .anyMatch(session -> buddyPairIds[i].equals(session.get("buddyPairId")));

                    assertTrue(hasActiveSession, "Buddy pair should have active session tracked");
                }
            });

        // Verify no data corruption occurred
        for (int i = 0; i < 4; i++) {
            Response userAnalytics = given()
                .header("Authorization", "Bearer " + additionalTokens[i])
                .when()
                .get("/api/analytics/users/{userId}/buddy-metrics", additionalUserIds[i])
                .then()
                .statusCode(200)
                .extract().response();

            Map<String, Object> metrics = userAnalytics.as(new TypeReference<Map<String, Object>>() {});
            assertEquals(1, metrics.get("activeBuddyPartnerships"), 
                "Each user should have exactly 1 active buddy partnership");
        }
    }

    @Test
    @Order(6)
    @DisplayName("Edge Case: Buddy partnership termination and analytics cleanup")
    void testBuddyPartnershipTerminationAnalytics() {
        // Given: Active buddy partnership exists
        assertNotNull(buddyPairId, "Buddy pair should exist");

        // When: One user terminates the buddy partnership
        Map<String, Object> terminationRequest = Map.of(
            "buddyPairId", buddyPairId,
            "initiatedBy", user1Id,
            "reason", "SCHEDULE_CONFLICT",
            "feedback", "Great buddy, but our schedules don't align anymore",
            "rating", 4
        );

        Response terminationResponse = given()
            .header("Authorization", "Bearer " + user1Token)
            .contentType(ContentType.JSON)
            .body(terminationRequest)
            .when()
            .post("/api/buddy/partnerships/terminate")
            .then()
            .statusCode(200)
            .body("status", equalTo("TERMINATED"))
            .extract().response();

        // Then: Analytics should record termination and update historical metrics
        CrossServiceTestUtils.waitForEventualConsistency();

        // Verify termination event was recorded
        Awaitility.await()
            .atMost(15, TimeUnit.SECONDS)
            .pollInterval(1, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                Response analyticsResponse = given()
                    .header("Authorization", "Bearer " + user1Token)
                    .when()
                    .get("/api/analytics/events")
                    .then()
                    .statusCode(200)
                    .extract().response();

                List<Map<String, Object>> events = analyticsResponse.jsonPath().getList("content");
                
                boolean terminationEventExists = events.stream()
                    .anyMatch(event -> 
                        "BUDDY_PARTNERSHIP_TERMINATED".equals(event.get("eventType")) &&
                        buddyPairId.equals(event.get("buddyPairId"))
                    );

                assertTrue(terminationEventExists, 
                    "Analytics should record partnership termination event");
            });

        // Verify analytics updated active partnerships count
        Response user1FinalMetrics = given()
            .header("Authorization", "Bearer " + user1Token)
            .when()
            .get("/api/analytics/users/{userId}/buddy-metrics", user1Id)
            .then()
            .statusCode(200)
            .body("userId", equalTo(user1Id))
            .extract().response();

        Response user2FinalMetrics = given()
            .header("Authorization", "Bearer " + user2Token)
            .when()
            .get("/api/analytics/users/{userId}/buddy-metrics", user2Id)
            .then()
            .statusCode(200)
            .body("userId", equalTo(user2Id))
            .extract().response();

        // Verify active partnerships count is updated
        Map<String, Object> user1Final = user1FinalMetrics.as(new TypeReference<Map<String, Object>>() {});
        Map<String, Object> user2Final = user2FinalMetrics.as(new TypeReference<Map<String, Object>>() {});

        assertEquals(0, user1Final.get("activeBuddyPartnerships"), 
            "User 1 should have 0 active partnerships after termination");
        assertEquals(0, user2Final.get("activeBuddyPartnerships"), 
            "User 2 should have 0 active partnerships after termination");

        // Verify historical data is preserved
        assertTrue((Integer) user1Final.get("totalHistoricalPartnerships") >= 1, 
            "User 1 should have historical partnership data");
        assertTrue((Integer) user2Final.get("totalHistoricalPartnerships") >= 1, 
            "User 2 should have historical partnership data");

        // Verify termination feedback is recorded
        assertNotNull(user1Final.get("partnershipFeedback"), 
            "User 1 should have partnership feedback recorded");
        assertTrue((Double) user1Final.get("averagePartnerRating") >= 4.0, 
            "User 1 should have partner rating recorded");
    }

    // Private helper methods

    private void setupBuddyUser1() {
        Map<String, Object> user1Request = TestDataFactory.Users.createUserWithPersonas(
            "buddy-user-1@example.com", 
            "Buddy User 1",
            List.of("work", "study")
        );

        Response user1Response = given()
            .contentType(ContentType.JSON)
            .body(user1Request)
            .when()
            .post("/api/auth/register")
            .then()
            .statusCode(201)
            .extract().response();

        user1Id = user1Response.jsonPath().getString("id");

        // Authenticate
        Map<String, Object> loginRequest = Map.of(
            "email", "buddy-user-1@example.com",
            "password", "TestPassword123!"
        );

        Response authResponse = given()
            .contentType(ContentType.JSON)
            .body(loginRequest)
            .when()
            .post("/api/auth/login")
            .then()
            .statusCode(200)
            .extract().response();

        user1Token = authResponse.jsonPath().getString("token");
    }

    private void setupBuddyUser2() {
        Map<String, Object> user2Request = TestDataFactory.Users.createUserWithPersonas(
            "buddy-user-2@example.com", 
            "Buddy User 2",
            List.of("work", "study")
        );

        Response user2Response = given()
            .contentType(ContentType.JSON)
            .body(user2Request)
            .when()
            .post("/api/auth/register")
            .then()
            .statusCode(201)
            .extract().response();

        user2Id = user2Response.jsonPath().getString("id");

        // Authenticate
        Map<String, Object> loginRequest = Map.of(
            "email", "buddy-user-2@example.com",
            "password", "TestPassword123!"
        );

        Response authResponse = given()
            .contentType(ContentType.JSON)
            .body(loginRequest)
            .when()
            .post("/api/auth/login")
            .then()
            .statusCode(200)
            .extract().response();

        user2Token = authResponse.jsonPath().getString("token");
    }

    private void setupBuddyPreferences() {
        // Set buddy preferences for User 1
        Map<String, Object> user1Preferences = Map.of(
            "workingHours", Map.of(
                "start", "09:00",
                "end", "17:00",
                "timezone", "UTC"
            ),
            "focusStyle", "DEEP_WORK",
            "communicationStyle", "REGULAR_CHECK_INS",
            "goals", List.of("Productivity", "Learning", "Accountability"),
            "preferredSessionDuration", 50,
            "availabilityDays", List.of("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY")
        );

        given()
            .header("Authorization", "Bearer " + user1Token)
            .contentType(ContentType.JSON)
            .body(user1Preferences)
            .when()
            .put("/api/buddy/preferences")
            .then()
            .statusCode(anyOf(is(200), is(201)));

        // Set buddy preferences for User 2 (compatible)
        Map<String, Object> user2Preferences = Map.of(
            "workingHours", Map.of(
                "start", "09:00",
                "end", "17:00",
                "timezone", "UTC"
            ),
            "focusStyle", "DEEP_WORK",
            "communicationStyle", "REGULAR_CHECK_INS",
            "goals", List.of("Productivity", "Learning", "Skill_Development"),
            "preferredSessionDuration", 50,
            "availabilityDays", List.of("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY")
        );

        given()
            .header("Authorization", "Bearer " + user2Token)
            .contentType(ContentType.JSON)
            .body(user2Preferences)
            .when()
            .put("/api/buddy/preferences")
            .then()
            .statusCode(anyOf(is(200), is(201)));

        CrossServiceTestUtils.waitForEventualConsistency();
    }
}