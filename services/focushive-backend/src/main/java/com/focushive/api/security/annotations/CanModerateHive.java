package com.focushive.api.security.annotations;

import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom security annotation to check if the current user can moderate a hive.
 * This includes both hive owners and moderators.
 * Part of Phase 2, Task 2.4: Authorization Rules implementation.
 *
 * Usage:
 * @CanModerateHive("hiveId")
 * public void kickMember(@PathVariable String hiveId, @PathVariable String memberId) { ... }
 *
 * @CanModerateHive("#request.hiveId")
 * public void updateHiveSettings(@RequestBody HiveSettingsRequest request) { ... }
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("isAuthenticated() and @securityService.canModerateHive(#hiveId)")
public @interface CanModerateHive {

    /**
     * SpEL expression to extract the hive ID from method parameters.
     * Default assumes a parameter named "hiveId".
     */
    String value() default "#hiveId";
}