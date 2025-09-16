package com.focushive.api.security.annotations;

import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom security annotation to check if the current user has a specific role.
 * Part of Phase 2, Task 2.4: Authorization Rules implementation.
 *
 * Usage:
 * @RequiresRole("ADMIN")
 * public void deleteUser(@PathVariable String userId) { ... }
 *
 * @RequiresRole(value = "MODERATOR", anyRole = {"ADMIN", "MODERATOR"})
 * public void moderateContent(@RequestBody ModerationRequest request) { ... }
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("isAuthenticated() and " +
    "(#anyRole.length > 0 ? " +
    "T(java.util.Arrays).stream(#anyRole).anyMatch(role -> @securityService.hasRole(role)) : " +
    "@securityService.hasRole(#value))")
public @interface RequiresRole {

    /**
     * The required role (without ROLE_ prefix).
     */
    String value();

    /**
     * Alternative roles - user needs at least one of these roles.
     * If specified, the value() parameter is ignored.
     */
    String[] anyRole() default {};
}