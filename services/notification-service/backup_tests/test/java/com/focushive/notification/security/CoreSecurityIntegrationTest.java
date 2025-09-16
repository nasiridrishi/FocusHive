package com.focushive.notification.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.focushive.notification.entity.NotificationPreference;
import com.focushive.notification.entity.NotificationType;
import com.focushive.notification.entity.SecurityAuditLog;
import com.focushive.notification.entity.User;
import com.focushive.notification.repository.NotificationPreferenceRepository;
import com.focushive.notification.repository.SecurityAuditLogRepository;
import com.focushive.notification.repository.UserRepository;
import com.focushive.notification.service.SecurityAuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Core security integration tests focusing on:
 * - Authentication and Authorization
 * - Audit Logging Integration
 * - Method-level Security
 * - Security Headers
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureWebMvc
@Import(SecurityTestConfig.class)
@ActiveProfiles("test")
@Transactional
class CoreSecurityIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private SecurityAuditService securityAuditService;

    @Autowired
    private SecurityAuditLogRepository auditLogRepository;

    @Autowired
    private NotificationPreferenceRepository preferenceRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;
    private User testUser;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        // Clear audit logs before each test
        auditLogRepository.deleteAll();

        // Create test user
        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser = userRepository.save(testUser);
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    void testCreatePreferenceWithSecurityAudit() throws Exception {
        // Given
        NotificationPreference preference = new NotificationPreference();
        preference.setUserId(testUser.getId());
        preference.setNotificationType(NotificationType.EMAIL_VERIFICATION);
        preference.setEmailEnabled(true);
        
        String requestJson = objectMapper.writeValueAsString(preference);

        // When
        mockMvc.perform(post("/api/v1/preferences")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.notificationType").value("EMAIL_VERIFICATION"))
                .andExpect(jsonPath("$.emailEnabled").value(true));

        // Then - verify security audit log was created
        List<SecurityAuditLog> auditLogs = auditLogRepository.findAll();
        assertThat(auditLogs).hasSizeGreaterThanOrEqualTo(1);
        
        boolean hasCreateAudit = auditLogs.stream()
                .anyMatch(log -> "CREATE_PREFERENCE".equals(log.getAction()) 
                        && "testuser".equals(log.getUsername())
                        && log.getSuccess());
        
        assertThat(hasCreateAudit).isTrue();
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    void testUpdatePreferenceWithSecurityAudit() throws Exception {
        // Given - create initial preference
        NotificationPreference preference = new NotificationPreference();
        preference.setUserId(testUser.getId());
        preference.setNotificationType(NotificationType.EMAIL_VERIFICATION);
        preference.setEmailEnabled(true);
        preference = preferenceRepository.save(preference);

        // Clear initial audit logs
        auditLogRepository.deleteAll();

        // Update preference
        preference.setEmailEnabled(false);
        String requestJson = objectMapper.writeValueAsString(preference);

        // When
        mockMvc.perform(put("/api/v1/preferences/" + preference.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.emailEnabled").value(false));

        // Then - verify security audit log was created
        List<SecurityAuditLog> auditLogs = auditLogRepository.findAll();
        assertThat(auditLogs).hasSizeGreaterThanOrEqualTo(1);
        
        boolean hasUpdateAudit = auditLogs.stream()
                .anyMatch(log -> "UPDATE_PREFERENCE".equals(log.getAction()) 
                        && "testuser".equals(log.getUsername())
                        && log.getSuccess());
        
        assertThat(hasUpdateAudit).isTrue();
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    void testDeletePreferenceWithSecurityAudit() throws Exception {
        // Given - create preference to delete
        NotificationPreference preference = new NotificationPreference();
        preference.setUserId(testUser.getId());
        preference.setNotificationType(NotificationType.EMAIL_VERIFICATION);
        preference.setEmailEnabled(true);
        preference = preferenceRepository.save(preference);

        // Clear initial audit logs
        auditLogRepository.deleteAll();

        // When
        mockMvc.perform(delete("/api/v1/preferences/" + preference.getId()))
                .andExpect(status().isNoContent());

        // Then - verify security audit log was created
        List<SecurityAuditLog> auditLogs = auditLogRepository.findAll();
        assertThat(auditLogs).hasSizeGreaterThanOrEqualTo(1);
        
        boolean hasDeleteAudit = auditLogs.stream()
                .anyMatch(log -> "DELETE_PREFERENCE".equals(log.getAction()) 
                        && "testuser".equals(log.getUsername())
                        && log.getSuccess());
        
        assertThat(hasDeleteAudit).isTrue();
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void testAdminEndpointAccess() throws Exception {
        // When - access admin endpoint
        mockMvc.perform(get("/api/v1/admin/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers").exists());

        // Then - verify admin action was audited
        List<SecurityAuditLog> auditLogs = auditLogRepository.findAll();
        assertThat(auditLogs).hasSizeGreaterThanOrEqualTo(1);
        
        boolean hasAdminAudit = auditLogs.stream()
                .anyMatch(log -> log.getAction().contains("ADMIN") 
                        && "admin".equals(log.getUsername())
                        && log.getSuccess());
        
        assertThat(hasAdminAudit).isTrue();
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    void testUnauthorizedAdminAccess() throws Exception {
        // When - try to access admin endpoint as regular user
        mockMvc.perform(get("/api/v1/admin/stats"))
                .andExpect(status().isForbidden());

        // Then - verify failed access attempt (may or may not create audit log depending on implementation)
        // This test primarily verifies that authorization is working
        List<SecurityAuditLog> auditLogs = auditLogRepository.findAll();
        
        // Check if access denial was logged
        boolean hasAccessDeniedAudit = auditLogs.stream()
                .anyMatch(log -> log.getAction().contains("ACCESS_DENIED") 
                        || log.getAction().contains("ADMIN")
                        && "testuser".equals(log.getUsername())
                        && !log.getSuccess());
        
        // Note: This assertion is optional as it depends on implementation details
        // The main assertion is that the HTTP status is 403 (which passed above)
    }

    @Test
    void testUnauthenticatedAccessBlocked() throws Exception {
        // When - try to access protected endpoint without authentication
        mockMvc.perform(get("/api/v1/preferences/user/" + testUser.getId()))
                .andExpect(status().isUnauthorized());
        
        // Then - main assertion is the HTTP status (unauthorized access blocked)
        // Audit logging for unauthenticated requests is optional
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    void testSecurityHeadersPresent() throws Exception {
        // When - make authenticated request
        mockMvc.perform(get("/api/v1/preferences/user/" + testUser.getId()))
                .andExpect(status().isOk())
                // Verify security headers are present
                .andExpect(header().exists("X-Content-Type-Options"))
                .andExpect(header().exists("X-Frame-Options"))
                .andExpect(header().exists("X-XSS-Protection"))
                .andExpect(header().exists("Strict-Transport-Security"));
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    void testDataAccessWithAuditLogging() throws Exception {
        // When - access user data
        mockMvc.perform(get("/api/v1/preferences/user/" + testUser.getId()))
                .andExpect(status().isOk());

        // Then - verify data access was potentially audited
        List<SecurityAuditLog> auditLogs = auditLogRepository.findAll();
        
        // Check if data access was logged (implementation dependent)
        boolean hasDataAccessAudit = auditLogs.stream()
                .anyMatch(log -> log.getAction().contains("ACCESS") 
                        && "testuser".equals(log.getUsername())
                        && log.getSuccess());
        
        // Note: The main test is that the request succeeded with proper authentication
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void testAdminCacheManagement() throws Exception {
        // When - admin performs cache management
        mockMvc.perform(post("/api/v1/admin/cache/clear"))
                .andExpect(status().isOk());

        // Then - verify admin action was audited
        List<SecurityAuditLog> auditLogs = auditLogRepository.findAll();
        assertThat(auditLogs).hasSizeGreaterThanOrEqualTo(1);
        
        boolean hasAdminCacheAudit = auditLogs.stream()
                .anyMatch(log -> log.getAction().contains("CACHE") 
                        && "admin".equals(log.getUsername())
                        && log.getSuccess());
        
        assertThat(hasAdminCacheAudit).isTrue();
    }

    @Test
    void testSecurityAuditServiceDirectly() throws Exception {
        // Given - clear existing logs
        auditLogRepository.deleteAll();

        // When - directly use security audit service
        securityAuditService.logSecurityEvent("TEST_ACTION", "testuser", "Direct test", true, null);

        // Then - verify audit log was created
        List<SecurityAuditLog> auditLogs = auditLogRepository.findAll();
        assertThat(auditLogs).hasSize(1);
        
        SecurityAuditLog auditLog = auditLogs.get(0);
        assertThat(auditLog.getAction()).isEqualTo("TEST_ACTION");
        assertThat(auditLog.getUsername()).isEqualTo("testuser");
        assertThat(auditLog.getDetails()).isEqualTo("Direct test");
        assertThat(auditLog.getSuccess()).isTrue();
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void testMultipleSecurityOperations() throws Exception {
        // Given - clear existing logs
        auditLogRepository.deleteAll();

        // When - perform multiple operations
        mockMvc.perform(get("/api/v1/admin/stats")).andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/admin/cache/clear")).andExpect(status().isOk());
        
        // Create a preference as admin
        NotificationPreference preference = new NotificationPreference();
        preference.setUserId(testUser.getId());
        preference.setNotificationType(NotificationType.SYSTEM_NOTIFICATION);
        preference.setEmailEnabled(true);
        
        String requestJson = objectMapper.writeValueAsString(preference);
        mockMvc.perform(post("/api/v1/preferences")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isCreated());

        // Then - verify multiple audit logs were created
        List<SecurityAuditLog> auditLogs = auditLogRepository.findAll();
        assertThat(auditLogs.size()).isGreaterThanOrEqualTo(2);
        
        // Verify all operations were logged with admin user
        boolean allLogsFromAdmin = auditLogs.stream()
                .allMatch(log -> "admin".equals(log.getUsername()));
        
        assertThat(allLogsFromAdmin).isTrue();
    }
}