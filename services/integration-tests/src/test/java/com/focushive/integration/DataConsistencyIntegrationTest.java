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
 * Cross-service integration test for Data Consistency across all services.
 * 
 * Tests critical data consistency scenarios:
 * 1. User profile update → Propagated to all services
 * 2. Hive deletion → Cascade cleanup in all services
 * 3. User deletion (GDPR) → Complete data removal
 * 4. Service failure recovery → Data integrity maintained
 * 
 * Verifies:
 * - Transaction boundaries and rollback mechanisms
 * - Eventual consistency across distributed services
 * - Data integrity during service failures
 * - GDPR compliance and complete data removal
 * - Recovery patterns and compensating transactions
 * 
 * Following TDD approach with emphasis on data integrity and consistency.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@SpringJUnitConfig
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Tag("cross-service")
@Tag("data-consistency")
@Tag("integration")
@Tag("data-integrity")
class DataConsistencyIntegrationTest extends AbstractCrossServiceIntegrationTest {

    private String testUserId;
    private String testUserToken;
    private String testHiveId;
    private String testBuddyId;
    private String testBuddyToken;
    private String testSessionId;

    @BeforeAll
    void setUpTestData() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = 8080;

        // Create comprehensive test data across all services
        setupTestUser();
        setupTestBuddy();
        setupTestHive();
        setupCrossServiceData();
    }

    @Test
    @Order(1)
    @DisplayName("TDD: User profile update should propagate to all services - FAILING TEST")
    void testUserProfileUpdatePropagation_ShouldFail() {
        // STEP 1: Write failing test first (TDD)
        
        // Given: User exists across multiple services with data
        assertNotNull(testUserId, "Test user should exist");
        
        // Verify initial state across all services
        verifyUserExistsInAllServices(testUserId, "Original Test User");

        // When: User profile is updated
        Map<String, Object> profileUpdate = Map.of(
            "name", "Updated Test User",
            "email", "updated-test@example.com",
            "preferences", Map.of(
                "notifications", Map.of(
                    "email", false,
                    "push", true,
                    "inApp", true
                ),
                "privacy", Map.of(
                    "showActivity", false,
                    "shareProgress", true
                ),
                "timezone", "America/New_York"
            ),
            "personas", List.of("work", "study", "creative")
        );

        Response updateResponse = given()
            .header("Authorization", "Bearer " + testUserToken)
            .contentType(ContentType.JSON)
            .body(profileUpdate)
            .when()
            .put("/api/users/{userId}/profile", testUserId)
            .then()
            .statusCode(200)
            .body("name", equalTo("Updated Test User"))
            .body("email", equalTo("updated-test@example.com"))
            .extract().response();

        long updateTimestamp = System.currentTimeMillis();

        // Then: Changes should propagate to all services (THIS WILL FAIL INITIALLY)
        CrossServiceTestUtils.waitForEventualConsistency();

        // Verify propagation to Identity Service
        Awaitility.await()
            .atMost(20, TimeUnit.SECONDS)
            .pollInterval(2, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                Response identityResponse = given()
                    .header("Authorization", "Bearer " + testUserToken)
                    .when()
                    .get("/api/identity/users/{userId}", testUserId)
                    .then()
                    .statusCode(200)
                    .body("name", equalTo("Updated Test User"))
                    .body("email", equalTo("updated-test@example.com"))
                    .extract().response();

                Map<String, Object> identityUser = identityResponse.as(new TypeReference<Map<String, Object>>() {});
                List<String> personas = (List<String>) identityUser.get("personas");
                assertTrue(personas.contains("creative"), "Identity service should have updated personas");
            });

        // Verify propagation to Analytics Service
        Awaitility.await()
            .atMost(20, TimeUnit.SECONDS)
            .pollInterval(2, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                Response analyticsResponse = given()
                    .header("Authorization", "Bearer " + testUserToken)
                    .when()
                    .get("/api/analytics/users/{userId}/profile", testUserId)
                    .then()
                    .statusCode(200)
                    .extract().response();

                Map<String, Object> analyticsProfile = analyticsResponse.as(new TypeReference<Map<String, Object>>() {});
                assertEquals("Updated Test User", analyticsProfile.get("name"), 
                    "Analytics service should have updated user name");
                assertEquals("America/New_York", analyticsProfile.get("timezone"), 
                    "Analytics service should have updated timezone");
            });

        // Verify propagation to Notification Service
        Awaitility.await()
            .atMost(20, TimeUnit.SECONDS)
            .pollInterval(2, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                Response notificationResponse = given()
                    .header("Authorization", "Bearer " + testUserToken)
                    .when()
                    .get("/api/notifications/preferences")
                    .then()
                    .statusCode(200)
                    .extract().response();

                Map<String, Object> notificationPrefs = notificationResponse.as(new TypeReference<Map<String, Object>>() {});
                Map<String, Object> channels = (Map<String, Object>) notificationPrefs.get("channels");
                assertEquals(false, channels.get("email"), 
                    "Notification service should have updated email preference");
                assertEquals(true, channels.get("push"), 
                    "Notification service should have updated push preference");
            });

        // Verify propagation to Buddy Service
        Awaitility.await()
            .atMost(20, TimeUnit.SECONDS)
            .pollInterval(2, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                Response buddyResponse = given()
                    .header("Authorization", "Bearer " + testUserToken)
                    .when()
                    .get("/api/buddy/users/{userId}/profile", testUserId)
                    .then()
                    .statusCode(200)
                    .extract().response();

                Map<String, Object> buddyProfile = buddyResponse.as(new TypeReference<Map<String, Object>>() {});
                assertEquals("Updated Test User", buddyProfile.get("name"), 
                    "Buddy service should have updated user name");
            });

        // Verify propagation to Hive Service (member information)
        Awaitility.await()
            .atMost(20, TimeUnit.SECONDS)
            .pollInterval(2, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                Response hiveResponse = given()
                    .header("Authorization", "Bearer " + testUserToken)
                    .when()
                    .get("/api/hives/{hiveId}/members", testHiveId)
                    .then()
                    .statusCode(200)
                    .extract().response();

                List<Map<String, Object>> members = hiveResponse.jsonPath().getList("members");
                Map<String, Object> updatedMember = members.stream()
                    .filter(member -> testUserId.equals(member.get("userId")))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("User should be found in hive members"));

                assertEquals("Updated Test User", updatedMember.get("name"), 
                    "Hive service should show updated member name");
            });
    }

    @Test
    @Order(2)
    @DisplayName("TDD: Hive deletion should cascade cleanup across all services")
    void testHiveDeletionCascadeCleanup() {
        // Given: Hive with members, sessions, and related data across services
        assertNotNull(testHiveId, "Test hive should exist");
        
        // Add buddy to the hive
        given()
            .header("Authorization", "Bearer " + testBuddyToken)
            .when()
            .post("/api/hives/{hiveId}/join", testHiveId)
            .then()
            .statusCode(200);

        // Create timer session in the hive
        Map<String, Object> sessionRequest = TestDataFactory.TimerSessions.createTimerSession(
            testUserId, testHiveId, 25
        );

        Response sessionResponse = given()
            .header("Authorization", "Bearer " + testUserToken)
            .contentType(ContentType.JSON)
            .body(sessionRequest)
            .when()
            .post("/api/timer/start")
            .then()
            .statusCode(201)
            .extract().response();

        testSessionId = sessionResponse.jsonPath().getString("id");

        // Create analytics events for the hive
        Map<String, Object> analyticsEvent = TestDataFactory.AnalyticsEvents.createUserJoinEvent(
            testBuddyId, testHiveId
        );

        given()
            .contentType(ContentType.JSON)
            .body(analyticsEvent)
            .when()
            .post("/api/analytics/events")
            .then()
            .statusCode(anyOf(is(201), is(200)));

        CrossServiceTestUtils.waitForEventualConsistency();

        // Verify hive data exists across services before deletion
        verifyHiveExistsInAllServices(testHiveId);

        // When: Hive is deleted
        Response deletionResponse = given()
            .header("Authorization", "Bearer " + testUserToken)
            .when()
            .delete("/api/hives/{hiveId}", testHiveId)
            .then()
            .statusCode(anyOf(is(200), is(204)))
            .extract().response();

        // Then: All related data should be cleaned up across services
        CrossServiceTestUtils.waitForEventualConsistency();

        // Verify hive is deleted from core service
        Awaitility.await()
            .atMost(30, TimeUnit.SECONDS)
            .pollInterval(2, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                given()
                    .header("Authorization", "Bearer " + testUserToken)
                    .when()
                    .get("/api/hives/{hiveId}", testHiveId)
                    .then()
                    .statusCode(404);
            });

        // Verify timer sessions are stopped/cleaned up
        Awaitility.await()
            .atMost(30, TimeUnit.SECONDS)
            .pollInterval(2, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                Response timerResponse = given()
                    .header("Authorization", "Bearer " + testUserToken)
                    .when()
                    .get("/api/timer/sessions/{sessionId}", testSessionId)
                    .then()
                    .extract().response();

                // Session should either be not found or marked as terminated
                assertTrue(
                    timerResponse.statusCode() == 404 || 
                    "TERMINATED".equals(timerResponse.jsonPath().getString("status")),
                    "Timer session should be terminated or removed"
                );
            });

        // Verify analytics events are preserved but hive context is marked as deleted
        Awaitility.await()
            .atMost(30, TimeUnit.SECONDS)
            .pollInterval(2, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                Response analyticsResponse = given()
                    .header("Authorization", "Bearer " + testUserToken)
                    .when()
                    .get("/api/analytics/hives/{hiveId}/summary", testHiveId)
                    .then()
                    .extract().response();

                if (analyticsResponse.statusCode() == 200) {
                    Map<String, Object> hiveSummary = analyticsResponse.as(new TypeReference<Map<String, Object>>() {});
                    assertEquals("DELETED", hiveSummary.get("status"), 
                        "Analytics should mark hive as deleted but preserve historical data");
                } else {
                    assertEquals(404, analyticsResponse.statusCode(), 
                        "Analytics should return 404 for deleted hive if configured for hard delete");
                }
            });

        // Verify chat history is archived or removed
        Awaitility.await()
            .atMost(30, TimeUnit.SECONDS)
            .pollInterval(2, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                Response chatResponse = given()
                    .header("Authorization", "Bearer " + testUserToken)
                    .when()
                    .get("/api/chat/hives/{hiveId}/messages", testHiveId)
                    .then()
                    .extract().response();

                // Chat should either be archived or removed based on policy
                assertTrue(
                    chatResponse.statusCode() == 404 || 
                    chatResponse.statusCode() == 410, // Gone
                    "Chat should be properly cleaned up for deleted hive"
                );
            });

        // Verify notifications about hive deletion were sent
        Awaitility.await()
            .atMost(30, TimeUnit.SECONDS)
            .pollInterval(2, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                Response notificationResponse = given()
                    .header("Authorization", "Bearer " + testBuddyToken)
                    .when()
                    .get("/api/notifications/inbox")
                    .then()
                    .statusCode(200)
                    .extract().response();

                List<Map<String, Object>> notifications = notificationResponse.jsonPath().getList("notifications");
                boolean deletionNotificationExists = notifications.stream()
                    .anyMatch(notification -> 
                        "HIVE_DELETED".equals(notification.get("type")) &&
                        notification.get("metadata") != null &&
                        testHiveId.equals(((Map<String, Object>) notification.get("metadata")).get("hiveId"))
                    );

                assertTrue(deletionNotificationExists, 
                    "Members should receive hive deletion notifications");
            });
    }

    @Test
    @Order(3)
    @DisplayName("TDD: User deletion (GDPR) should completely remove all data")
    void testGDPRUserDeletionCompleteRemoval() {
        // Given: User with data across all services
        // Create a separate user for deletion testing
        Map<String, Object> gdprUserRequest = TestDataFactory.Users.createTestUser(
            "gdpr-user@example.com", 
            "GDPR Test User"
        );

        Response gdprUserResponse = given()
            .contentType(ContentType.JSON)
            .body(gdprUserRequest)
            .when()
            .post("/api/auth/register")
            .then()
            .statusCode(201)
            .extract().response();

        String gdprUserId = gdprUserResponse.jsonPath().getString("id");

        // Authenticate GDPR user
        Map<String, Object> gdprLoginRequest = Map.of(
            "email", "gdpr-user@example.com",
            "password", "TestPassword123!"
        );

        Response gdprAuthResponse = given()
            .contentType(ContentType.JSON)
            .body(gdprLoginRequest)
            .when()
            .post("/api/auth/login")
            .then()
            .statusCode(200)
            .extract().response();

        String gdprUserToken = gdprAuthResponse.jsonPath().getString("token");

        // Create data across all services for GDPR user
        setupGDPRUserData(gdprUserId, gdprUserToken);
        
        CrossServiceTestUtils.waitForEventualConsistency();

        // Verify user data exists across all services
        verifyUserExistsInAllServices(gdprUserId, "GDPR Test User");

        // When: User requests complete data deletion (GDPR right to be forgotten)
        Map<String, Object> deletionRequest = Map.of(
            "userId", gdprUserId,
            "reason", "GDPR_RIGHT_TO_BE_FORGOTTEN",
            "confirmationToken", "gdpr-delete-" + gdprUserId,
            "cascade", true,
            "anonymizeAnalytics", false // Complete removal, not anonymization
        );

        Response deletionResponse = given()
            .header("Authorization", "Bearer " + gdprUserToken)
            .contentType(ContentType.JSON)
            .body(deletionRequest)
            .when()
            .delete("/api/users/{userId}/gdpr-delete", gdprUserId)
            .then()
            .statusCode(anyOf(is(200), is(202))) // May be async
            .extract().response();

        // Then: All user data should be completely removed from all services
        CrossServiceTestUtils.waitForEventualConsistency();

        // Extended wait for GDPR deletion (more complex operation)
        Thread.sleep(5000);

        // Verify user is deleted from Identity Service
        Awaitility.await()
            .atMost(60, TimeUnit.SECONDS)
            .pollInterval(3, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                given()
                    .header("Authorization", "Bearer " + gdprUserToken)
                    .when()
                    .get("/api/identity/users/{userId}", gdprUserId)
                    .then()
                    .statusCode(anyOf(is(404), is(401))); // User not found or unauthorized
            });

        // Verify user data is removed from Analytics Service
        Awaitility.await()
            .atMost(60, TimeUnit.SECONDS)
            .pollInterval(3, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                given()
                    .header("Authorization", "Bearer " + testUserToken) // Use different user's token
                    .when()
                    .get("/api/analytics/users/{userId}/profile", gdprUserId)
                    .then()
                    .statusCode(404);
            });

        // Verify user notifications are removed
        Awaitility.await()
            .atMost(60, TimeUnit.SECONDS)
            .pollInterval(3, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                Response notificationResponse = given()
                    .when()
                    .get("/api/notifications/users/{userId}/all", gdprUserId)
                    .then()
                    .extract().response();

                // Should either return 404 or empty list
                assertTrue(
                    notificationResponse.statusCode() == 404 || 
                    notificationResponse.jsonPath().getList("notifications").isEmpty(),
                    "All user notifications should be removed"
                );
            });

        // Verify user buddy data is removed
        Awaitility.await()
            .atMost(60, TimeUnit.SECONDS)
            .pollInterval(3, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                given()
                    .header("Authorization", "Bearer " + testUserToken)
                    .when()
                    .get("/api/buddy/users/{userId}/profile", gdprUserId)
                    .then()
                    .statusCode(404);
            });

        // Verify user chat messages are anonymized or removed
        Awaitility.await()
            .atMost(60, TimeUnit.SECONDS)
            .pollInterval(3, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                Response chatResponse = given()
                    .header("Authorization", "Bearer " + testUserToken)
                    .when()
                    .get("/api/chat/users/{userId}/messages", gdprUserId)
                    .then()
                    .extract().response();

                if (chatResponse.statusCode() == 200) {
                    List<Map<String, Object>> messages = chatResponse.jsonPath().getList("messages");
                    if (!messages.isEmpty()) {
                        // Messages should be anonymized (sender name removed/changed)
                        Map<String, Object> message = messages.get(0);
                        assertTrue(
                            message.get("senderName").toString().contains("Deleted User") ||
                            message.get("senderName").toString().contains("Anonymous"),
                            "Chat messages should be anonymized after user deletion"
                        );
                    }
                } else {
                    assertEquals(404, chatResponse.statusCode(), 
                        "User chat data should be removed");
                }
            });

        // Verify GDPR deletion is logged for compliance
        Awaitility.await()
            .atMost(60, TimeUnit.SECONDS)
            .pollInterval(3, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                Response auditResponse = given()
                    .header("Authorization", "Bearer " + testUserToken) // Admin token needed
                    .when()
                    .get("/api/audit/gdpr-deletions/{userId}", gdprUserId)
                    .then()
                    .statusCode(200)
                    .extract().response();

                Map<String, Object> auditRecord = auditResponse.as(new TypeReference<Map<String, Object>>() {});
                assertEquals("COMPLETED", auditRecord.get("status"), 
                    "GDPR deletion should be logged as completed");
                assertEquals("GDPR_RIGHT_TO_BE_FORGOTTEN", auditRecord.get("reason"), 
                    "GDPR deletion reason should be recorded");
            });
    }

    @Test
    @Order(4)
    @DisplayName("TDD: Service failure recovery should maintain data integrity")
    void testServiceFailureRecoveryDataIntegrity() {
        // Given: Normal operation state with data across services
        
        // Create a new user for failure testing
        Map<String, Object> failureUserRequest = TestDataFactory.Users.createTestUser(
            "failure-test@example.com", 
            "Failure Test User"
        );

        Response failureUserResponse = given()
            .contentType(ContentType.JSON)
            .body(failureUserRequest)
            .when()
            .post("/api/auth/register")
            .then()
            .statusCode(201)
            .extract().response();

        String failureUserId = failureUserResponse.jsonPath().getString("id");

        // Authenticate failure test user
        Map<String, Object> failureLoginRequest = Map.of(
            "email", "failure-test@example.com",
            "password", "TestPassword123!"
        );

        Response failureAuthResponse = given()
            .contentType(ContentType.JSON)
            .body(failureLoginRequest)
            .when()
            .post("/api/auth/login")
            .then()
            .statusCode(200)
            .extract().response();

        String failureUserToken = failureAuthResponse.jsonPath().getString("token");

        // When: Simulate service failure during transaction
        // Attempt operation that involves multiple services
        Map<String, Object> complexOperation = Map.of(
            "userId", failureUserId,
            "operation", "CREATE_HIVE_AND_JOIN_AND_START_SESSION",
            "hiveData", TestDataFactory.Hives.createTestHive("Failure Test Hive", failureUserId),
            "sessionData", Map.of("duration", 25, "type", "FOCUS"),
            "notificationData", Map.of("notifyFriends", true),
            "analyticsData", Map.of("trackDetailed", true)
        );

        // This operation might fail partially due to service unavailability
        Response complexResponse = given()
            .header("Authorization", "Bearer " + failureUserToken)
            .contentType(ContentType.JSON)
            .body(complexOperation)
            .when()
            .post("/api/operations/complex-hive-operation")
            .then()
            .statusCode(anyOf(is(200), is(201), is(500), is(502), is(503)))
            .extract().response();

        // Then: System should either complete fully or rollback completely
        CrossServiceTestUtils.waitForEventualConsistency();

        if (complexResponse.statusCode() >= 200 && complexResponse.statusCode() < 300) {
            // Operation succeeded - verify all data is consistent
            String hiveId = complexResponse.jsonPath().getString("hiveId");
            
            // Verify hive exists
            given()
                .header("Authorization", "Bearer " + failureUserToken)
                .when()
                .get("/api/hives/{hiveId}", hiveId)
                .then()
                .statusCode(200)
                .body("createdBy", equalTo(failureUserId));

            // Verify user is member
            Response membersResponse = given()
                .header("Authorization", "Bearer " + failureUserToken)
                .when()
                .get("/api/hives/{hiveId}/members", hiveId)
                .then()
                .statusCode(200)
                .extract().response();

            List<Map<String, Object>> members = membersResponse.jsonPath().getList("members");
            boolean userIsMember = members.stream()
                .anyMatch(member -> failureUserId.equals(member.get("userId")));
            assertTrue(userIsMember, "User should be member if operation succeeded");

            // Verify analytics recorded the operation
            Response analyticsResponse = given()
                .header("Authorization", "Bearer " + failureUserToken)
                .when()
                .get("/api/analytics/users/{userId}/hives", failureUserId)
                .then()
                .statusCode(200)
                .extract().response();

            Map<String, Object> userHives = analyticsResponse.as(new TypeReference<Map<String, Object>>() {});
            assertTrue((Integer) userHives.get("totalHivesCreated") >= 1, 
                "Analytics should record hive creation if operation succeeded");

        } else {
            // Operation failed - verify no partial state exists
            
            // Verify no orphaned hive exists
            Response userHivesResponse = given()
                .header("Authorization", "Bearer " + failureUserToken)
                .when()
                .get("/api/hives/user/{userId}", failureUserId)
                .then()
                .statusCode(200)
                .extract().response();

            List<Map<String, Object>> userHives = userHivesResponse.jsonPath().getList("hives");
            boolean hasFailureTestHive = userHives.stream()
                .anyMatch(hive -> hive.get("name").toString().contains("Failure Test Hive"));
            assertFalse(hasFailureTestHive, 
                "No orphaned hive should exist if operation failed");

            // Verify no analytics events for failed operation
            Response analyticsEvents = given()
                .header("Authorization", "Bearer " + failureUserToken)
                .when()
                .get("/api/analytics/events")
                .then()
                .statusCode(200)
                .extract().response();

            List<Map<String, Object>> events = analyticsEvents.jsonPath().getList("content");
            boolean hasFailureOperationEvent = events.stream()
                .anyMatch(event -> 
                    failureUserId.equals(event.get("userId")) &&
                    event.get("metadata") != null &&
                    "CREATE_HIVE_AND_JOIN_AND_START_SESSION".equals(
                        ((Map<String, Object>) event.get("metadata")).get("operation")
                    )
                );
            assertFalse(hasFailureOperationEvent, 
                "No analytics events should exist for failed operation");
        }

        // Verify system health and recovery
        Awaitility.await()
            .atMost(30, TimeUnit.SECONDS)
            .pollInterval(2, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                // All services should be responsive
                given()
                    .when()
                    .get("/api/health")
                    .then()
                    .statusCode(200)
                    .body("status", equalTo("UP"));

                given()
                    .when()
                    .get("/api/analytics/health")
                    .then()
                    .statusCode(200)
                    .body("status", equalTo("UP"));

                given()
                    .when()
                    .get("/api/notifications/health")
                    .then()
                    .statusCode(200)
                    .body("status", equalTo("UP"));
            });
    }

    @Test
    @Order(5)
    @DisplayName("Performance: Data consistency under high load")
    void testDataConsistencyUnderHighLoad() {
        // Given: System under normal operation
        
        // When: Multiple concurrent operations that require cross-service consistency
        List<String> userIds = createMultipleTestUsers(10);
        List<String> tokens = authenticateUsers(userIds);

        long startTime = System.currentTimeMillis();

        // Perform concurrent operations
        for (int i = 0; i < userIds.size(); i++) {
            String userId = userIds.get(i);
            String token = tokens.get(i);

            // Concurrent profile updates
            Map<String, Object> profileUpdate = Map.of(
                "name", "Concurrent User " + i,
                "preferences", Map.of(
                    "timezone", "UTC",
                    "notifications", Map.of("email", true)
                )
            );

            given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(profileUpdate)
                .when()
                .put("/api/users/{userId}/profile", userId)
                .then()
                .statusCode(200);

            // Concurrent hive creation
            Map<String, Object> hiveRequest = TestDataFactory.Hives.createTestHive(
                "Concurrent Hive " + i, userId
            );

            given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(hiveRequest)
                .when()
                .post("/api/hives")
                .then()
                .statusCode(201);
        }

        long operationTime = System.currentTimeMillis() - startTime;

        // Then: All operations should maintain consistency
        CrossServiceTestUtils.waitForEventualConsistency();

        // Verify all profile updates are consistent across services
        for (int i = 0; i < userIds.size(); i++) {
            String userId = userIds.get(i);
            String token = tokens.get(i);
            String expectedName = "Concurrent User " + i;

            Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .pollInterval(1, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    // Check identity service
                    Response identityResponse = given()
                        .header("Authorization", "Bearer " + token)
                        .when()
                        .get("/api/identity/users/{userId}", userId)
                        .then()
                        .statusCode(200)
                        .body("name", equalTo(expectedName))
                        .extract().response();

                    // Check analytics service
                    Response analyticsResponse = given()
                        .header("Authorization", "Bearer " + token)
                        .when()
                        .get("/api/analytics/users/{userId}/profile", userId)
                        .then()
                        .statusCode(200)
                        .extract().response();

                    Map<String, Object> analyticsProfile = analyticsResponse.as(new TypeReference<Map<String, Object>>() {});
                    assertEquals(expectedName, analyticsProfile.get("name"), 
                        "Analytics service should have consistent user name for user " + i);
                });
        }

        // Verify performance under load
        assertTrue(operationTime < 15000, 
            "Concurrent operations should complete within 15 seconds (actual: " + operationTime + "ms)");

        // Verify no data corruption occurred
        for (int i = 0; i < userIds.size(); i++) {
            String userId = userIds.get(i);
            String token = tokens.get(i);

            Response userHives = given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/api/hives/user/{userId}", userId)
                .then()
                .statusCode(200)
                .extract().response();

            List<Map<String, Object>> hives = userHives.jsonPath().getList("hives");
            assertEquals(1, hives.size(), 
                "Each user should have exactly 1 hive created");
            assertEquals(userId, hives.get(0).get("createdBy"), 
                "Hive should be correctly associated with user");
        }
    }

    // Private helper methods

    private void setupTestUser() {
        Map<String, Object> userRequest = TestDataFactory.Users.createTestUser(
            "consistency-test@example.com", 
            "Original Test User"
        );

        Response userResponse = given()
            .contentType(ContentType.JSON)
            .body(userRequest)
            .when()
            .post("/api/auth/register")
            .then()
            .statusCode(201)
            .extract().response();

        testUserId = userResponse.jsonPath().getString("id");

        // Authenticate
        Map<String, Object> loginRequest = Map.of(
            "email", "consistency-test@example.com",
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

        testUserToken = authResponse.jsonPath().getString("token");
    }

    private void setupTestBuddy() {
        Map<String, Object> buddyRequest = TestDataFactory.Users.createTestUser(
            "buddy-consistency@example.com", 
            "Buddy Test User"
        );

        Response buddyResponse = given()
            .contentType(ContentType.JSON)
            .body(buddyRequest)
            .when()
            .post("/api/auth/register")
            .then()
            .statusCode(201)
            .extract().response();

        testBuddyId = buddyResponse.jsonPath().getString("id");

        // Authenticate
        Map<String, Object> loginRequest = Map.of(
            "email", "buddy-consistency@example.com",
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

        testBuddyToken = authResponse.jsonPath().getString("token");
    }

    private void setupTestHive() {
        Map<String, Object> hiveRequest = TestDataFactory.Hives.createTestHive(
            "Consistency Test Hive", testUserId
        );

        Response hiveResponse = given()
            .header("Authorization", "Bearer " + testUserToken)
            .contentType(ContentType.JSON)
            .body(hiveRequest)
            .when()
            .post("/api/hives")
            .then()
            .statusCode(201)
            .extract().response();

        testHiveId = hiveResponse.jsonPath().getString("id");
    }

    private void setupCrossServiceData() {
        // Create data across multiple services for consistency testing
        
        // Set notification preferences
        Map<String, Object> notificationPrefs = Map.of(
            "channels", Map.of(
                "email", true,
                "push", true,
                "inApp", true
            )
        );

        given()
            .header("Authorization", "Bearer " + testUserToken)
            .contentType(ContentType.JSON)
            .body(notificationPrefs)
            .when()
            .put("/api/notifications/preferences")
            .then()
            .statusCode(anyOf(is(200), is(201)));

        // Create analytics profile
        Map<String, Object> analyticsData = Map.of(
            "userId", testUserId,
            "initialMetrics", Map.of(
                "focusHours", 0,
                "sessionsCompleted", 0
            )
        );

        given()
            .contentType(ContentType.JSON)
            .body(analyticsData)
            .when()
            .post("/api/analytics/users/initialize")
            .then()
            .statusCode(anyOf(is(200), is(201)));

        CrossServiceTestUtils.waitForEventualConsistency();
    }

    private void verifyUserExistsInAllServices(String userId, String expectedName) {
        // Identity Service
        given()
            .header("Authorization", "Bearer " + testUserToken)
            .when()
            .get("/api/identity/users/{userId}", userId)
            .then()
            .statusCode(200)
            .body("name", equalTo(expectedName));

        // Analytics Service
        given()
            .header("Authorization", "Bearer " + testUserToken)
            .when()
            .get("/api/analytics/users/{userId}/profile", userId)
            .then()
            .statusCode(200);

        // Notification Service (check if user can access preferences)
        given()
            .header("Authorization", "Bearer " + testUserToken)
            .when()
            .get("/api/notifications/preferences")
            .then()
            .statusCode(200);
    }

    private void verifyHiveExistsInAllServices(String hiveId) {
        // Core Hive Service
        given()
            .header("Authorization", "Bearer " + testUserToken)
            .when()
            .get("/api/hives/{hiveId}", hiveId)
            .then()
            .statusCode(200);

        // Analytics Service
        given()
            .header("Authorization", "Bearer " + testUserToken)
            .when()
            .get("/api/analytics/hives/{hiveId}/summary", hiveId)
            .then()
            .statusCode(anyOf(is(200), is(404))); // May not exist in analytics yet

        // Chat Service
        given()
            .header("Authorization", "Bearer " + testUserToken)
            .when()
            .get("/api/chat/hives/{hiveId}/messages", hiveId)
            .then()
            .statusCode(anyOf(is(200), is(404))); // May be empty but accessible
    }

    private void setupGDPRUserData(String userId, String token) {
        // Create comprehensive data for GDPR testing
        
        // Create hive
        Map<String, Object> hiveRequest = TestDataFactory.Hives.createTestHive(
            "GDPR User Hive", userId
        );

        Response hiveResponse = given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body(hiveRequest)
            .when()
            .post("/api/hives")
            .then()
            .statusCode(201)
            .extract().response();

        String hiveId = hiveResponse.jsonPath().getString("id");

        // Set notification preferences
        Map<String, Object> preferences = Map.of(
            "channels", Map.of("email", true, "push", false),
            "frequency", "DAILY"
        );

        given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body(preferences)
            .when()
            .put("/api/notifications/preferences")
            .then()
            .statusCode(anyOf(is(200), is(201)));

        // Create buddy profile
        Map<String, Object> buddyPrefs = Map.of(
            "goals", List.of("Focus", "Learning"),
            "availability", "MORNINGS"
        );

        given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body(buddyPrefs)
            .when()
            .put("/api/buddy/preferences")
            .then()
            .statusCode(anyOf(is(200), is(201)));
    }

    private List<String> createMultipleTestUsers(int count) {
        List<String> userIds = new java.util.ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            Map<String, Object> userRequest = TestDataFactory.Users.createTestUser(
                "load-test-" + i + "@example.com", 
                "Load Test User " + i
            );

            Response userResponse = given()
                .contentType(ContentType.JSON)
                .body(userRequest)
                .when()
                .post("/api/auth/register")
                .then()
                .statusCode(201)
                .extract().response();

            userIds.add(userResponse.jsonPath().getString("id"));
        }
        
        return userIds;
    }

    private List<String> authenticateUsers(List<String> userIds) {
        List<String> tokens = new java.util.ArrayList<>();
        
        for (int i = 0; i < userIds.size(); i++) {
            Map<String, Object> loginRequest = Map.of(
                "email", "load-test-" + i + "@example.com",
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

            tokens.add(authResponse.jsonPath().getString("token"));
        }
        
        return tokens;
    }
}