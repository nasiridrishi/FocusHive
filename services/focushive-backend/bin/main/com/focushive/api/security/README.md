# FocusHive Authorization System

## Overview

This document describes the comprehensive authorization system implemented for FocusHive Backend Service as part of **Phase 2, Task 2.4: Authorization Rules**. The implementation follows **Test-Driven Development (TDD)** principles and provides enterprise-grade security features.

## Features Implemented

### ✅ 1. Resource Ownership Verification
- **Hive Ownership**: Users can fully control hives they own (create, read, update, delete)
- **Member Access**: Hive members can participate in hive activities (read, join timers, chat)
- **Public/Private Access**: Public hives allow read access to non-members; private hives restrict access to members only

### ✅ 2. Role-Based Access Control (RBAC)
- **OWNER**: Full control over owned hives and resources
- **MODERATOR**: Can moderate hives they're assigned to (invite/remove members, moderate content)
- **MEMBER**: Basic participation rights in hives they've joined
- **ADMIN**: System-wide administrative privileges
- **USER**: Basic authenticated user permissions

### ✅ 3. Permission Hierarchy
Implemented granular permission system:

#### Hive Permissions
- `hive:create` - Create new hives
- `hive:read` - View hive details
- `hive:update` - Modify hive settings
- `hive:delete` - Delete hive (owner only)
- `hive:join` - Join public hives
- `hive:leave` - Leave hives

#### Member Management Permissions
- `member:invite` - Invite new members (moderator+)
- `member:remove` - Remove members (moderator+)
- `member:promote` - Change member roles (owner only)

#### System Permissions
- `system:manage-users` - User administration (admin only)
- `system:manage-hives` - Global hive management (admin only)
- `system:access-any-hive` - Access any hive regardless of membership (admin only)
- `system:admin-panel` - Access administrative interface (admin only)

### ✅ 4. Method-Level Security Annotations
Created custom annotations for clean, expressive authorization:

```java
// Custom annotations
@IsHiveOwner("hiveId")              // Check hive ownership
@IsHiveMember("hiveId")             // Check hive membership
@CanModerateHive("hiveId")          // Check moderation rights
@HasPermission("hive:create")       // Check specific permission
@RequiresRole("ADMIN")              // Check user role

// Usage examples
@DeleteMapping("/{id}")
@IsHiveOwner("id")
public ResponseEntity<Void> deleteHive(@PathVariable String id) { ... }

@PostMapping("/{id}/members/{memberId}/promote")
@HasPermission(value = "member:promote", resourceId = "#id")
public ResponseEntity<Void> promoteMember(@PathVariable String id, @PathVariable String memberId) { ... }
```

### ✅ 5. Dynamic Permission Evaluation
Implemented SpEL (Spring Expression Language) support for complex authorization logic:

```java
// Dynamic expressions
@PreAuthorize("isAuthenticated() and (@securityService.isHiveOwner(#hiveId) or @securityService.canModerateHive(#hiveId))")
public void complexOperation(@PathVariable String hiveId) { ... }

// Permission evaluator support
@PreAuthorize("hasPermission(#hiveId, 'Hive', 'MODERATE')")
public void moderateHive(@PathVariable String hiveId) { ... }
```

### ✅ 6. Comprehensive Audit Logging
Security events are tracked and logged:

```java
// Automatic audit logging
SecurityAuditEvent {
    userId: "user-123",
    operation: "hive:delete",
    resourceId: "hive-456",
    granted: false,
    timestamp: "2025-01-14T10:30:00"
}

// Permission change tracking
PermissionChangeEvent {
    userId: "user-123",
    permission: "hive:member",
    resourceId: "hive-456",
    changeType: GRANTED/REVOKED,
    changedByUserId: "admin-789"
}
```

### ✅ 7. Performance Requirements
- **Authorization checks complete within 5ms** (performance-tested)
- **In-memory caching** for frequently accessed permissions
- **Efficient database queries** with proper indexing
- **Minimal overhead** on existing authentication flow

## Implementation Architecture

### Core Components

1. **SecurityService** (`/api/service/SecurityService.java`)
   - Central authorization logic
   - Permission evaluation methods
   - Audit logging and tracking
   - SpEL expression support

2. **Custom Security Annotations** (`/api/security/annotations/`)
   - `@IsHiveOwner` - Ownership verification
   - `@IsHiveMember` - Membership verification
   - `@CanModerateHive` - Moderation rights
   - `@HasPermission` - Generic permission checks
   - `@RequiresRole` - Role-based authorization

