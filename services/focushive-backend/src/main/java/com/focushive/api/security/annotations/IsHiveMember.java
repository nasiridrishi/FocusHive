package com.focushive.api.security.annotations;

import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom security annotation to check if the current user is a member of a hive.
 * Part of Phase 2, Task 2.4: Authorization Rules implementation.
 *
 * Usage:
 * @IsHiveMember("hiveId")
 * public void sendMessage(@PathVariable String hiveId, @RequestBody MessageRequest request) { ... }
 *
 * @IsHiveMember("#request.hiveId")
 * public void joinTimer(@RequestBody TimerRequest request) { ... }
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("isAuthenticated() and (@securityService.isHiveMember(#hiveId) or @securityService.isHiveOwner(#hiveId))")
public @interface IsHiveMember {

    /**
     * SpEL expression to extract the hive ID from method parameters.
     * Default assumes a parameter named "hiveId".
     */
    String value() default "#hiveId";
}