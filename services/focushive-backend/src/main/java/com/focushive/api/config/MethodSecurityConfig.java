package com.focushive.api.config;

import com.focushive.api.security.FocusHivePermissionEvaluator;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

/**
 * Method security configuration for FocusHive authorization rules.
 * Configures custom permission evaluator for complex authorization logic.
 *
 * Part of Phase 2, Task 2.4: Authorization Rules implementation.
 */
@Configuration
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
@Profile("!test") // Don't load this configuration in test profile
public class MethodSecurityConfig {

    private final FocusHivePermissionEvaluator permissionEvaluator;

    /**
     * Configure method security expression handler with custom permission evaluator.
     */
    @Bean
    public MethodSecurityExpressionHandler methodSecurityExpressionHandler() {
        DefaultMethodSecurityExpressionHandler expressionHandler = new DefaultMethodSecurityExpressionHandler();
        expressionHandler.setPermissionEvaluator(permissionEvaluator);
        return expressionHandler;
    }
}