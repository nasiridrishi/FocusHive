package com.focushive.api.service;

import com.focushive.backend.security.IdentityServicePrincipal;
import com.focushive.hive.entity.Hive;
import com.focushive.hive.entity.HiveMember;
import com.focushive.hive.repository.HiveMemberRepository;
import com.focushive.hive.repository.HiveRepository;
import com.focushive.timer.entity.FocusSession;
import com.focushive.timer.repository.FocusSessionRepository;
import com.focushive.user.entity.User;
import com.focushive.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Comprehensive Test-Driven Development (TDD) tests for Authorization Rules.
 * These tests define the expected authorization behavior and should FAIL initially.
 *
 * Phase 2, Task 2.4: Authorization Rules Implementation
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Authorization Rules - TDD Tests")
class AuthorizationRulesTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private HiveRepository hiveRepository;

    @Mock
    private HiveMemberRepository hiveMemberRepository;

    @Mock
    private FocusSessionRepository focusSessionRepository;

    @Mock
    private SecurityContext securityContext;

    @InjectMocks
    private SecurityService securityService;

    // Test data
    private User testUser;
    private User otherUser;
    private User adminUser;
    private User moderatorUser;
    private Hive publicHive;
    private Hive privateHive;
    private HiveMember ownerMember;
    private HiveMember moderatorMember;
    private HiveMember regularMember;
    private FocusSession focusSession;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.setContext(securityContext);

        // Set up test users
        testUser = new User();
        testUser.setId(UUID.randomUUID().toString());
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");

        otherUser = new User();
        otherUser.setId(UUID.randomUUID().toString());
        otherUser.setUsername("otheruser");
        otherUser.setEmail("other@example.com");

        adminUser = new User();
        adminUser.setId(UUID.randomUUID().toString());
        adminUser.setUsername("admin");
        adminUser.setEmail("admin@example.com");

        moderatorUser = new User();
        moderatorUser.setId(UUID.randomUUID().toString());
        moderatorUser.setUsername("moderator");
        moderatorUser.setEmail("moderator@example.com");

        // Set up test hives
        publicHive = new Hive();
        publicHive.setId(UUID.randomUUID().toString());
        publicHive.setName("Public Hive");
        publicHive.setIsPublic(true);
        publicHive.setOwner(testUser);

        privateHive = new Hive();
        privateHive.setId(UUID.randomUUID().toString());
        privateHive.setName("Private Hive");
        privateHive.setIsPublic(false);
        privateHive.setOwner(testUser);

        // Set up hive members
        ownerMember = new HiveMember();
        ownerMember.setHive(publicHive);
        ownerMember.setUser(testUser);
        ownerMember.setRole(HiveMember.MemberRole.OWNER);

        moderatorMember = new HiveMember();
        moderatorMember.setHive(publicHive);
        moderatorMember.setUser(moderatorUser);
        moderatorMember.setRole(HiveMember.MemberRole.MODERATOR);

        regularMember = new HiveMember();
        regularMember.setHive(publicHive);
        regularMember.setUser(otherUser);
        regularMember.setRole(HiveMember.MemberRole.MEMBER);

        // Set up focus session
        focusSession = new FocusSession();
        focusSession.setId(UUID.randomUUID().toString());
        focusSession.setUserId(testUser.getId());
        focusSession.setHiveId(publicHive.getId());
    }

    @Nested
    @DisplayName("Resource Ownership Tests")
    class ResourceOwnershipTests {

        @Test
        @DisplayName("Should allow hive owner to delete their hive")
        void shouldAllowHiveOwnerToDelete() {
            // Given: User is authenticated as hive owner
            setupAuthenticatedUser(testUser);
            when(hiveRepository.findById(publicHive.getId())).thenReturn(Optional.of(publicHive));

            // When: Checking if user can delete hive
            boolean canDelete = securityService.isHiveOwner(publicHive.getId());

            // Then: Permission should be granted
            assertThat(canDelete).isTrue();
        }

        @Test
        @DisplayName("Should deny non-member access to private hive")
        void shouldDenyNonMemberAccess() {
            // Given: User is not a member of private hive
            setupAuthenticatedUser(otherUser);
            when(hiveRepository.findById(privateHive.getId())).thenReturn(Optional.of(privateHive));
            when(hiveMemberRepository.existsByHiveIdAndUserId(privateHive.getId(), otherUser.getId()))
                .thenReturn(false);

            // When: Checking access to private hive
            boolean hasAccess = securityService.hasAccessToHive(privateHive.getId());

            // Then: Access should be denied
            assertThat(hasAccess).isFalse();
        }

        @Test
        @DisplayName("Should allow public hive access to non-members")
        void shouldAllowPublicHiveAccess() {
            // Given: User is not a member but hive is public
            setupAuthenticatedUser(otherUser);
            when(hiveRepository.findById(publicHive.getId())).thenReturn(Optional.of(publicHive));
            when(hiveMemberRepository.existsByHiveIdAndUserId(publicHive.getId(), otherUser.getId()))
                .thenReturn(false);

            // When: Checking access to public hive
            boolean hasAccess = securityService.hasAccessToHive(publicHive.getId());

            // Then: Access should be granted for reading
            assertThat(hasAccess).isTrue();
        }

        @Test
        @DisplayName("Should enforce role-based access control")
        void shouldEnforceRoleBasedAccess() {
            // Given: User has MEMBER role (not MODERATOR or OWNER)
            setupAuthenticatedUser(otherUser);
            when(hiveRepository.findById(publicHive.getId())).thenReturn(Optional.of(publicHive));
            when(hiveMemberRepository.findByHiveIdAndUserId(publicHive.getId(), otherUser.getId()))
                .thenReturn(Optional.of(regularMember));

            // When: Checking if member can moderate hive
            boolean canModerate = securityService.canModerateHive(publicHive.getId());

            // Then: Moderation should be denied for regular members
            assertThat(canModerate).isFalse();
        }

        @Test
        @DisplayName("Should audit authorization failures")
        void shouldAuditAuthorizationFailures() {
            // Given: User attempts unauthorized access
            setupAuthenticatedUser(otherUser);
            when(hiveRepository.findById(privateHive.getId())).thenReturn(Optional.of(privateHive));
            when(hiveMemberRepository.existsByHiveIdAndUserId(privateHive.getId(), otherUser.getId()))
                .thenReturn(false);

            // When: Access is denied and logged
            boolean hasAccess = securityService.hasAccessToHive(privateHive.getId());
            securityService.logAuthorizationAttempt("READ", privateHive.getId(), hasAccess);

            // Then: Access should be denied and audit log should be called
            assertThat(hasAccess).isFalse();
            // Note: Audit logging is currently just log.warn() - this test documents expected behavior
        }
    }

    @Nested
    @DisplayName("Permission-Based Access Control Tests")
    class PermissionBasedAccessTests {

        @Test
        @DisplayName("Should check hive:create permission")
        void shouldCheckHiveCreatePermission() {
            // Given: User with hive creation permissions
            setupAuthenticatedUser(testUser);

            // When: Checking hive creation permission
            boolean canCreateHives = securityService.hasPermission("hive:create");

            // Then: Permission should be evaluated correctly
            // This test will FAIL initially as hasPermission() doesn't exist yet
            assertThat(canCreateHives).isTrue();
        }

        @Test
        @DisplayName("Should check hive:update permission with ownership")
        void shouldCheckHiveUpdatePermission() {
            // Given: User owns the hive
            setupAuthenticatedUser(testUser);
            when(hiveRepository.findById(publicHive.getId())).thenReturn(Optional.of(publicHive));

            // When: Checking hive update permission
            boolean canUpdate = securityService.hasPermissionOnResource("hive:update", publicHive.getId());

            // Then: Permission should be granted for owner
            // This test will FAIL initially as hasPermissionOnResource() doesn't exist yet
            assertThat(canUpdate).isTrue();
        }

        @Test
        @DisplayName("Should check member:invite permission")
        void shouldCheckMemberInvitePermission() {
            // Given: User is moderator of hive
            setupAuthenticatedUser(moderatorUser);
            when(hiveRepository.findById(publicHive.getId())).thenReturn(Optional.of(publicHive));
            when(hiveMemberRepository.findByHiveIdAndUserId(publicHive.getId(), moderatorUser.getId()))
                .thenReturn(Optional.of(moderatorMember));

            // When: Checking member invite permission
            boolean canInvite = securityService.hasPermissionOnResource("member:invite", publicHive.getId());

            // Then: Permission should be granted for moderator
            // This test will FAIL initially as hasPermissionOnResource() doesn't exist yet
            assertThat(canInvite).isTrue();
        }

        @Test
        @DisplayName("Should deny member:remove for regular members")
        void shouldDenyMemberRemoveForRegularMembers() {
            // Given: User is regular member
            setupAuthenticatedUser(otherUser);
            when(hiveRepository.findById(publicHive.getId())).thenReturn(Optional.of(publicHive));
            when(hiveMemberRepository.findByHiveIdAndUserId(publicHive.getId(), otherUser.getId()))
                .thenReturn(Optional.of(regularMember));

            // When: Checking member remove permission
            boolean canRemove = securityService.hasPermissionOnResource("member:remove", publicHive.getId());

            // Then: Permission should be denied
            // This test will FAIL initially as hasPermissionOnResource() doesn't exist yet
            assertThat(canRemove).isFalse();
        }
    }

    @Nested
    @DisplayName("Role Hierarchy Tests")
    class RoleHierarchyTests {

        @Test
        @DisplayName("Should enforce admin role has all permissions")
        void shouldEnforceAdminRolePermissions() {
            // Given: User has ADMIN role
            setupAuthenticatedUserWithRole(adminUser, "ADMIN");

            // When: Checking various permissions
            boolean canManageUsers = securityService.hasSystemPermission("system:manage-users");
            boolean canManageHives = securityService.hasSystemPermission("system:manage-hives");
            boolean canAccessAnyHive = securityService.hasSystemPermission("system:access-any-hive");

            // Then: All permissions should be granted for admin
            // These tests will FAIL initially as hasSystemPermission() doesn't exist yet
            assertThat(canManageUsers).isTrue();
            assertThat(canManageHives).isTrue();
            assertThat(canAccessAnyHive).isTrue();
        }

        @Test
        @DisplayName("Should enforce moderator role permissions")
        void shouldEnforceModeratorrolePermissions() {
            // Given: User has MODERATOR role in hive
            setupAuthenticatedUser(moderatorUser);
            when(hiveMemberRepository.findByHiveIdAndUserId(publicHive.getId(), moderatorUser.getId()))
                .thenReturn(Optional.of(moderatorMember));

            // When: Checking moderator-specific permissions
            boolean canModerateHive = securityService.canModerateHive(publicHive.getId());
            boolean canInviteMembers = securityService.hasPermissionOnResource("member:invite", publicHive.getId());
            boolean canRemoveMembers = securityService.hasPermissionOnResource("member:remove", publicHive.getId());

            // Then: Moderator permissions should be granted
            assertThat(canModerateHive).isTrue();
            // These will FAIL initially as hasPermissionOnResource() doesn't exist yet
            assertThat(canInviteMembers).isTrue();
            assertThat(canRemoveMembers).isTrue();
        }

        @Test
        @DisplayName("Should enforce user role limitations")
        void shouldEnforceUserRoleLimitations() {
            // Given: User has basic USER role
            setupAuthenticatedUserWithRole(testUser, "USER");

            // When: Checking system-level permissions
            boolean canManageUsers = securityService.hasSystemPermission("system:manage-users");
            boolean canAccessAdminPanel = securityService.hasSystemPermission("system:admin-panel");

            // Then: System permissions should be denied
            // These tests will FAIL initially as hasSystemPermission() doesn't exist yet
            assertThat(canManageUsers).isFalse();
            assertThat(canAccessAdminPanel).isFalse();
        }
    }

    @Nested
    @DisplayName("Dynamic Permission Evaluation Tests")
    class DynamicPermissionTests {

        @Test
        @DisplayName("Should evaluate dynamic hive membership permissions")
        void shouldEvaluateDynamicHiveMembershipPermissions() {
            // Given: User becomes member of hive dynamically
            setupAuthenticatedUser(otherUser);
            when(hiveMemberRepository.existsByHiveIdAndUserId(publicHive.getId(), otherUser.getId()))
                .thenReturn(true);
            when(hiveRepository.findById(publicHive.getId())).thenReturn(Optional.of(publicHive));

            // When: Checking dynamic membership permission
            boolean hasMembershipAccess = securityService.evaluatePermission(
                "hasRole('USER') and @securityService.isHiveMember('" + publicHive.getId() + "')"
            );

            // Then: Dynamic permission should be evaluated correctly
            // This test will FAIL initially as evaluatePermission() doesn't exist yet
            assertThat(hasMembershipAccess).isTrue();
        }

        @Test
        @DisplayName("Should evaluate complex ownership and membership expressions")
        void shouldEvaluateComplexExpressions() {
            // Given: Complex permission scenario
            setupAuthenticatedUser(testUser);
            when(hiveRepository.findById(publicHive.getId())).thenReturn(Optional.of(publicHive));

            // When: Evaluating complex expression
            String expression = "isAuthenticated() and (@securityService.isHiveOwner('" +
                publicHive.getId() + "') or @securityService.canModerateHive('" +
                publicHive.getId() + "'))";
            boolean hasComplexPermission = securityService.evaluatePermission(expression);

            // Then: Complex expression should be evaluated correctly
            // This test will FAIL initially as evaluatePermission() doesn't exist yet
            assertThat(hasComplexPermission).isTrue();
        }
    }

    @Nested
    @DisplayName("Security Audit Tests")
    class SecurityAuditTests {

        @Test
        @DisplayName("Should record all authorization attempts")
        void shouldRecordAllAuthorizationAttempts() {
            // Given: Authorization attempt
            setupAuthenticatedUser(testUser);
            when(hiveRepository.findById(publicHive.getId())).thenReturn(Optional.of(publicHive));

            // When: Permission is checked
            boolean hasAccess = securityService.isHiveOwner(publicHive.getId());

            // Then: Audit record should be created
            // This test documents expected behavior for audit logging
            SecurityAuditEvent expectedEvent = new SecurityAuditEvent(
                testUser.getId(),
                "hive:owner-check",
                publicHive.getId(),
                hasAccess,
                System.currentTimeMillis()
            );

            // This will FAIL initially as SecurityAuditEvent doesn't exist yet
            SecurityAuditEvent actualEvent = securityService.getLastAuditEvent();
            assertThat(actualEvent).isNotNull();
            assertThat(actualEvent.getUserId()).isEqualTo(expectedEvent.getUserId());
            assertThat(actualEvent.getOperation()).isEqualTo(expectedEvent.getOperation());
        }

        @Test
        @DisplayName("Should track permission changes over time")
        void shouldTrackPermissionChanges() {
            // Given: User permission changes
            setupAuthenticatedUser(otherUser);

            // When: User gains and loses permissions
            securityService.recordPermissionChange(
                otherUser.getId(),
                "hive:member",
                publicHive.getId(),
                PermissionChangeType.GRANTED
            );

            securityService.recordPermissionChange(
                otherUser.getId(),
                "hive:member",
                publicHive.getId(),
                PermissionChangeType.REVOKED
            );

            // Then: Permission change history should be tracked
            // This test will FAIL initially as permission change tracking doesn't exist yet
            List<PermissionChangeEvent> changes = securityService.getPermissionChangeHistory(
                otherUser.getId(), publicHive.getId()
            );

            assertThat(changes).hasSize(2);
            assertThat(changes.get(0).getChangeType()).isEqualTo(PermissionChangeType.GRANTED);
            assertThat(changes.get(1).getChangeType()).isEqualTo(PermissionChangeType.REVOKED);
        }
    }

    @Nested
    @DisplayName("Performance Tests")
    class PerformanceTests {

        @Test
        @DisplayName("Should complete authorization check within 5ms")
        void shouldCompleteAuthorizationCheckWithinTimeLimit() {
            // Given: Performance requirement < 5ms
            setupAuthenticatedUser(testUser);
            when(hiveRepository.findById(publicHive.getId())).thenReturn(Optional.of(publicHive));

            // When: Performing authorization check with timing
            long startTime = System.currentTimeMillis();
            boolean hasAccess = securityService.hasAccessToHive(publicHive.getId());
            long endTime = System.currentTimeMillis();

            // Then: Authorization should complete within performance budget
            long executionTime = endTime - startTime;
            assertThat(hasAccess).isTrue();
            assertThat(executionTime).isLessThan(5L); // 5ms limit
        }
    }

    // Helper methods for test setup
    private void setupAuthenticatedUser(User user) {
        IdentityServicePrincipal principal = new IdentityServicePrincipal(
            user.getId(), user.getEmail(), null
        );

        Authentication authentication = new UsernamePasswordAuthenticationToken(
            principal, null, List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(userRepository.findByUsername(user.getUsername())).thenReturn(Optional.of(user));
    }

    private void setupAuthenticatedUserWithRole(User user, String role) {
        IdentityServicePrincipal principal = new IdentityServicePrincipal(
            user.getId(), user.getEmail(), null
        );

        Authentication authentication = new UsernamePasswordAuthenticationToken(
            principal, null, List.of(new SimpleGrantedAuthority("ROLE_" + role))
        );

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
    }

    // Placeholder classes that will need to be implemented
    public static class SecurityAuditEvent {
        private final String userId;
        private final String operation;
        private final String resourceId;
        private final boolean granted;
        private final long timestamp;

        public SecurityAuditEvent(String userId, String operation, String resourceId, boolean granted, long timestamp) {
            this.userId = userId;
            this.operation = operation;
            this.resourceId = resourceId;
            this.granted = granted;
            this.timestamp = timestamp;
        }

        public String getUserId() { return userId; }
        public String getOperation() { return operation; }
        public String getResourceId() { return resourceId; }
        public boolean isGranted() { return granted; }
        public long getTimestamp() { return timestamp; }
    }

    public static class PermissionChangeEvent {
        private final String userId;
        private final String permission;
        private final String resourceId;
        private final PermissionChangeType changeType;
        private final long timestamp;

        public PermissionChangeEvent(String userId, String permission, String resourceId,
                                   PermissionChangeType changeType, long timestamp) {
            this.userId = userId;
            this.permission = permission;
            this.resourceId = resourceId;
            this.changeType = changeType;
            this.timestamp = timestamp;
        }

        public PermissionChangeType getChangeType() { return changeType; }
    }

    public enum PermissionChangeType {
        GRANTED, REVOKED
    }
}