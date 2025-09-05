package com.focushive.notification.controller;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Test configuration for NotificationControllerTest that excludes JPA auditing
 * to prevent JPA metamodel initialization issues.
 */
@SpringBootApplication(scanBasePackages = {
    "com.focushive.notification",
    "com.focushive.backend.security", 
    "com.focushive.backend.service"
})
@EnableFeignClients
@EnableScheduling
public class NotificationControllerTestConfig {
    // This configuration excludes @EnableJpaAuditing to avoid JPA metamodel issues
}