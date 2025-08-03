package com.focushive.test;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Minimal test application for WebMvcTest slices.
 * Excludes problematic auto-configurations that cause issues in slice tests.
 */
@SpringBootApplication(exclude = {
        HibernateJpaAutoConfiguration.class
})
@ComponentScan(basePackages = {
        "com.focushive.chat.controller",
        "com.focushive.presence.controller", 
        "com.focushive.timer.controller",
        "com.focushive.config"
})
public class MinimalTestApplication {
    // Empty - used only for slice tests
}