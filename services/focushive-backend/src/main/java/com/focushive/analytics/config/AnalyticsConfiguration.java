package com.focushive.analytics.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Configuration for Analytics Service.
 * Enables async processing for events and background tasks.
 */
@Configuration
@EnableAsync
@Slf4j
public class AnalyticsConfiguration {

    /**
     * Thread pool executor for analytics event processing
     */
    @Bean(name = "analyticsTaskExecutor")
    public Executor analyticsTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("analytics-");
        executor.setRejectedExecutionHandler((runnable, threadPoolExecutor) -> {
            log.warn("Analytics task queue is full, executing task in calling thread");
            runnable.run();
        });
        executor.initialize();

        log.info("Configured analytics task executor with core pool size: {}, max pool size: {}",
            executor.getCorePoolSize(), executor.getMaxPoolSize());

        return executor;
    }

    /**
     * Achievement processing executor for high-priority notifications
     */
    @Bean(name = "achievementTaskExecutor")
    public Executor achievementTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(3);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("achievement-");
        executor.setRejectedExecutionHandler((runnable, threadPoolExecutor) -> {
            log.warn("Achievement task queue is full, executing task in calling thread");
            runnable.run();
        });
        executor.initialize();

        log.info("Configured achievement task executor with core pool size: {}, max pool size: {}",
            executor.getCorePoolSize(), executor.getMaxPoolSize());

        return executor;
    }
}