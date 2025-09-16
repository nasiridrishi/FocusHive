package com.focushive.buddy.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * Configuration for Spring Task Scheduling.
 *
 * Provides:
 * - Custom thread pool for scheduled tasks
 * - Error handling for failed scheduled tasks
 * - Performance monitoring and metrics
 * - Graceful shutdown handling
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class SchedulingConfig {

    private final BuddySchedulingProperties schedulingProperties;

    /**
     * Creates a custom TaskScheduler with configurable thread pool size.
     *
     * This scheduler is used by all @Scheduled methods in the application.
     */
    @Bean
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();

        // Configure thread pool
        int poolSize = schedulingProperties.getPerformance().getThreadPoolSize();
        scheduler.setPoolSize(poolSize);
        scheduler.setThreadNamePrefix("buddy-scheduler-");

        // Configure rejection and error handling
        scheduler.setRejectedExecutionHandler((runnable, executor) -> {
            log.warn("Scheduled task rejected - thread pool may be full. Pool size: {}, Active: {}, Queue: {}",
                poolSize, executor.getActiveCount(), executor.getQueue().size());
        });

        // Configure graceful shutdown
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(30);

        // Initialize and return
        scheduler.initialize();

        log.info("Configured TaskScheduler with thread pool size: {}", poolSize);
        return scheduler;
    }
}