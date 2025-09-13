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
 * Cross-service integration test for Hive Activity → Notification Generation flow.
 * 
 * Tests the complete notification flow:
 * 1. New member joins hive → Notification to hive creator
 * 2. Timer session starts → Notification to active members
 * 3. Hive reaches milestone → Achievement notifications
 * 4. Hive becomes inactive → Warning notifications
 * 
 * Verifies:
 * - Notification delivery across multiple channels (in-app, email, push)
 * - User notification preferences are respected
 * - Notification templates and content accuracy
 * - Performance and eventual consistency
 * 
 * Following TDD approach with emphasis on notification reliability.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@SpringJUnitConfig
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Tag("cross-service")
@Tag("data-flow")
@Tag("integration")
@Tag("notifications")
class HiveNotificationFlowIntegrationTest extends AbstractCrossServiceIntegrationTest {

    private String hiveCreatorId;
    private String hiveCreatorToken;
    private String newMemberId;
    private String newMemberToken;
    private String testHiveId;
    private String thirdUserId;
    private String thirdUserToken;

    @BeforeAll
    void setUpTestData() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = 8080;

        // Create test users
        setupHiveCreator();
        setupNewMember();
        setupThirdUser();
        
        // Create test hive
        setupTestHive();
        
        // Configure notification preferences
        setupNotificationPreferences();
    }

    @Test
    @Order(1)
    @DisplayName("TDD: New member joining hive should trigger notifications to creator - FAILING TEST")
    void testNewMemberJoinNotification_ShouldFail() {
        // STEP 1: Write failing test first (TDD)
        
        // Given: A hive with a creator and notification preferences set
        assertNotNull(testHiveId, "Test hive should exist");
        assertNotNull(hiveCreatorId, "Hive creator should exist");
        assertNotNull(newMemberId, "New member should exist");

        // When: New member joins the hive
        Response joinResponse = given()
            .header("Authorization", "Bearer " + newMemberToken)
            .contentType(ContentType.JSON)
            .when()
            .post("/api/hives/{hiveId}/join", testHiveId)
            .then()
            .statusCode(200)
            .body("message", containsString("joined"))
            .extract().response();

        long actionTimestamp = System.currentTimeMillis();

        // Then: Notification service should generate notifications for hive creator
        CrossServiceTestUtils.waitForEventualConsistency();

        // Verify notification was created and queued
        Awaitility.await()
            .atMost(15, TimeUnit.SECONDS)
            .pollInterval(1, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                Response notificationsResponse = given()
                    .header("Authorization", "Bearer " + hiveCreatorToken)
                    .when()
                    .get("/api/notifications/inbox")
                    .then()
                    .statusCode(200)
                    .extract().response();

                List<Map<String, Object>> notifications = notificationsResponse.jsonPath().getList("notifications");
                
                // Find the member join notification
                boolean joinNotificationExists = notifications.stream()
                    .anyMatch(notification -> 
                        "HIVE_NEW_MEMBER".equals(notification.get("type")) &&
                        hiveCreatorId.equals(notification.get("recipientId")) &&
                        notification.get("metadata") != null &&
                        testHiveId.equals(((Map<String, Object>) notification.get("metadata")).get("hiveId"))
                    );

                assertTrue(joinNotificationExists, 
                    "Hive creator should receive HIVE_NEW_MEMBER notification");
            });

        // Verify notification content and metadata
        Response creatorNotifications = given()
            .header("Authorization", "Bearer " + hiveCreatorToken)
            .when()
            .get("/api/notifications/inbox")
            .then()
            .statusCode(200)
            .extract().response();

        List<Map<String, Object>> notifications = creatorNotifications.jsonPath().getList("notifications");
        Map<String, Object> joinNotification = notifications.stream()
            .filter(n -> "HIVE_NEW_MEMBER".equals(n.get("type")))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Join notification not found"));

        // Verify notification content
        assertNotNull(joinNotification.get("title"), "Notification should have a title");
        assertNotNull(joinNotification.get("message"), "Notification should have a message");
        assertTrue(joinNotification.get("message").toString().contains("joined"), 
            "Notification message should mention 'joined'");

        // Verify notification channels
        List<String> channels = (List<String>) joinNotification.get("channels");
        assertTrue(channels.contains("IN_APP"), "Should include in-app notification");
        assertTrue(channels.contains("EMAIL"), "Should include email notification");
    }

    @Test
    @Order(2)
    @DisplayName("TDD: Timer session start should notify active hive members")
    void testTimerSessionStartNotification() {
        // Given: Multiple members are in the hive
        // Join third user to hive
        given()
            .header("Authorization", "Bearer " + thirdUserToken)
            .when()
            .post("/api/hives/{hiveId}/join", testHiveId)
            .then()
            .statusCode(200);

        CrossServiceTestUtils.waitForEventualConsistency();

        // When: New member starts a timer session
        Map<String, Object> sessionRequest = TestDataFactory.TimerSessions.createTimerSession(
            newMemberId, testHiveId, 25
        );
        
        Response sessionResponse = given()
            .header("Authorization", "Bearer " + newMemberToken)
            .contentType(ContentType.JSON)
            .body(sessionRequest)
            .when()
            .post("/api/timer/start")
            .then()
            .statusCode(201)
            .body("status", equalTo("ACTIVE"))
            .extract().response();

        String sessionId = sessionResponse.jsonPath().getString("id");

        // Then: Other active members should receive notifications
        CrossServiceTestUtils.waitForEventualConsistency();

        // Verify hive creator received session start notification
        Awaitility.await()
            .atMost(15, TimeUnit.SECONDS)
            .pollInterval(1, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                Response creatorNotifications = given()
                    .header("Authorization", "Bearer " + hiveCreatorToken)
                    .when()
                    .get("/api/notifications/inbox")
                    .then()
                    .statusCode(200)
                    .extract().response();

                List<Map<String, Object>> notifications = creatorNotifications.jsonPath().getList("notifications");
                
                boolean sessionStartNotificationExists = notifications.stream()
                    .anyMatch(notification -> 
                        "TIMER_SESSION_STARTED".equals(notification.get("type")) &&
                        notification.get("metadata") != null &&
                        sessionId.equals(((Map<String, Object>) notification.get("metadata")).get("sessionId"))
                    );

                assertTrue(sessionStartNotificationExists, 
                    "Hive members should receive timer session start notification");
            });

        // Verify third user also received notification
        Awaitility.await()
            .atMost(15, TimeUnit.SECONDS)
            .pollInterval(1, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                Response thirdUserNotifications = given()
                    .header("Authorization", "Bearer " + thirdUserToken)
                    .when()
                    .get("/api/notifications/inbox")
                    .then()
                    .statusCode(200)
                    .extract().response();

                List<Map<String, Object>> notifications = thirdUserNotifications.jsonPath().getList("notifications");
                
                boolean sessionStartNotificationExists = notifications.stream()
                    .anyMatch(notification -> 
                        "TIMER_SESSION_STARTED".equals(notification.get("type"))
                    );

                assertTrue(sessionStartNotificationExists, 
                    "All hive members should receive session notifications");
            });

        // Verify notification preferences are respected
        Response thirdUserNotifications = given()
            .header("Authorization", "Bearer " + thirdUserToken)
            .when()
            .get("/api/notifications/inbox")
            .then()
            .statusCode(200)
            .extract().response();

        List<Map<String, Object>> notifications = thirdUserNotifications.jsonPath().getList("notifications");
        Map<String, Object> sessionNotification = notifications.stream()
            .filter(n -> "TIMER_SESSION_STARTED".equals(n.get("type")))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Session notification not found"));

        // Verify notification priority (should be LOW for session starts)
        assertEquals("LOW", sessionNotification.get("priority"), 
            "Session start notifications should have LOW priority");
    }

    @Test
    @Order(3)
    @DisplayName("TDD: Hive milestone achievement should trigger celebration notifications")
    void testHiveMilestoneNotification() {
        // Given: Hive with active members and sessions
        // Complete the timer session to create milestone opportunity
        Map<String, Object> completionRequest = Map.of(
            "actualDuration", 25,
            "productivityScore", 90
        );

        given()
            .header("Authorization", "Bearer " + newMemberToken)
            .contentType(ContentType.JSON)
            .body(completionRequest)
            .when()
            .post("/api/timer/complete")
            .then()
            .statusCode(200);

        CrossServiceTestUtils.waitForEventualConsistency();

        // When: Hive reaches a milestone (simulate by triggering achievement)
        Map<String, Object> milestoneRequest = Map.of(
            "hiveId", testHiveId,
            "milestoneType", "FIRST_COMPLETED_SESSION",
            "achievedBy", List.of(hiveCreatorId, newMemberId, thirdUserId),
            "celebrationLevel", "BRONZE",
            "pointsEarned", 150
        );

        Response milestoneResponse = given()
            .header("Authorization", "Bearer " + hiveCreatorToken)
            .contentType(ContentType.JSON)
            .body(milestoneRequest)
            .when()
            .post("/api/hives/{hiveId}/milestones", testHiveId)
            .then()
            .statusCode(201)
            .body("milestoneType", equalTo("FIRST_COMPLETED_SESSION"))
            .extract().response();

        // Then: All hive members should receive milestone notifications
        CrossServiceTestUtils.waitForEventualConsistency();

        // Verify all members receive milestone notifications
        List<String> memberTokens = List.of(hiveCreatorToken, newMemberToken, thirdUserToken);
        List<String> memberIds = List.of(hiveCreatorId, newMemberId, thirdUserId);

        for (int i = 0; i < memberTokens.size(); i++) {
            String token = memberTokens.get(i);
            String userId = memberIds.get(i);

            Awaitility.await()
                .atMost(20, TimeUnit.SECONDS)
                .pollInterval(2, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    Response memberNotifications = given()
                        .header("Authorization", "Bearer " + token)
                        .when()
                        .get("/api/notifications/inbox")
                        .then()
                        .statusCode(200)
                        .extract().response();

                    List<Map<String, Object>> notifications = memberNotifications.jsonPath().getList("notifications");
                    
                    boolean milestoneNotificationExists = notifications.stream()
                        .anyMatch(notification -> 
                            "HIVE_MILESTONE_ACHIEVED".equals(notification.get("type")) &&
                            userId.equals(notification.get("recipientId"))
                        );

                    assertTrue(milestoneNotificationExists, 
                        "Member " + userId + " should receive milestone notification");
                });
        }

        // Verify milestone notification content
        Response creatorNotifications = given()
            .header("Authorization", "Bearer " + hiveCreatorToken)
            .when()
            .get("/api/notifications/inbox")
            .then()
            .statusCode(200)
            .extract().response();

        List<Map<String, Object>> notifications = creatorNotifications.jsonPath().getList("notifications");
        Map<String, Object> milestoneNotification = notifications.stream()
            .filter(n -> "HIVE_MILESTONE_ACHIEVED".equals(n.get("type")))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Milestone notification not found"));

        // Verify notification is marked as HIGH priority for celebrations
        assertEquals("HIGH", milestoneNotification.get("priority"), 
            "Milestone notifications should have HIGH priority");
        
        // Verify celebration elements are included
        List<String> channels = (List<String>) milestoneNotification.get("channels");
        assertTrue(channels.contains("IN_APP"), "Should include in-app notification");
        assertTrue(channels.contains("PUSH"), "Should include push notification for celebrations");
        assertTrue(channels.contains("EMAIL"), "Should include email for milestone celebrations");
    }

    @Test
    @Order(4)
    @DisplayName("TDD: Inactive hive should trigger warning notifications")
    void testInactiveHiveWarningNotification() {
        // Given: A hive that has been inactive
        // Simulate hive inactivity by creating a separate hive and not using it
        Map<String, Object> inactiveHiveRequest = TestDataFactory.Hives.createTestHive(
            "Inactive Test Hive", hiveCreatorId
        );

        Response inactiveHiveResponse = given()
            .header("Authorization", "Bearer " + hiveCreatorToken)
            .contentType(ContentType.JSON)
            .body(inactiveHiveRequest)
            .when()
            .post("/api/hives")
            .then()
            .statusCode(201)
            .extract().response();

        String inactiveHiveId = inactiveHiveResponse.jsonPath().getString("id");
        
        // Wait and then trigger inactivity check (simulate scheduled job)
        CrossServiceTestUtils.waitForEventualConsistency();

        // When: System detects hive inactivity and triggers warning
        Map<String, Object> inactivityRequest = Map.of(
            "hiveId", inactiveHiveId,
            "inactivityDays", 7,
            "lastActivityType", "CREATION",
            "riskLevel", "MEDIUM"
        );

        Response warningResponse = given()
            .header("Authorization", "Bearer " + hiveCreatorToken)
            .contentType(ContentType.JSON)
            .body(inactivityRequest)
            .when()
            .post("/api/notifications/hive-inactivity-warning")
            .then()
            .statusCode(200)
            .body("warningType", equalTo("INACTIVITY"))
            .extract().response();

        // Then: Hive creator should receive warning notification
        CrossServiceTestUtils.waitForEventualConsistency();

        Awaitility.await()
            .atMost(15, TimeUnit.SECONDS)
            .pollInterval(1, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                Response creatorNotifications = given()
                    .header("Authorization", "Bearer " + hiveCreatorToken)
                    .when()
                    .get("/api/notifications/inbox")
                    .then()
                    .statusCode(200)
                    .extract().response();

                List<Map<String, Object>> notifications = creatorNotifications.jsonPath().getList("notifications");
                
                boolean warningNotificationExists = notifications.stream()
                    .anyMatch(notification -> 
                        "HIVE_INACTIVITY_WARNING".equals(notification.get("type")) &&
                        notification.get("metadata") != null &&
                        inactiveHiveId.equals(((Map<String, Object>) notification.get("metadata")).get("hiveId"))
                    );

                assertTrue(warningNotificationExists, 
                    "Hive creator should receive inactivity warning notification");
            });

        // Verify warning notification content
        Response creatorNotifications = given()
            .header("Authorization", "Bearer " + hiveCreatorToken)
            .when()
            .get("/api/notifications/inbox")
            .then()
            .statusCode(200)
            .extract().response();

        List<Map<String, Object>> notifications = creatorNotifications.jsonPath().getList("notifications");
        Map<String, Object> warningNotification = notifications.stream()
            .filter(n -> "HIVE_INACTIVITY_WARNING".equals(n.get("type")))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Warning notification not found"));

        // Verify warning notification properties
        assertEquals("MEDIUM", warningNotification.get("priority"), 
            "Warning notifications should have MEDIUM priority");
        assertTrue(warningNotification.get("message").toString().contains("inactive"), 
            "Warning message should mention inactivity");

        // Verify actionable content
        Map<String, Object> metadata = (Map<String, Object>) warningNotification.get("metadata");
        assertNotNull(metadata.get("actionUrl"), "Warning should include action URL");
        assertNotNull(metadata.get("suggestions"), "Warning should include suggestions");
    }

    @Test
    @Order(5)
    @DisplayName("Performance: Notification delivery performance and reliability")
    void testNotificationPerformanceAndReliability() {
        // Given: System with multiple notifications pending

        // When: We measure notification delivery performance
        long startTime = System.currentTimeMillis();

        // Trigger multiple notifications simultaneously
        List<Map<String, Object>> bulkNotifications = List.of(
            TestDataFactory.Notifications.createHiveJoinNotification(hiveCreatorId, "Test User", "Test Hive"),
            TestDataFactory.Notifications.createTimerStartNotification(newMemberId, "Creator", "Test Hive"),
            TestDataFactory.Notifications.createAchievementNotification(thirdUserId, "EARLY_BIRD")
        );

        for (Map<String, Object> notification : bulkNotifications) {
            given()
                .contentType(ContentType.JSON)
                .body(notification)
                .when()
                .post("/api/notifications/send")
                .then()
                .statusCode(anyOf(is(200), is(202))); // Accept async responses
        }

        // Then: All notifications should be delivered within acceptable time
        CrossServiceTestUtils.waitForEventualConsistency();

        long maxDeliveryTime = 0;
        List<String> recipientTokens = List.of(hiveCreatorToken, newMemberToken, thirdUserToken);

        for (String token : recipientTokens) {
            long recipientStartTime = System.currentTimeMillis();
            
            Awaitility.await()
                .atMost(10, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    Response notifications = given()
                        .header("Authorization", "Bearer " + token)
                        .when()
                        .get("/api/notifications/inbox")
                        .then()
                        .statusCode(200)
                        .extract().response();

                    List<Map<String, Object>> userNotifications = notifications.jsonPath().getList("notifications");
                    assertTrue(userNotifications.size() > 0, "User should have received notifications");
                });

            long deliveryTime = System.currentTimeMillis() - recipientStartTime;
            maxDeliveryTime = Math.max(maxDeliveryTime, deliveryTime);
        }

        // Verify performance benchmarks
        assertTrue(maxDeliveryTime < 5000, 
            "Notification delivery should complete within 5 seconds (actual: " + maxDeliveryTime + "ms)");

        // Verify notification reliability - all notifications delivered
        for (String token : recipientTokens) {
            Response notifications = given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/api/notifications/inbox")
                .then()
                .statusCode(200)
                .extract().response();

            List<Map<String, Object>> userNotifications = notifications.jsonPath().getList("notifications");
            assertTrue(userNotifications.size() >= 3, 
                "Each user should have received multiple notifications from previous tests");
        }
    }

    @Test
    @Order(6)
    @DisplayName("Edge Case: Notification preferences respected across channels")
    void testNotificationPreferencesRespected() {
        // Given: User with specific notification preferences
        // Update third user to disable email notifications
        Map<String, Object> preferencesUpdate = Map.of(
            "channels", Map.of(
                "email", false,
                "push", true,
                "inApp", true
            ),
            "priorities", Map.of(
                "HIGH", true,
                "MEDIUM", true,
                "LOW", false
            )
        );

        given()
            .header("Authorization", "Bearer " + thirdUserToken)
            .contentType(ContentType.JSON)
            .body(preferencesUpdate)
            .when()
            .put("/api/notifications/preferences")
            .then()
            .statusCode(200);

        CrossServiceTestUtils.waitForEventualConsistency();

        // When: A notification is triggered for this user
        Map<String, Object> testNotification = TestDataFactory.Notifications.createHiveJoinNotification(
            thirdUserId, "Preference Test User", testHiveId
        );

        Response sendResponse = given()
            .contentType(ContentType.JSON)
            .body(testNotification)
            .when()
            .post("/api/notifications/send")
            .then()
            .statusCode(anyOf(is(200), is(202)))
            .extract().response();

        CrossServiceTestUtils.waitForEventualConsistency();

        // Then: Notification should respect user preferences
        // Verify notification was created with correct channels
        Awaitility.await()
            .atMost(10, TimeUnit.SECONDS)
            .pollInterval(1, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                Response userNotifications = given()
                    .header("Authorization", "Bearer " + thirdUserToken)
                    .when()
                    .get("/api/notifications/inbox")
                    .then()
                    .statusCode(200)
                    .extract().response();

                List<Map<String, Object>> notifications = userNotifications.jsonPath().getList("notifications");
                
                // Find the preference test notification
                Map<String, Object> preferenceNotification = notifications.stream()
                    .filter(n -> n.get("message").toString().contains("Preference Test User"))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("Preference test notification not found"));

                // Verify channels respect user preferences
                List<String> channels = (List<String>) preferenceNotification.get("channels");
                assertFalse(channels.contains("EMAIL"), 
                    "Email channel should be disabled per user preference");
                assertTrue(channels.contains("IN_APP"), 
                    "In-app channel should be enabled per user preference");
                assertTrue(channels.contains("PUSH"), 
                    "Push channel should be enabled per user preference");
            });

        // Verify delivery logs show preference filtering
        Response deliveryLogs = given()
            .header("Authorization", "Bearer " + thirdUserToken)
            .when()
            .get("/api/notifications/delivery-logs")
            .then()
            .statusCode(200)
            .extract().response();

        List<Map<String, Object>> logs = deliveryLogs.jsonPath().getList("logs");
        boolean preferencesRespected = logs.stream()
            .anyMatch(log -> 
                thirdUserId.equals(log.get("recipientId")) &&
                Boolean.FALSE.equals(log.get("emailSent")) &&
                Boolean.TRUE.equals(log.get("pushSent"))
            );

        assertTrue(preferencesRespected, "Delivery logs should show preferences were respected");
    }

    // Private helper methods

    private void setupHiveCreator() {
        Map<String, Object> creatorRequest = TestDataFactory.Users.createTestUser(
            "hive-creator@example.com", 
            "Hive Creator"
        );

        Response creatorResponse = given()
            .contentType(ContentType.JSON)
            .body(creatorRequest)
            .when()
            .post("/api/auth/register")
            .then()
            .statusCode(201)
            .extract().response();

        hiveCreatorId = creatorResponse.jsonPath().getString("id");

        // Authenticate
        Map<String, Object> loginRequest = Map.of(
            "email", "hive-creator@example.com",
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

        hiveCreatorToken = authResponse.jsonPath().getString("token");
    }

    private void setupNewMember() {
        Map<String, Object> memberRequest = TestDataFactory.Users.createTestUser(
            "new-member@example.com", 
            "New Member"
        );

        Response memberResponse = given()
            .contentType(ContentType.JSON)
            .body(memberRequest)
            .when()
            .post("/api/auth/register")
            .then()
            .statusCode(201)
            .extract().response();

        newMemberId = memberResponse.jsonPath().getString("id");

        // Authenticate
        Map<String, Object> loginRequest = Map.of(
            "email", "new-member@example.com",
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

        newMemberToken = authResponse.jsonPath().getString("token");
    }

    private void setupThirdUser() {
        Map<String, Object> userRequest = TestDataFactory.Users.createTestUser(
            "third-user@example.com", 
            "Third User"
        );

        Response userResponse = given()
            .contentType(ContentType.JSON)
            .body(userRequest)
            .when()
            .post("/api/auth/register")
            .then()
            .statusCode(201)
            .extract().response();

        thirdUserId = userResponse.jsonPath().getString("id");

        // Authenticate
        Map<String, Object> loginRequest = Map.of(
            "email", "third-user@example.com",
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

        thirdUserToken = authResponse.jsonPath().getString("token");
    }

    private void setupTestHive() {
        Map<String, Object> hiveRequest = TestDataFactory.Hives.createTestHive(
            "Notification Test Hive", 
            hiveCreatorId
        );

        Response hiveResponse = given()
            .header("Authorization", "Bearer " + hiveCreatorToken)
            .contentType(ContentType.JSON)
            .body(hiveRequest)
            .when()
            .post("/api/hives")
            .then()
            .statusCode(201)
            .extract().response();

        testHiveId = hiveResponse.jsonPath().getString("id");
    }

    private void setupNotificationPreferences() {
        // Set up notification preferences for all users
        List<String> tokens = List.of(hiveCreatorToken, newMemberToken, thirdUserToken);
        
        for (String token : tokens) {
            Map<String, Object> preferences = Map.of(
                "channels", Map.of(
                    "email", true,
                    "push", true,
                    "inApp", true
                ),
                "priorities", Map.of(
                    "HIGH", true,
                    "MEDIUM", true,
                    "LOW", true
                )
            );

            given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(preferences)
                .when()
                .put("/api/notifications/preferences")
                .then()
                .statusCode(anyOf(is(200), is(201)));
        }
    }
}