3. **Permission Evaluator** (`/api/security/FocusHivePermissionEvaluator.java`)
   - Complex authorization logic
   - Support for `hasPermission()` expressions
   - Resource-type-specific evaluation

4. **Security Configuration** (`/api/config/MethodSecurityConfig.java`)
   - Method security enablement
   - Permission evaluator integration
   - Expression handler configuration

5. **Audit System**
   - `SecurityAuditEvent` - Authorization attempt tracking
   - `PermissionChangeEvent` - Permission change history
   - Thread-safe event queues and history maps

## TDD Test Coverage

Comprehensive test suite covering:

### Resource Ownership Tests
- ✅ Hive owner can delete their hive
- ✅ Non-members denied access to private hives
- ✅ Public hive access for non-members (read-only)
- ✅ Role-based access control enforcement

### Permission-Based Access Tests
- ✅ `hive:create` permission validation
- ✅ `hive:update` with ownership checks
- ✅ `member:invite` for moderators
- ✅ `member:remove` denied for regular members

### Role Hierarchy Tests
- ✅ Admin role has all system permissions
- ✅ Moderator role limited to assigned hives
- ✅ User role system limitations enforced

### Dynamic Permission Tests
- ✅ SpEL expression evaluation
- ✅ Complex ownership and membership expressions
- ✅ Dynamic membership permission changes

### Security Audit Tests
- ✅ All authorization attempts recorded
- ✅ Permission change tracking over time
- ✅ Audit event persistence and querying

### Performance Tests
- ✅ Authorization checks complete within 5ms
- ✅ Concurrent access performance validation

## Usage Examples

### Controller Security

```java
@RestController
@RequestMapping("/api/v1/hives")
public class HiveController {

    @PostMapping
    @HasPermission("hive:create")
    public ResponseEntity<HiveResponse> createHive(@RequestBody CreateHiveRequest request) { ... }

    @GetMapping("/{id}")
    @PreAuthorize("@securityService.hasAccessToHive(#id)")
    public ResponseEntity<HiveResponse> getHive(@PathVariable String id) { ... }

    @PutMapping("/{id}")
    @CanModerateHive("#id")
    public ResponseEntity<HiveResponse> updateHive(@PathVariable String id, @RequestBody UpdateHiveRequest request) { ... }

    @DeleteMapping("/{id}")
    @IsHiveOwner("#id")
    public ResponseEntity<Void> deleteHive(@PathVariable String id) { ... }

    @PostMapping("/{id}/members/{memberId}/remove")
    @HasPermission(value = "member:remove", resourceId = "#id")
    public ResponseEntity<Void> removeMember(@PathVariable String id, @PathVariable String memberId) { ... }
}
```

### Service Layer Security

```java
@Service
public class HiveService {

    @PreAuthorize("hasPermission(#hiveId, 'Hive', 'READ')")
    public HiveResponse getHiveDetails(String hiveId) { ... }

    @PreAuthorize("hasPermission(#hiveId, 'Hive', 'MODERATE')")
    public void updateHiveSettings(String hiveId, HiveSettingsDto settings) { ... }
}
```

## Security Best Practices Implemented

1. **Least Privilege Principle** - Users granted minimal necessary permissions
2. **Defense in Depth** - Multiple security layers (authentication + authorization)
3. **Fail-Safe Defaults** - Deny access by default, explicit grants required
4. **Audit Trail** - All security events logged for compliance and monitoring
5. **Performance Optimized** - Efficient permission checks with minimal overhead
6. **Separation of Concerns** - Authorization logic centralized and reusable

## Migration Guide

### From Basic Security to Enhanced Authorization

1. **Replace basic @PreAuthorize expressions:**
   ```java
   // Before
   @PreAuthorize("isAuthenticated()")

   // After
   @HasPermission("hive:create")
   ```

2. **Add resource-specific checks:**
   ```java
   // Before
   @PreAuthorize("isAuthenticated()")

   // After
   @IsHiveOwner("#hiveId")
   ```

3. **Implement audit logging:**
   ```java
   // Add to service methods
   securityService.logAuthorizationAttempt("hive:delete", hiveId, hasAccess);
   ```

## Completion Status

✅ **Phase 2, Task 2.4: Authorization Rules - COMPLETED**

- [x] Resource ownership verification
- [x] Role-based access control (RBAC)
- [x] Permission hierarchy implementation
- [x] Method-level security annotations
- [x] Dynamic permission evaluation
- [x] Security audit logging
- [x] Permission change tracking
- [x] Performance optimization (<5ms)
- [x] Comprehensive TDD test suite
- [x] Documentation and usage examples

This completes the final task of Phase 2: Authentication & Security for the FocusHive Backend Service.