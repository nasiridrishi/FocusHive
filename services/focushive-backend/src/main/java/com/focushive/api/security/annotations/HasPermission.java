package com.focushive.api.security.annotations;

import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom security annotation to check if the current user has a specific permission.
 * Part of Phase 2, Task 2.4: Authorization Rules implementation.
 *
 * Usage:
 * @HasPermission("hive:create")
 * public void createHive(@RequestBody CreateHiveRequest request) { ... }
 *
 * @HasPermission(value = "hive:update", resourceId = "#hiveId")
 * public void updateHive(@PathVariable String hiveId, @RequestBody UpdateHiveRequest request) { ... }
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("isAuthenticated() and " +
    "(T(org.springframework.util.StringUtils).hasText(#resourceId) ? " +
    "@securityService.hasPermissionOnResource(#permission, #resourceId) : " +
    "@securityService.hasPermission(#permission))")
public @interface HasPermission {

    /**
     * The permission to check (e.g., "hive:create", "user:profile").
     */
    String value();

    /**
     * Optional resource ID for resource-specific permission checks.
     * Can be a SpEL expression (e.g., "#hiveId", "#request.resourceId").
     */
    String resourceId() default "";
}