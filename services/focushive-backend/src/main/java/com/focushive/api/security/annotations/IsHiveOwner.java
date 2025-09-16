package com.focushive.api.security.annotations;

import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom security annotation to check if the current user is the owner of a hive.
 * Part of Phase 2, Task 2.4: Authorization Rules implementation.
 *
 * Usage:
 * @IsHiveOwner("hiveId")
 * public void deleteHive(@PathVariable String hiveId) { ... }
 *
 * @IsHiveOwner("#request.hiveId")
 * public void updateHive(@RequestBody UpdateHiveRequest request) { ... }
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("isAuthenticated() and @securityService.isHiveOwner(#hiveId)")
public @interface IsHiveOwner {

    /**
     * SpEL expression to extract the hive ID from method parameters.
     * Default assumes a parameter named "hiveId".
     */
    String value() default "#hiveId";
}