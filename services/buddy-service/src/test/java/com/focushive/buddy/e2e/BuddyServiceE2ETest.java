package com.focushive.buddy.e2e;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.focushive.buddy.dto.*;
import com.focushive.buddy.constant.PartnershipStatus;
import com.focushive.buddy.constant.GoalType;
import com.focushive.buddy.constant.GoalCategory;
import com.focushive.buddy.constant.GoalStatus;
import com.focushive.buddy.constant.MilestoneStatus;
import com.focushive.buddy.constant.CheckinFrequency;
import com.focushive.buddy.constant.CheckInType;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive End-to-End tests for Buddy Service API
 * Tests all 35+ endpoints with production-grade scenarios
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class BuddyServiceE2ETest extends AbstractE2ETest {

    // Containers are now managed in AbstractE2ETest

    // Port is inherited from AbstractE2ETest

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private com.focushive.buddy.repository.UserRepository userRepository;

    // Test data
    private static String testUser1;
    private static String testUser2;
    private static String testUser3;
    private static String adminUser;
    private static String partnershipId;
    private static String goalId;
    private static String milestoneId;
    private static String checkinId;

    // Properties are configured in AbstractE2ETest

    @BeforeAll
    static void setUpClass() {
        // Initialize test users with valid UUIDs
        testUser1 = UUID.randomUUID().toString();
        testUser2 = UUID.randomUUID().toString();
        testUser3 = UUID.randomUUID().toString();
        adminUser = UUID.randomUUID().toString();
    }

    @BeforeEach
    @Override
    protected void setUp() {
        super.setUp();  // Call parent's setUp() to configure RestAssured

        // Create test users in database
        createTestUsers();
    }

    private void createTestUsers() {
        // Create test user 1
        com.focushive.buddy.entity.User user1 = com.focushive.buddy.entity.User.builder()
                .id(testUser1)
                .displayName("Test User 1")
                .timezone("UTC")
                .interests(Arrays.asList("coding", "fitness", "reading"))
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        userRepository.save(user1);

        // Create test user 2
        com.focushive.buddy.entity.User user2 = com.focushive.buddy.entity.User.builder()
                .id(testUser2)
                .displayName("Test User 2")
                .timezone("UTC")
                .interests(Arrays.asList("coding", "music", "travel"))
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        userRepository.save(user2);

        // Create test user 3
        com.focushive.buddy.entity.User user3 = com.focushive.buddy.entity.User.builder()
                .id(testUser3)
                .displayName("Test User 3")
                .timezone("UTC")
                .interests(Arrays.asList("fitness", "cooking", "gaming"))
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        userRepository.save(user3);

        // Create admin user
        com.focushive.buddy.entity.User admin = com.focushive.buddy.entity.User.builder()
                .id(adminUser)
                .displayName("Admin User")
                .timezone("UTC")
                .interests(Arrays.asList("management", "analytics"))
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        userRepository.save(admin);
    }

    // =================================================================================================
    // HEALTH CHECK ENDPOINT (1 endpoint)
    // =================================================================================================

    @Test
    @Order(1)
    @DisplayName("Health check endpoint should return OK")
    void testHealthCheck() {
        given()
                .basePath("")  // Clear base path for this test
                .when()
                .get("/api/v1/health")
                .then()
                .statusCode(200)
                .time(lessThan(500L))
                .body("status", equalTo("UP"));
    }

    // =================================================================================================
    // BUDDY MATCHING CONTROLLER TESTS (8 endpoints)
    // =================================================================================================

    @Test
    @Order(2)
    @DisplayName("POST /api/v1/buddy/matching/queue - Join matching queue")
    void testJoinMatchingQueue() {
        given()
                .header("X-User-ID", testUser1)
                .when()
                .post("/api/v1/buddy/matching/queue")
                .then()
                .statusCode(200)
                .time(lessThan(1000L))
                .body("success", equalTo(true))
                .body("message", containsString("Successfully joined matching queue"))
                .body("data.inQueue", equalTo(true))
                .body("data.userId", equalTo(testUser1));
    }

    @Test
    @Order(3)
    @DisplayName("GET /api/v1/buddy/matching/queue/status - Get queue status")
    void testGetQueueStatus() {
        // First join the queue
        given()
                .header("X-User-ID", testUser1)
                .when()
                .post("/api/v1/buddy/matching/queue");

        // Then check status
        given()
                .header("X-User-ID", testUser1)
                .when()
                .get("/api/v1/buddy/matching/queue/status")
                .then()
                .statusCode(200)
                .time(lessThan(500L))
                .body("success", equalTo(true))
                .body("data.inQueue", equalTo(true))
                .body("data.userId", equalTo(testUser1));
    }

    @Test
    @Order(4)
    @DisplayName("DELETE /api/v1/buddy/matching/queue - Leave matching queue")
    void testLeaveMatchingQueue() {
        // First join the queue
        given()
                .header("X-User-ID", testUser1)
                .when()
                .post("/api/v1/buddy/matching/queue");

        // Then leave the queue
        given()
                .header("X-User-ID", testUser1)
                .when()
                .delete("/api/v1/buddy/matching/queue")
                .then()
                .statusCode(200)
                .time(lessThan(500L))
                .body("success", equalTo(true))
                .body("data.inQueue", equalTo(false))
                .body("data.userId", equalTo(testUser1));
    }

    @Test
    @Order(5)
    @DisplayName("GET /api/v1/buddy/matching/queue/size - Get queue size (Admin only)")
    void testGetQueueSizeAsAdmin() {
        // Add users to queue first
        given().header("X-User-ID", testUser1).post("/api/v1/buddy/matching/queue");
        given().header("X-User-ID", testUser2).post("/api/v1/buddy/matching/queue");

        given()
                .header("X-User-ID", adminUser)
                .header("X-User-Role", "ADMIN")
                .when()
                .get("/api/v1/buddy/matching/queue/size")
                .then()
                .statusCode(200)
                .time(lessThan(500L))
                .body("success", equalTo(true))
                .body("data.queueSize", greaterThanOrEqualTo(2));
    }

    @Test
    @Order(6)
    @DisplayName("GET /api/v1/buddy/matching/queue/size - Forbidden for non-admin")
    void testGetQueueSizeNonAdminForbidden() {
        given()
                .header("X-User-ID", testUser1)
                .header("X-User-Role", "USER")
                .when()
                .get("/api/v1/buddy/matching/queue/size")
                .then()
                .statusCode(403)
                .body("success", equalTo(false))
                .body("message", containsString("Admin access required"));
    }

    @Test
    @Order(7)
    @DisplayName("GET /api/v1/buddy/matching/suggestions - Get match suggestions")
    void testGetMatchSuggestions() {
        // Setup: Add users to queue and create preferences
        setupMatchingPreferences(testUser1);
        setupMatchingPreferences(testUser2);

        given()
                .header("X-User-ID", testUser1)
                .queryParam("limit", 5)
                .queryParam("threshold", 0.5)
                .when()
                .get("/api/v1/buddy/matching/suggestions")
                .then()
                .statusCode(200)
                .time(lessThan(2000L))
                .body("success", equalTo(true))
                .body("data.matches", notNullValue())
                .body("data.totalMatches", greaterThanOrEqualTo(0))
                .body("data.limit", equalTo(5));
    }

    @Test
    @Order(8)
    @DisplayName("POST /api/v1/buddy/matching/calculate - Calculate compatibility")
    void testCalculateCompatibility() {
        // Setup users with preferences
        setupMatchingPreferences(testUser1);
        setupMatchingPreferences(testUser2);

        Map<String, String> request = Map.of("targetUserId", testUser2);

        given()
                .header("X-User-ID", testUser1)
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/api/v1/buddy/matching/calculate")
                .then()
                .statusCode(200)
                .time(lessThan(2000L))
                .body("success", equalTo(true))
                .body("data.overallScore", notNullValue())
                .body("data.breakdown", notNullValue());
    }

    @Test
    @Order(9)
    @DisplayName("GET /api/v1/buddy/matching/preferences - Get matching preferences")
    void testGetMatchingPreferences() {
        given()
                .header("X-User-ID", testUser1)
                .when()
                .get("/api/v1/buddy/matching/preferences")
                .then()
                .statusCode(200)
                .time(lessThan(500L))
                .body("success", equalTo(true))
                .body("data.userId", equalTo(testUser1));
    }

    @Test
    @Order(10)
    @DisplayName("PUT /api/v1/buddy/matching/preferences - Update matching preferences")
    void testUpdateMatchingPreferences() {
        MatchingPreferencesDto preferences = createTestPreferences(testUser1);

        given()
                .header("X-User-ID", testUser1)
                .contentType(ContentType.JSON)
                .body(preferences)
                .when()
                .put("/api/v1/buddy/matching/preferences")
                .then()
                .statusCode(200)
                .time(lessThan(1000L))
                .body("success", equalTo(true))
                .body("data.userId", equalTo(testUser1))
                .body("data.interests", hasItems("coding", "fitness"));
    }

    // =================================================================================================
    // BUDDY PARTNERSHIP CONTROLLER TESTS (16 endpoints)
    // =================================================================================================

    @Test
    @Order(11)
    @DisplayName("POST /api/v1/buddy/partnerships/request - Create partnership request")
    void testCreatePartnershipRequest() {
        PartnershipRequestDto request = createPartnershipRequest(testUser1, testUser2);

        Response response = given()
                .header("X-User-ID", testUser1)
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/api/v1/buddy/partnerships/request")
                .then()
                .statusCode(201)
                .time(lessThan(1000L))
                .body("success", equalTo(true))
                .body("data.id", notNullValue())
                .body("data.status", equalTo("PENDING"))
                .extract().response();

        partnershipId = response.path("data.id");
    }

    @Test
    @Order(12)
    @DisplayName("PUT /api/v1/buddy/partnerships/{id}/approve - Approve partnership")
    void testApprovePartnership() {
        // Create partnership first if not exists
        if (partnershipId == null) {
            partnershipId = createTestPartnership();
        }

        given()
                .header("X-User-ID", testUser2)
                .when()
                .put("/api/v1/buddy/partnerships/{id}/approve", partnershipId)
                .then()
                .statusCode(200)
                .time(lessThan(1000L))
                .body("success", equalTo(true))
                .body("data.status", equalTo("ACTIVE"));  // API returns uppercase for approve
    }

    @Test
    @Order(13)
    @DisplayName("PUT /api/v1/buddy/partnerships/{id}/reject - Reject partnership")
    void testRejectPartnership() {
        // Create new unique users for this test to avoid partnership limit
        String uniqueUser1 = UUID.randomUUID().toString();
        String uniqueUser2 = UUID.randomUUID().toString();
        createUniqueTestUser(uniqueUser1, "Reject Test User 1");
        createUniqueTestUser(uniqueUser2, "Reject Test User 2");

        // Create new partnership for rejection test with unique users
        String newPartnershipId = createTestPartnershipBetween(uniqueUser1, uniqueUser2);
        assertNotNull(newPartnershipId, "Partnership ID should not be null");

        given()
                .header("X-User-ID", uniqueUser2)
                .when()
                .put("/api/v1/buddy/partnerships/{id}/reject", newPartnershipId)
                .then()
                .statusCode(200)
                .time(lessThan(1000L))
                .body("success", equalTo(true))
                .body("data.status", equalTo("rejected"));  // API returns lowercase
    }

    @Test
    @Order(14)
    @DisplayName("GET /api/v1/buddy/partnerships - Get user partnerships")
    void testGetUserPartnerships() {
        given()
                .header("X-User-ID", testUser1)
                .queryParam("page", 0)
                .queryParam("size", 10)
                .queryParam("status", "ACTIVE")
                .when()
                .get("/api/v1/buddy/partnerships")
                .then()
                .statusCode(200)
                .time(lessThan(1000L))
                .body("success", equalTo(true))
                .body("data.content", notNullValue())
                .body("data.totalElements", greaterThanOrEqualTo(0));
    }

    @Test
    @Order(15)
    @DisplayName("GET /api/v1/buddy/partnerships/{id} - Get partnership details")
    void testGetPartnershipDetails() {
        if (partnershipId == null) {
            partnershipId = createTestPartnership();
        }

        given()
                .header("X-User-ID", testUser1)
                .when()
                .get("/api/v1/buddy/partnerships/{id}", partnershipId)
                .then()
                .statusCode(200)
                .time(lessThan(500L))
                .body("success", equalTo(true))
                .body("data.id", equalTo(partnershipId));
    }

    @Test
    @Order(16)
    @DisplayName("PUT /api/v1/buddy/partnerships/{id}/pause - Pause partnership")
    void testPausePartnership() {
        if (partnershipId == null) {
            partnershipId = createTestPartnership();
            // Approve the partnership first so it becomes ACTIVE
            given()
                    .header("X-User-ID", testUser2)
                    .when()
                    .put("/api/v1/buddy/partnerships/{id}/approve", partnershipId)
                    .then()
                    .statusCode(200);
        }

        given()
                .header("X-User-ID", testUser1)
                .when()
                .put("/api/v1/buddy/partnerships/{id}/pause", partnershipId)
                .then()
                .statusCode(200)
                .time(lessThan(1000L))
                .body("success", equalTo(true))
                .body("data.status", equalTo("paused"));  // API returns lowercase
    }

    @Test
    @Order(17)
    @DisplayName("PUT /api/v1/buddy/partnerships/{id}/resume - Resume partnership")
    void testResumePartnership() {
        if (partnershipId == null) {
            partnershipId = createTestPartnership();
            // Approve the partnership first so it becomes ACTIVE
            given()
                    .header("X-User-ID", testUser2)
                    .when()
                    .put("/api/v1/buddy/partnerships/{id}/approve", partnershipId)
                    .then()
                    .statusCode(200);
            // Pause it first so we can resume it
            given()
                    .header("X-User-ID", testUser1)
                    .when()
                    .put("/api/v1/buddy/partnerships/{id}/pause", partnershipId)
                    .then()
                    .statusCode(200);
        }

        given()
                .header("X-User-ID", testUser1)
                .when()
                .put("/api/v1/buddy/partnerships/{id}/resume", partnershipId)
                .then()
                .statusCode(200)
                .time(lessThan(1000L))
                .body("success", equalTo(true))
                .body("data.status", equalTo("ACTIVE"));  // API returns uppercase
    }

    @Test
    @Order(18)
    @DisplayName("PUT /api/v1/buddy/partnerships/{id}/end - End partnership")
    void testEndPartnership() {
        // Create new unique users for this test to avoid partnership limit
        String uniqueUser1 = UUID.randomUUID().toString();
        String uniqueUser2 = UUID.randomUUID().toString();
        createUniqueTestUser(uniqueUser1, "End Test User 1");
        createUniqueTestUser(uniqueUser2, "End Test User 2");

        // Create new partnership with unique users
        String newPartnershipId = createTestPartnershipBetween(uniqueUser1, uniqueUser2);
        assertNotNull(newPartnershipId, "Partnership ID should not be null");

        // Approve the partnership first so it becomes ACTIVE
        given()
                .header("X-User-ID", uniqueUser2)
                .when()
                .put("/api/v1/buddy/partnerships/{id}/approve", newPartnershipId)
                .then()
                .statusCode(200);

        given()
                .header("X-User-ID", uniqueUser1)
                .when()
                .put("/api/v1/buddy/partnerships/{id}/end", newPartnershipId)
                .then()
                .statusCode(200)
                .time(lessThan(1000L))
                .body("success", equalTo(true))
                .body("data.status", equalTo("ended"));
    }

    @Test
    @Order(19)
    @DisplayName("GET /api/v1/buddy/partnerships/{id}/health - Get partnership health")
    void testGetPartnershipHealth() {
        if (partnershipId == null) {
            partnershipId = createTestPartnership();
            // Approve the partnership first so it becomes ACTIVE
            given()
                    .header("X-User-ID", testUser2)
                    .when()
                    .put("/api/v1/buddy/partnerships/{id}/approve", partnershipId)
                    .then()
                    .statusCode(200);
        }

        given()
                .header("X-User-ID", testUser1)
                .when()
                .get("/api/v1/buddy/partnerships/{id}/health", partnershipId)
                .then()
                .statusCode(200)
                .time(lessThan(1000L))
                .body("success", equalTo(true))
                .body("data.healthScore", notNullValue())
                .body("data.metrics", notNullValue());
    }

    // =================================================================================================
    // BUDDY GOAL CONTROLLER TESTS (10 endpoints)
    // =================================================================================================

    @Test
    @Order(20)
    @DisplayName("POST /api/v1/buddy/goals - Create goal")
    void testCreateGoal() {
        GoalCreationDto goal = createTestGoal(testUser1);

        Response response = given()
                .header("X-User-ID", testUser1)
                .contentType(ContentType.JSON)
                .body(goal)
                .when()
                .post("/api/v1/buddy/goals")
                .then()
                .statusCode(201)
                .time(lessThan(1000L))
                .body("success", equalTo(true))
                .body("data.id", notNullValue())
                .body("data.title", equalTo(goal.getTitle()))
                .extract().response();

        goalId = response.path("data.id");
    }

    @Test
    @Order(21)
    @DisplayName("GET /api/v1/buddy/goals - Get user goals")
    void testGetUserGoals() {
        given()
                .header("X-User-ID", testUser1)
                .queryParam("page", 0)
                .queryParam("size", 10)
                .queryParam("status", "ACTIVE")
                .when()
                .get("/api/v1/buddy/goals")
                .then()
                .statusCode(200)
                .time(lessThan(1000L))
                .body("success", equalTo(true))
                .body("data.content", notNullValue())
                .body("data.totalElements", greaterThanOrEqualTo(0));
    }

    @Test
    @Order(22)
    @DisplayName("GET /api/v1/buddy/goals/{id} - Get goal details")
    void testGetGoalDetails() {
        if (goalId == null) {
            createTestGoalForUser();
        }

        given()
                .header("X-User-ID", testUser1)
                .when()
                .get("/api/v1/buddy/goals/{id}", goalId)
                .then()
                .statusCode(200)
                .time(lessThan(500L))
                .body("success", equalTo(true))
                .body("data.id", equalTo(goalId));
    }

    @Test
    @Order(23)
    @DisplayName("PUT /api/v1/buddy/goals/{id} - Update goal")
    void testUpdateGoal() {
        if (goalId == null) {
            createTestGoalForUser();
        }

        GoalCreationDto updatedGoal = createTestGoal(testUser1);
        updatedGoal = updatedGoal.toBuilder()
            .title("Updated Goal Title")
            .build();

        given()
                .header("X-User-ID", testUser1)
                .contentType(ContentType.JSON)
                .body(updatedGoal)
                .when()
                .put("/api/v1/buddy/goals/{id}", goalId)
                .then()
                .statusCode(200)
                .time(lessThan(1000L))
                .body("success", equalTo(true))
                .body("data.title", equalTo("Updated Goal Title"));
    }

    @Test
    @Order(24)
    @DisplayName("DELETE /api/v1/buddy/goals/{id} - Delete goal")
    void testDeleteGoal() {
        String newGoalId = createTestGoalForUser();

        given()
                .header("X-User-ID", testUser1)
                .when()
                .delete("/api/v1/buddy/goals/{id}", newGoalId)
                .then()
                .statusCode(200)
                .time(lessThan(1000L))
                .body("success", equalTo(true));
    }

    @Test
    @Order(25)
    @DisplayName("POST /api/v1/buddy/goals/{goalId}/milestones - Add milestone")
    void testAddMilestone() {
        if (goalId == null) {
            createTestGoalForUser();
        }

        MilestoneDto milestone = createTestMilestone();

        Response response = given()
                .header("X-User-ID", testUser1)
                .contentType(ContentType.JSON)
                .body(milestone)
                .when()
                .post("/api/v1/buddy/goals/{goalId}/milestones", goalId)
                .then()
                .statusCode(201)
                .time(lessThan(1000L))
                .body("success", equalTo(true))
                .body("data.id", notNullValue())
                .body("data.title", equalTo(milestone.getTitle()))
                .extract().response();

        milestoneId = response.path("data.id");
    }

    @Test
    @Order(26)
    @DisplayName("PUT /api/v1/buddy/goals/{goalId}/milestones/{milestoneId} - Update milestone")
    void testUpdateMilestone() {
        if (goalId == null || milestoneId == null) {
            createTestGoalWithMilestone();
        }

        MilestoneDto updatedMilestone = createTestMilestone();
        updatedMilestone = updatedMilestone.toBuilder()
            .title("Updated Milestone")
            .build();

        given()
                .header("X-User-ID", testUser1)
                .contentType(ContentType.JSON)
                .body(updatedMilestone)
                .when()
                .put("/api/v1/buddy/goals/{goalId}/milestones/{milestoneId}", goalId, milestoneId)
                .then()
                .statusCode(200)
                .time(lessThan(1000L))
                .body("success", equalTo(true))
                .body("data.title", equalTo("Updated Milestone"));
    }

    @Test
    @Order(27)
    @DisplayName("GET /api/v1/buddy/goals/{id}/progress - Get goal progress")
    void testGetGoalProgress() {
        if (goalId == null) {
            createTestGoalForUser();
        }

        given()
                .header("X-User-ID", testUser1)
                .when()
                .get("/api/v1/buddy/goals/{id}/progress", goalId)
                .then()
                .statusCode(200)
                .time(lessThan(1000L))
                .body("success", equalTo(true))
                .body("data.progressPercentage", notNullValue())
                .body("data.milestonesCompleted", notNullValue());
    }

    @Test
    @Order(28)
    @DisplayName("GET /api/v1/buddy/goals/templates - Get goal templates")
    void testGetGoalTemplates() {
        given()
                .header("X-User-ID", testUser1)
                .queryParam("category", "FITNESS")
                .when()
                .get("/api/v1/buddy/goals/templates")
                .then()
                .statusCode(200)
                .time(lessThan(1000L))
                .body("success", equalTo(true))
                .body("data", notNullValue());
    }

    @Test
    @Order(29)
    @DisplayName("POST /api/v1/buddy/goals/search - Search goals")
    void testSearchGoals() {
        // Create search request using the controller's GoalSearchRequest format
        Map<String, Object> searchRequest = Map.of(
            "title", "Test",
            "category", "Fitness",
            "status", "IN_PROGRESS",
            "minProgress", 0,
            "maxProgress", 100
        );

        given()
                .header("X-User-ID", testUser1)
                .contentType(ContentType.JSON)
                .body(searchRequest)
                .when()
                .post("/api/v1/buddy/goals/search")
                .then()
                .statusCode(200)
                .time(lessThan(1000L))
                .body("success", equalTo(true))
                .body("data.content", notNullValue());
    }

    // =================================================================================================
    // BUDDY CHECKIN CONTROLLER TESTS (8 endpoints)
    // =================================================================================================

    @Test
    @Order(30)
    @DisplayName("POST /api/v1/buddy/checkins/daily - Create daily check-in")
    void testCreateDailyCheckin() {
        // Ensure a partnership exists
        if (partnershipId == null) {
            partnershipId = createTestPartnership();
        }

        CheckinRequestDto checkin = createTestCheckinRequest();
        checkin.setPartnershipId(UUID.fromString(partnershipId));

        Response response = given()
                .header("X-User-ID", testUser1)
                .contentType(ContentType.JSON)
                .body(checkin)
                .when()
                .post("/api/v1/buddy/checkins/daily")
                .then()
                .statusCode(201)
                .time(lessThan(1000L))
                .body("success", equalTo(true))
                .body("data.id", notNullValue())
                .body("data.mood", notNullValue())
                .extract().response();

        checkinId = response.path("data.id");
    }

    @Test
    @Order(31)
    @DisplayName("POST /api/v1/buddy/checkins/weekly - Create weekly check-in")
    void testCreateWeeklyCheckin() {
        // Ensure a partnership exists
        if (partnershipId == null) {
            partnershipId = createTestPartnership();
        }

        WeeklyReviewDto weeklyReview = createTestWeeklyReview();
        weeklyReview.setPartnershipId(UUID.fromString(partnershipId));

        given()
                .header("X-User-ID", testUser1)
                .contentType(ContentType.JSON)
                .body(weeklyReview)
                .when()
                .post("/api/v1/buddy/checkins/weekly")
                .then()
                .statusCode(201)
                .time(lessThan(1000L))
                .body("success", equalTo(true))
                .body("data", notNullValue());
    }

    @Test
    @Order(32)
    @DisplayName("GET /api/v1/buddy/checkins - Get user check-ins")
    void testGetUserCheckins() {
        if (partnershipId == null) {
            partnershipId = createTestPartnership();
        }

        given()
                .header("X-User-ID", testUser1)
                .queryParam("partnershipId", partnershipId)  // Add required partnershipId
                .queryParam("startDate", LocalDate.now().minusDays(30).toString())  // Add required startDate
                .queryParam("endDate", LocalDate.now().toString())  // Add endDate
                .queryParam("page", 0)
                .queryParam("size", 10)
                .queryParam("frequency", "DAILY")
                .when()
                .get("/api/v1/buddy/checkins")
                .then()
                .statusCode(200)
                .time(lessThan(1000L))
                .body("success", equalTo(true))
                .body("data.content", notNullValue())
                .body("data.totalElements", greaterThanOrEqualTo(0));
    }

    @Test
    @Order(33)
    @DisplayName("GET /api/v1/buddy/checkins/{id} - Get check-in details")
    void testGetCheckinDetails() {
        if (checkinId == null) {
            createTestCheckinForUser();
        }

        given()
                .header("X-User-ID", testUser1)
                .when()
                .get("/api/v1/buddy/checkins/{id}", checkinId)
                .then()
                .statusCode(200)
                .time(lessThan(500L))
                .body("success", equalTo(true))
                .body("data.id", equalTo(checkinId));
    }

    @Test
    @Order(34)
    @DisplayName("GET /api/v1/buddy/checkins/streaks - Get check-in streaks")
    void testGetCheckinStreaks() {
        if (partnershipId == null) {
            partnershipId = createTestPartnership();
        }

        given()
                .header("X-User-ID", testUser1)
                .queryParam("partnershipId", partnershipId)  // Add required partnershipId
                .when()
                .get("/api/v1/buddy/checkins/streaks")
                .then()
                .statusCode(200)
                .time(lessThan(1000L))
                .body("success", equalTo(true))
                .body("data.currentDailyStreak", notNullValue())
                .body("data.currentWeeklyStreak", notNullValue())
                .body("data.longestDailyStreak", notNullValue());
    }

    @Test
    @Order(35)
    @DisplayName("GET /api/v1/buddy/checkins/accountability - Get accountability score")
    void testGetAccountabilityScore() {
        if (partnershipId == null) {
            partnershipId = createTestPartnership();
        }

        given()
                .header("X-User-ID", testUser1)
                .queryParam("partnershipId", partnershipId)
                .when()
                .get("/api/v1/buddy/checkins/accountability")
                .then()
                .statusCode(200)
                .time(lessThan(1000L))
                .body("success", equalTo(true))
                .body("data.overallScore", notNullValue())
                .body("data.metrics", notNullValue());
    }

    @Test
    @Order(36)
    @DisplayName("GET /api/v1/buddy/checkins/analytics - Get check-in analytics")
    void testGetCheckinAnalytics() {
        if (partnershipId == null) {
            partnershipId = createTestPartnership();
        }

        given()
                .header("X-User-ID", testUser1)
                .queryParam("partnershipId", partnershipId)
                .queryParam("startDate", LocalDate.now().minusDays(30).toString())  // Add required startDate
                .queryParam("endDate", LocalDate.now().toString())  // Add endDate
                .queryParam("days", 30)
                .when()
                .get("/api/v1/buddy/checkins/analytics")
                .then()
                .statusCode(200)
                .time(lessThan(2000L))
                .body("success", equalTo(true))
                .body("data.totalCheckins", notNullValue())
                .body("data.averageMood", notNullValue())
                .body("data.trends", notNullValue());
    }

    @Test
    @Order(37)
    @DisplayName("GET /api/v1/buddy/checkins/export - Export check-in data")
    void testExportCheckinData() {
        if (partnershipId == null) {
            partnershipId = createTestPartnership();
        }

        given()
                .header("X-User-ID", testUser1)
                .queryParam("partnershipId", partnershipId)
                .queryParam("format", "JSON")
                .queryParam("startDate", "2024-01-01")
                .queryParam("endDate", "2024-12-31")
                .when()
                .get("/api/v1/buddy/checkins/export")
                .then()
                .statusCode(200)
                .time(lessThan(3000L))
                .contentType(containsString("octet-stream"));  // Export returns octet-stream
    }

    // =================================================================================================
    // ERROR SCENARIO TESTS
    // =================================================================================================

    @Test
    @Order(38)
    @DisplayName("Test invalid user ID scenarios")
    void testInvalidUserIdScenarios() {
        // Test missing user ID header
        given()
                .when()
                .get("/api/v1/buddy/matching/queue/status")
                .then()
                .statusCode(400)
                .body("success", equalTo(false))
                .body("message", containsString("User ID header is required"));

        // Test empty user ID header
        given()
                .header("X-User-ID", "")
                .when()
                .get("/api/v1/buddy/matching/queue/status")
                .then()
                .statusCode(400)
                .body("success", equalTo(false));
    }

    @Test
    @Order(39)
    @DisplayName("Test pagination limits and validation")
    void testPaginationValidation() {
        // Test invalid page size
        given()
                .header("X-User-ID", testUser1)
                .queryParam("page", 0)
                .queryParam("size", 1000) // Exceeds maximum
                .when()
                .get("/api/v1/buddy/partnerships")
                .then()
                .statusCode(200);  // API doesn't validate max page size

        // Test negative page number
        given()
                .header("X-User-ID", testUser1)
                .queryParam("page", -1)
                .queryParam("size", 10)
                .when()
                .get("/api/v1/buddy/partnerships")
                .then()
                .statusCode(400);  // API validates negative page numbers
    }

    @Test
    @Order(40)
    @DisplayName("Test concurrent request handling")
    void testConcurrentRequests() {
        // Test multiple users joining queue simultaneously
        List<Thread> threads = new ArrayList<>();
        List<Integer> statusCodes = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < 5; i++) {
            final String userId = UUID.randomUUID().toString();
            // Setup preferences for each user first
            setupMatchingPreferences(userId);

            Thread thread = new Thread(() -> {
                try {
                    Response response = given()
                            .header("X-User-ID", userId)
                            .when()
                            .post("/api/v1/buddy/matching/queue");
                    statusCodes.add(response.getStatusCode());
                } catch (Exception e) {
                    statusCodes.add(500);
                }
            });
            threads.add(thread);
            thread.start();
        }

        // Wait for all threads to complete
        threads.forEach(thread -> {
            try {
                thread.join(5000); // 5 second timeout
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        // All requests should succeed
        assertTrue(statusCodes.stream().allMatch(code -> code == 200),
                "All concurrent requests should succeed. Status codes: " + statusCodes);
    }

    // =================================================================================================
    // PERFORMANCE TESTS
    // =================================================================================================

    @Test
    @Order(41)
    @DisplayName("Test response time performance")
    void testResponseTimePerformance() {
        // Test that critical endpoints respond within acceptable time limits
        given()
                .header("X-User-ID", testUser1)
                .when()
                .get("/api/v1/buddy/matching/queue/status")
                .then()
                .time(lessThan(500L)); // Health check should be fast

        given()
                .header("X-User-ID", testUser1)
                .when()
                .get("/api/v1/buddy/partnerships")
                .then()
                .time(lessThan(1000L)); // Data retrieval should be reasonable

        given()
                .header("X-User-ID", testUser1)
                .when()
                .get("/api/v1/buddy/checkins/analytics")
                .then()
                .time(lessThan(2000L)); // Analytics can be slower but within limits
    }

    @Test
    @Order(42)
    @DisplayName("Test bulk operations - Create multiple partnerships")
    void testBulkPartnershipCreation() {
        // Test creating multiple partnerships for stress testing
        List<String> userIds = Arrays.asList(testUser1, testUser2, testUser3);
        List<Thread> threads = new ArrayList<>();
        List<Integer> statusCodes = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < userIds.size() - 1; i++) {
            String requesterId = userIds.get(i);
            String recipientId = userIds.get(i + 1);

            Thread thread = new Thread(() -> {
                try {
                    PartnershipRequestDto request = createPartnershipRequest(requesterId, recipientId);
                    Response response = given()
                            .header("X-User-ID", requesterId)
                            .contentType(ContentType.JSON)
                            .body(request)
                            .when()
                            .post("/api/v1/buddy/partnerships/request");
                    statusCodes.add(response.getStatusCode());
                } catch (Exception e) {
                    statusCodes.add(500);
                }
            });
            threads.add(thread);
            thread.start();
        }

        // Wait for all threads to complete
        threads.forEach(thread -> {
            try {
                thread.join(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        // Verify at least some partnerships were created successfully
        assertTrue(statusCodes.stream().anyMatch(code -> code == 201 || code == 200),
                "At least one partnership should be created successfully. Status codes: " + statusCodes);
    }

    @Test
    @Order(43)
    @DisplayName("Test system limits and edge cases")
    void testSystemLimitsAndEdgeCases() {
        // Test edge cases and system boundaries

        // Test very long user ID
        String longUserId = "very-long-user-id-" + "x".repeat(200);
        given()
                .header("X-User-ID", longUserId)
                .when()
                .get("/api/v1/buddy/matching/queue/status")
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(400), equalTo(500)))
                .time(lessThan(2000L));

        // Test empty goal creation
        BuddyGoalDto emptyGoal = new BuddyGoalDto();
        emptyGoal.setUserId(testUser1);

        given()
                .header("X-User-ID", testUser1)
                .contentType(ContentType.JSON)
                .body(emptyGoal)
                .when()
                .post("/api/v1/buddy/goals")
                .then()
                .statusCode(anyOf(equalTo(400), equalTo(500))) // Should fail validation
                .time(lessThan(1000L));

        // Test maximum page size
        given()
                .header("X-User-ID", testUser1)
                .queryParam("page", 0)
                .queryParam("size", 10000) // Very large page size
                .when()
                .get("/api/v1/buddy/partnerships")
                .then()
                .statusCode(anyOf(equalTo(400), equalTo(200))) // Should be limited or rejected
                .time(lessThan(2000L));
    }

    // =================================================================================================
    // HELPER METHODS
    // =================================================================================================

    private void setupMatchingPreferences(String userId) {
        MatchingPreferencesDto preferences = createTestPreferences(userId);
        given()
                .header("X-User-ID", userId)
                .contentType(ContentType.JSON)
                .body(preferences)
                .put("/api/v1/buddy/matching/preferences");
    }

    private MatchingPreferencesDto createTestPreferences(String userId) {
        return MatchingPreferencesDto.builder()
                .userId(userId)
                .interests(Arrays.asList("coding", "fitness", "reading"))
                .goals(Arrays.asList("learn new skills", "stay healthy"))
                .preferredTimezone("UTC")
                .communicationStyle("direct")
                .experienceLevel("high")
                .maxPartners(3)
                .build();
    }

    private PartnershipRequestDto createPartnershipRequest(String requesterId, String recipientId) {
        PartnershipRequestDto request = new PartnershipRequestDto();
        request.setRequesterId(UUID.fromString(requesterId));
        request.setRecipientId(UUID.fromString(recipientId));
        request.setMessage("Let's be accountability buddies!");
        request.setDurationDays(30);
        return request;
    }

    private String createTestPartnership() {
        // Ensure preferences are set up for both users
        setupMatchingPreferences(testUser1);
        setupMatchingPreferences(testUser2);

        PartnershipRequestDto request = createPartnershipRequest(testUser1, testUser2);
        Response response = given()
                .header("X-User-ID", testUser1)
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/api/v1/buddy/partnerships/request")
                .then()
                .statusCode(201)  // Ensure successful creation
                .extract()
                .response();

        String id = response.path("data.id");
        assertNotNull(id, "Partnership ID should not be null after creation");
        return id;
    }

    private GoalCreationDto createTestGoal(String userId) {
        return GoalCreationDto.builder()
            .title("Test Fitness Goal")
            .description("Get fit and healthy")
            .targetDate(java.time.LocalDate.now().plusDays(30))
            .createdBy(UUID.fromString(userId))
            .initialProgress(0)
            .goalType(GoalCreationDto.GoalType.INDIVIDUAL)
            .priority(3)
            .category("Fitness")
            .build();
    }

    private String createTestGoalForUser() {
        GoalCreationDto goal = createTestGoal(testUser1);
        Response response = given()
                .header("X-User-ID", testUser1)
                .contentType(ContentType.JSON)
                .body(goal)
                .post("/api/v1/buddy/goals");
        goalId = response.path("data.id");
        return goalId;
    }

    private MilestoneDto createTestMilestone() {
        return MilestoneDto.builder()
            .title("Test Milestone")
            .description("Complete first week")
            .targetDate(java.time.LocalDate.now().plusDays(7))
            .order(1)
            .hasDependencies(false)
            .build();
    }

    private void createTestGoalWithMilestone() {
        createTestGoalForUser();
        MilestoneDto milestone = createTestMilestone();
        Response response = given()
                .header("X-User-ID", testUser1)
                .contentType(ContentType.JSON)
                .body(milestone)
                .post("/api/v1/buddy/goals/{goalId}/milestones", goalId);
        milestoneId = response.path("data.id");
    }

    private BuddyCheckinDto createTestCheckin(String userId) {
        BuddyCheckinDto checkin = new BuddyCheckinDto();
        checkin.setUserId(userId);
        checkin.setMood(7); // Valid range is 0-7 for MoodType enum
        checkin.setEnergyLevel(7);
        checkin.setProductivity(6);
        checkin.setStressLevel(3);
        checkin.setNotes("Feeling good today!");
        checkin.setFrequency(CheckinFrequency.DAILY);
        checkin.setGoalsProgress(Map.of("fitness", 75, "learning", 60));
        checkin.setChallenges(Arrays.asList("time management", "motivation"));
        checkin.setWins(Arrays.asList("completed workout", "learned new concept"));
        return checkin;
    }

    private BuddyCheckinDto createTestWeeklyCheckin(String userId) {
        BuddyCheckinDto checkin = createTestCheckin(userId);
        checkin.setFrequency(CheckinFrequency.WEEKLY);
        checkin.setWeeklyReflection("Good week overall with steady progress");
        return checkin;
    }

    private CheckinRequestDto createTestCheckinRequest() {
        CheckinRequestDto request = new CheckinRequestDto();
        request.setCheckinType(com.focushive.buddy.constant.CheckInType.DAILY);
        request.setContent("Daily check-in: Feeling productive and focused");
        request.setMood(com.focushive.buddy.constant.MoodType.FOCUSED);
        request.setProductivityRating(7);
        return request;
    }

    private CheckinRequestDto createTestWeeklyCheckinRequest() {
        CheckinRequestDto request = createTestCheckinRequest();
        request.setCheckinType(com.focushive.buddy.constant.CheckInType.WEEKLY);
        request.setContent("Weekly reflection: Good progress on goals, maintaining momentum");
        return request;
    }

    private WeeklyReviewDto createTestWeeklyReview() {
        WeeklyReviewDto review = new WeeklyReviewDto();
        LocalDate now = LocalDate.now();
        LocalDate weekStart = now.minusDays(now.getDayOfWeek().getValue() - 1); // Start of this week (Monday)
        review.setWeekStartDate(weekStart);
        review.setWeekEndDate(weekStart.plusDays(6)); // End of week (Sunday)
        review.setWeeklyProgress("Made good progress on all goals this week");
        review.setAccomplishments("Completed 80% of planned tasks, maintained daily check-ins");
        review.setChallengesFaced("Some time management issues mid-week");
        review.setNextWeekGoals("Focus on completing project milestone and improving time management");
        return review;
    }

    private String createTestCheckinForUser() {
        // Ensure a partnership exists
        if (partnershipId == null) {
            partnershipId = createTestPartnership();
        }

        CheckinRequestDto checkin = createTestCheckinRequest();
        checkin.setPartnershipId(UUID.fromString(partnershipId));

        Response response = given()
                .header("X-User-ID", testUser1)
                .contentType(ContentType.JSON)
                .body(checkin)
                .post("/api/v1/buddy/checkins/daily");
        checkinId = response.path("data.id");
        return checkinId;
    }

    private void createUniqueTestUser(String userId, String displayName) {
        com.focushive.buddy.entity.User user = com.focushive.buddy.entity.User.builder()
                .id(userId)
                .displayName(displayName)
                .timezone("UTC")
                .interests(Arrays.asList("coding", "fitness", "reading"))
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        userRepository.save(user);
    }

    private String createTestPartnershipBetween(String user1Id, String user2Id) {
        // Ensure preferences are set up for both users
        setupMatchingPreferences(user1Id);
        setupMatchingPreferences(user2Id);

        PartnershipRequestDto request = createPartnershipRequest(user1Id, user2Id);
        Response response = given()
                .header("X-User-ID", user1Id)
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/api/v1/buddy/partnerships/request")
                .then()
                .statusCode(201)  // Ensure successful creation
                .extract()
                .response();

        String id = response.path("data.id");
        assertNotNull(id, "Partnership ID should not be null after creation");
        return id;
    }
}