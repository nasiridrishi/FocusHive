package com.focushive.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Comprehensive authorization security tests for FocusHive platform.
 * Tests Role-Based Access Control (RBAC), resource-level permissions,
 * cross-service authorization, privilege escalation attempts, and direct object references.
 * 
 * Security Areas Covered:
 * - Role-Based Access Control (RBAC) validation
 * - Resource-level permission enforcement
 * - API endpoint authorization checks
 * - Cross-service authorization validation
 * - Privilege escalation attack prevention
 * - Insecure Direct Object Reference (IDOR) testing
 * - Method-level security validation
 * - Context-based authorization (personas, hives, etc.)
 * - Administrative privilege protection
 * - Horizontal and vertical access control
 * 
 * @author FocusHive Security Team
 * @version 2.0
 * @since 2024-12-12
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebMvc
@ActiveProfiles("test")
@DisplayName("Authorization Security Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuthorizationSecurityTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    // Test users with different roles
    private static final String ADMIN_USERNAME = "admin_user";
    private static final String USER_USERNAME = "regular_user";
    private static final String MODERATOR_USERNAME = "moderator_user";
    private static final String GUEST_USERNAME = "guest_user";
    private static final UUID ADMIN_USER_ID = UUID.randomUUID();
    private static final UUID REGULAR_USER_ID = UUID.randomUUID();
    private static final UUID MODERATOR_USER_ID = UUID.randomUUID();
    private static final UUID GUEST_USER_ID = UUID.randomUUID();

    // Test resources
    private static final UUID TEST_HIVE_ID = UUID.randomUUID();
    private static final UUID TEST_PERSONA_ID = UUID.randomUUID();
    private static final UUID OTHER_USER_PERSONA_ID = UUID.randomUUID();
    private static final UUID TEST_FORUM_POST_ID = UUID.randomUUID();

    private String adminToken;
    private String userToken;
    private String moderatorToken;
    private String guestToken;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        // Generate tokens for different user roles
        adminToken = SecurityTestUtils.generateJwtToken(ADMIN_USERNAME, "ADMIN", 
                Instant.now().plus(1, ChronoUnit.HOURS), ADMIN_USER_ID);
        userToken = SecurityTestUtils.generateJwtToken(USER_USERNAME, "USER", 
                Instant.now().plus(1, ChronoUnit.HOURS), REGULAR_USER_ID);
        moderatorToken = SecurityTestUtils.generateJwtToken(MODERATOR_USERNAME, "MODERATOR", 
                Instant.now().plus(1, ChronoUnit.HOURS), MODERATOR_USER_ID);
        guestToken = SecurityTestUtils.generateJwtToken(GUEST_USERNAME, "GUEST", 
                Instant.now().plus(1, ChronoUnit.HOURS), GUEST_USER_ID);
    }

    // ============== Role-Based Access Control Tests ==============

    @Test
    @Order(1)
    @DisplayName("Should enforce admin-only access to admin endpoints")
    void testAdminOnlyEndpointAccess() throws Exception {
        // Admin endpoints that should be restricted
        String[] adminEndpoints = {
            "/api/v1/admin/users",
            "/api/v1/admin/hives",
            "/api/v1/admin/analytics",
            "/api/v1/admin/system/config",
            "/api/v1/admin/security/audit"
        };

        for (String endpoint : adminEndpoints) {
            // Admin should have access
            mockMvc.perform(get(endpoint)
                    .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk());

            // Regular users should be denied
            mockMvc.perform(get(endpoint)
                    .header("Authorization", "Bearer " + userToken))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.error").value("Access Denied"));

            // Moderators should be denied unless specifically granted
            mockMvc.perform(get(endpoint)
                    .header("Authorization", "Bearer " + moderatorToken))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.error").value("Access Denied"));

            // Guests should be denied
            mockMvc.perform(get(endpoint)
                    .header("Authorization", "Bearer " + guestToken))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.error").value("Access Denied"));
        }
    }

    @Test
    @Order(2)
    @DisplayName("Should enforce moderator permissions for moderation endpoints")
    void testModeratorEndpointAccess() throws Exception {
        String[] moderatorEndpoints = {
            "/api/v1/forum/posts/" + TEST_FORUM_POST_ID + "/moderate",
            "/api/v1/hives/" + TEST_HIVE_ID + "/moderate",
            "/api/v1/moderation/reports"
        };

        for (String endpoint : moderatorEndpoints) {
            // Admins should have access
            mockMvc.perform(post(endpoint)
                    .header("Authorization", "Bearer " + adminToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
                    .andExpect(status().isOk());

            // Moderators should have access
            mockMvc.perform(post(endpoint)
                    .header("Authorization", "Bearer " + moderatorToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
                    .andExpect(status().isOk());

            // Regular users should be denied
            mockMvc.perform(post(endpoint)
                    .header("Authorization", "Bearer " + userToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
                    .andExpect(status().isForbidden());

            // Guests should be denied
            mockMvc.perform(post(endpoint)
                    .header("Authorization", "Bearer " + guestToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    @Order(3)
    @DisplayName("Should allow user access to user-level endpoints")
    void testUserEndpointAccess() throws Exception {
        String[] userEndpoints = {
            "/api/v1/users/profile",
            "/api/v1/hives/public",
            "/api/v1/personas"
        };

        for (String endpoint : userEndpoints) {
            // All authenticated users should have access
            mockMvc.perform(get(endpoint)
                    .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk());

            mockMvc.perform(get(endpoint)
                    .header("Authorization", "Bearer " + userToken))
                    .andExpect(status().isOk());

            mockMvc.perform(get(endpoint)
                    .header("Authorization", "Bearer " + moderatorToken))
                    .andExpect(status().isOk());

            // Guests might have limited access
            mockMvc.perform(get(endpoint)
                    .header("Authorization", "Bearer " + guestToken))
                    .andExpect(status().isOk());
        }
    }

    // ============== Resource-Level Authorization Tests ==============

    @Test
    @Order(10)
    @DisplayName("Should enforce owner-only access to personal resources")
    void testOwnerOnlyResourceAccess() throws Exception {
        // Test persona access - users should only access their own personas
        mockMvc.perform(get("/api/v1/personas/" + TEST_PERSONA_ID)
                .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk()); // Assuming this persona belongs to the user

        mockMvc.perform(get("/api/v1/personas/" + OTHER_USER_PERSONA_ID)
                .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Access denied to resource"));

        // Test persona modification
        Map<String, Object> personaUpdate = Map.of(
            "name", "Updated Persona",
            "bio", "Updated bio"
        );

        mockMvc.perform(put("/api/v1/personas/" + TEST_PERSONA_ID)
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(SecurityTestUtils.toJson(personaUpdate)))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/v1/personas/" + OTHER_USER_PERSONA_ID)
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(SecurityTestUtils.toJson(personaUpdate)))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(11)
    @DisplayName("Should enforce hive membership for hive operations")
    void testHiveMembershipAuthorization() throws Exception {
        UUID memberHiveId = UUID.randomUUID();
        UUID nonMemberHiveId = UUID.randomUUID();

        // Member should have access to hive operations
        mockMvc.perform(get("/api/v1/hives/" + memberHiveId + "/presence")
                .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/hives/" + memberHiveId + "/timer/start")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isOk());

        // Non-member should be denied access to private hive operations
        mockMvc.perform(get("/api/v1/hives/" + nonMemberHiveId + "/presence")
                .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Not a member of this hive"));

        mockMvc.perform(post("/api/v1/hives/" + nonMemberHiveId + "/timer/start")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(12)
    @DisplayName("Should enforce admin override for all resources")
    void testAdminResourceOverride() throws Exception {
        // Admin should be able to access any user's resources
        mockMvc.perform(get("/api/v1/personas/" + TEST_PERSONA_ID)
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/personas/" + OTHER_USER_PERSONA_ID)
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        // Admin should be able to access any hive
        mockMvc.perform(get("/api/v1/hives/" + TEST_HIVE_ID + "/presence")
                .header("Authorization", "Bearer " + adminToken))
                .andExpected(status().isOk());

        // Admin should be able to moderate any content
        mockMvc.perform(post("/api/v1/forum/posts/" + TEST_FORUM_POST_ID + "/moderate")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"action\": \"hide\", \"reason\": \"inappropriate content\"}"))
                .andExpect(status().isOk());
    }

    // ============== Insecure Direct Object Reference (IDOR) Tests ==============

    @Test
    @Order(20)
    @DisplayName("Should prevent IDOR attacks on user endpoints")
    void testIDORPrevention() throws Exception {
        UUID targetUserId = UUID.randomUUID();
        
        // Try to access another user's profile directly
        mockMvc.perform(get("/api/v1/users/" + targetUserId)
                .header("Authorization", "Bearer " + userToken))
                .andExpected(status().isForbidden());

        // Try to update another user's profile
        Map<String, Object> profileUpdate = Map.of(
            "firstName", "Hacked",
            "lastName", "User",
            "email", "hacker@evil.com"
        );

        mockMvc.perform(put("/api/v1/users/" + targetUserId)
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(SecurityTestUtils.toJson(profileUpdate)))
                .andExpected(status().isForbidden());

        // Try to delete another user
        mockMvc.perform(delete("/api/v1/users/" + targetUserId)
                .header("Authorization", "Bearer " + userToken))
                .andExpected(status().isForbidden());
    }

    @Test
    @Order(21)
    @DisplayName("Should prevent IDOR attacks with sequential ID enumeration")
    void testSequentialIDORAttacks() throws Exception {
        List<Integer> sequentialIds = Arrays.asList(1, 2, 3, 4, 5, 10, 100, 1000);
        
        for (Integer id : sequentialIds) {
            // Try to access user profiles with sequential IDs
            mockMvc.perform(get("/api/v1/users/" + id)
                    .header("Authorization", "Bearer " + userToken))
                    .andExpected(status().isForbidden());

            // Try to access personas with sequential IDs
            mockMvc.perform(get("/api/v1/personas/" + id)
                    .header("Authorization", "Bearer " + userToken))
                    .andExpected(status().isForbidden());

            // Try to access hives with sequential IDs
            mockMvc.perform(get("/api/v1/hives/" + id)
                    .header("Authorization", "Bearer " + userToken))
                    .andExpected(status().isForbidden());
        }
    }

    // ============== Privilege Escalation Tests ==============

    @Test
    @Order(30)
    @DisplayName("Should prevent horizontal privilege escalation")
    void testHorizontalPrivilegeEscalation() throws Exception {
        // Create two users with same role level
        String user1Token = SecurityTestUtils.generateJwtToken("user1", "USER", 
                Instant.now().plus(1, ChronoUnit.HOURS), UUID.randomUUID());
        String user2Token = SecurityTestUtils.generateJwtToken("user2", "USER", 
                Instant.now().plus(1, ChronoUnit.HOURS), UUID.randomUUID());

        UUID user1PersonaId = UUID.randomUUID();
        UUID user2PersonaId = UUID.randomUUID();

        // User1 should not be able to access User2's resources
        mockMvc.perform(get("/api/v1/personas/" + user2PersonaId)
                .header("Authorization", "Bearer " + user1Token))
                .andExpected(status().isForbidden());

        // User2 should not be able to access User1's resources
        mockMvc.perform(get("/api/v1/personas/" + user1PersonaId)
                .header("Authorization", "Bearer " + user2Token))
                .andExpected(status().isForbidden());

        // Neither should be able to perform admin actions
        mockMvc.perform(get("/api/v1/admin/users")
                .header("Authorization", "Bearer " + user1Token))
                .andExpected(status().isForbidden());

        mockMvc.perform(get("/api/v1/admin/users")
                .header("Authorization", "Bearer " + user2Token))
                .andExpected(status().isForbidden());
    }

    @Test
    @Order(31)
    @DisplayName("Should prevent vertical privilege escalation")
    void testVerticalPrivilegeEscalation() throws Exception {
        // Regular user should not be able to access higher privilege endpoints
        mockMvc.perform(get("/api/v1/admin/users")
                .header("Authorization", "Bearer " + userToken))
                .andExpected(status().isForbidden());

        // Try to escalate through parameter manipulation
        mockMvc.perform(get("/api/v1/users/profile?role=ADMIN")
                .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk()) // Should work but not escalate privileges
                .andExpect(jsonPath("$.role").value("USER")); // Role should remain USER

        // Try to escalate through header manipulation
        mockMvc.perform(get("/api/v1/users/profile")
                .header("Authorization", "Bearer " + userToken)
                .header("X-User-Role", "ADMIN"))
                .andExpected(status().isOk())
                .andExpected(jsonPath("$.role").value("USER"));

        // Try to escalate through body manipulation in update requests
        Map<String, Object> escalationAttempt = Map.of(
            "firstName", "Regular",
            "lastName", "User", 
            "role", "ADMIN" // Try to change role
        );

        mockMvc.perform(put("/api/v1/users/profile")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(SecurityTestUtils.toJson(escalationAttempt)))
                .andExpected(status().isForbidden()) // Should reject role change
                .andExpected(jsonPath("$.error").value("Cannot modify role"));
    }

    @Test
    @Order(32)
    @DisplayName("Should prevent privilege escalation through JWT manipulation")
    void testJWTPrivilegeEscalation() throws Exception {
        // Create a JWT with tampered role claim
        String tamperedToken = SecurityTestUtils.generateJwtToken(USER_USERNAME, "ADMIN", 
                Instant.now().plus(1, ChronoUnit.HOURS), REGULAR_USER_ID);
        
        // But sign it with wrong key or tamper with it
        String invalidTamperedToken = tamperedToken.substring(0, tamperedToken.lastIndexOf('.')) + ".tampered_signature";

        // Should reject tampered token
        mockMvc.perform(get("/api/v1/admin/users")
                .header("Authorization", "Bearer " + invalidTamperedToken))
                .andExpected(status().isUnauthorized())
                .andExpected(jsonPath("$.error").exists());
    }

    // ============== Cross-Service Authorization Tests ==============

    @Test
    @Order(40)
    @DisplayName("Should maintain authorization context across microservices")
    void testCrossServiceAuthorization() throws Exception {
        // Test that authorization is maintained when calling between services
        
        // Identity service to Analytics service
        mockMvc.perform(get("/api/v1/analytics/user-stats")
                .header("Authorization", "Bearer " + userToken))
                .andExpected(status().isOk());

        // Regular user should not access other user's analytics
        mockMvc.perform(get("/api/v1/analytics/user-stats?userId=" + UUID.randomUUID())
                .header("Authorization", "Bearer " + userToken))
                .andExpected(status().isForbidden());

        // Admin should access any user's analytics
        mockMvc.perform(get("/api/v1/analytics/user-stats?userId=" + REGULAR_USER_ID)
                .header("Authorization", "Bearer " + adminToken))
                .andExpected(status().isOk());
    }

    @Test
    @Order(41)
    @DisplayName("Should enforce authorization in service-to-service communication")
    void testServiceToServiceAuthorization() throws Exception {
        // Simulate internal service calls that should validate authorization
        
        // Music service accessing user preferences
        mockMvc.perform(get("/api/v1/music/preferences")
                .header("Authorization", "Bearer " + userToken)
                .header("X-Service-Name", "focushive-backend"))
                .andExpected(status().isOk());

        // Chat service accessing hive membership
        mockMvc.perform(get("/api/v1/chat/hive/" + TEST_HIVE_ID + "/messages")
                .header("Authorization", "Bearer " + userToken))
                .andExpected(status().isOk()); // If user is member

        // Notification service should validate user access
        mockMvc.perform(get("/api/v1/notifications")
                .header("Authorization", "Bearer " + userToken))
                .andExpected(status().isOk());

        mockMvc.perform(get("/api/v1/notifications?userId=" + UUID.randomUUID())
                .header("Authorization", "Bearer " + userToken))
                .andExpected(status().isForbidden());
    }

    // ============== Method-Level Security Tests ==============

    @Test
    @Order(50)
    @DisplayName("Should enforce method-level security annotations")
    void testMethodLevelSecurity() throws Exception {
        // Test @PreAuthorize annotations
        
        // hasRole('ADMIN') methods
        mockMvc.perform(post("/api/v1/admin/system/maintenance")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpected(status().isOk());

        mockMvc.perform(post("/api/v1/admin/system/maintenance")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpected(status().isForbidden());

        // hasAuthority('WRITE_PRIVILEGES') methods
        mockMvc.perform(post("/api/v1/hives")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"Test Hive\", \"description\": \"Test\"}"))
                .andExpected(status().isCreated());

        // isOwner() or hasRole('ADMIN') methods
        mockMvc.perform(delete("/api/v1/personas/" + TEST_PERSONA_ID)
                .header("Authorization", "Bearer " + userToken))
                .andExpected(status().isNoContent()); // If owner

        mockMvc.perform(delete("/api/v1/personas/" + OTHER_USER_PERSONA_ID)
                .header("Authorization", "Bearer " + userToken))
                .andExpected(status().isForbidden()); // If not owner
    }

    // ============== Context-Based Authorization Tests ==============

    @Test
    @Order(60)
    @DisplayName("Should enforce persona-based authorization")
    void testPersonaBasedAuthorization() throws Exception {
        UUID activePersonaId = UUID.randomUUID();
        UUID inactivePersonaId = UUID.randomUUID();
        
        // Actions should be authorized based on active persona
        mockMvc.perform(post("/api/v1/hives/" + TEST_HIVE_ID + "/join")
                .header("Authorization", "Bearer " + userToken)
                .header("X-Active-Persona", activePersonaId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpected(status().isOk());

        // Actions with wrong persona should be denied
        mockMvc.perform(post("/api/v1/hives/" + TEST_HIVE_ID + "/moderate")
                .header("Authorization", "Bearer " + userToken)
                .header("X-Active-Persona", inactivePersonaId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpected(status().isForbidden());
    }

    @Test
    @Order(61)
    @DisplayName("Should enforce time-based authorization")
    void testTimeBasedAuthorization() throws Exception {
        // Some operations might be time-restricted
        
        // Try to access time-sensitive analytics during allowed hours
        mockMvc.perform(get("/api/v1/analytics/real-time")
                .header("Authorization", "Bearer " + userToken))
                .andExpected(status().isOk());

        // Test with expired session token (time-based)
        String expiredToken = SecurityTestUtils.generateJwtToken(USER_USERNAME, "USER",
                Instant.now().minus(1, ChronoUnit.HOURS), REGULAR_USER_ID);

        mockMvc.perform(get("/api/v1/analytics/real-time")
                .header("Authorization", "Bearer " + expiredToken))
                .andExpected(status().isUnauthorized());
    }

    // ============== Concurrent Authorization Tests ==============

    @Test
    @Order(70)
    @DisplayName("Should handle concurrent authorization requests correctly")
    void testConcurrentAuthorizationRequests() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(10);
        List<CompletableFuture<Integer>> futures = new ArrayList<>();

        // Simulate concurrent requests from same user
        for (int i = 0; i < 20; i++) {
            CompletableFuture<Integer> future = CompletableFuture.supplyAsync(() -> {
                try {
                    return mockMvc.perform(get("/api/v1/users/profile")
                            .header("Authorization", "Bearer " + userToken))
                            .andReturn()
                            .getResponse()
                            .getStatus();
                } catch (Exception e) {
                    return 500;
                }
            }, executor);
            futures.add(future);
        }

        // All requests should succeed
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
        allFutures.get();

        for (CompletableFuture<Integer> future : futures) {
            assertEquals(200, future.get());
        }

        executor.shutdown();
    }

    // ============== Authorization Bypass Attempts ==============

    @Test
    @Order(80)
    @DisplayName("Should prevent authorization bypass through parameter pollution")
    void testParameterPollutionBypass() throws Exception {
        // Try to bypass authorization using parameter pollution
        mockMvc.perform(get("/api/v1/users/profile?userId=" + UUID.randomUUID() + "&userId=" + REGULAR_USER_ID)
                .header("Authorization", "Bearer " + userToken))
                .andExpected(status().isOk())
                .andExpected(jsonPath("$.id").value(REGULAR_USER_ID.toString())); // Should use authenticated user's ID
    }

    @Test
    @Order(81)
    @DisplayName("Should prevent authorization bypass through HTTP method override")
    void testHTTPMethodOverrideBypass() throws Exception {
        // Try to bypass authorization using method override headers
        mockMvc.perform(get("/api/v1/admin/users")
                .header("Authorization", "Bearer " + userToken)
                .header("X-HTTP-Method-Override", "GET")
                .header("X-HTTP-Method", "GET"))
                .andExpected(status().isForbidden());

        mockMvc.perform(post("/api/v1/users/profile")
                .header("Authorization", "Bearer " + userToken)
                .header("X-HTTP-Method-Override", "PUT")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpected(status().isNotFound()); // Should not find POST endpoint
    }

    @Test
    @Order(82)
    @DisplayName("Should prevent authorization bypass through case manipulation")
    void testCaseManipulationBypass() throws Exception {
        // Try to bypass authorization using different cases in role names
        String[] caseVariations = {"admin", "ADMIN", "Admin", "aDmIn"};
        
        for (String roleVariation : caseVariations) {
            String manipulatedToken = SecurityTestUtils.generateJwtToken(USER_USERNAME, roleVariation,
                    Instant.now().plus(1, ChronoUnit.HOURS), REGULAR_USER_ID);
            
            mockMvc.perform(get("/api/v1/admin/users")
                    .header("Authorization", "Bearer " + manipulatedToken))
                    .andExpected(status().isForbidden()); // Should normalize and deny
        }
    }

    // ============== Resource Cleanup and Security State Tests ==============

    @Test
    @Order(90)
    @DisplayName("Should properly cleanup authorization state")
    void testAuthorizationStateCleanup() throws Exception {
        // Test that authorization state doesn't leak between requests
        
        // Make request with admin token
        mockMvc.perform(get("/api/v1/admin/users")
                .header("Authorization", "Bearer " + adminToken))
                .andExpected(status().isOk());

        // Make request with user token - should not inherit admin privileges
        mockMvc.perform(get("/api/v1/admin/users")
                .header("Authorization", "Bearer " + userToken))
                .andExpected(status().isForbidden());

        // Make request without token - should not inherit any privileges
        mockMvc.perform(get("/api/v1/admin/users"))
                .andExpected(status().isUnauthorized());
    }
}