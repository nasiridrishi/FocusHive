package com.focushive.notification.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.lang.reflect.Method;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Async configuration for the Notification Service.
 * Provides thread pool configuration for async email processing with proper error handling.
 *
 * Production Requirements (from TODO.md):
 * - Throughput: >100 emails/second
 * - Response time: <50ms for email queue acceptance
 * - Queue depth: <1000 under normal load
 * - Error rate: <0.1%
 */
@Configuration
@EnableAsync
@Slf4j
public class AsyncConfig implements AsyncConfigurer {

    /**
     * Email task executor with production-grade configuration.
     * Core pool size: 10 threads (baseline concurrency)
     * Max pool size: 50 threads (peak load handling)
     * Queue capacity: 500 (buffer for bursts)
     *
     * @return Configured ThreadPoolTaskExecutor for email processing
     */
    @Bean(name = "emailTaskExecutor")
    public ThreadPoolTaskExecutor emailTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // Core configuration as per TODO.md requirements
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(50);
        executor.setQueueCapacity(500);

        // Thread naming for debugging and monitoring
        executor.setThreadNamePrefix("email-async-");

        // Keep alive time for idle threads (60 seconds)
        executor.setKeepAliveSeconds(60);

        // Allow core threads to timeout and terminate if idle
        executor.setAllowCoreThreadTimeOut(true);

        // Wait for tasks to complete on shutdown
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);

        // Rejection policy: run in caller thread if queue is full
        // This provides backpressure and prevents email loss
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        // Initialize the executor
        executor.initialize();

        log.info("Email Task Executor configured: corePoolSize={}, maxPoolSize={}, queueCapacity={}",
                executor.getCorePoolSize(), executor.getMaxPoolSize(), executor.getQueueCapacity());

        return executor;
    }

    /**
     * Notification task executor for other async operations.
     * Smaller pool for non-email async tasks.
     */
    @Bean(name = "notificationTaskExecutor")
    public ThreadPoolTaskExecutor notificationTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("notification-async-");
        executor.setKeepAliveSeconds(60);
        executor.setAllowCoreThreadTimeOut(true);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(20);
        executor.setRejectedExecutionHandler(new CustomRejectedExecutionHandler());
        executor.initialize();

        return executor;
    }

    /**
     * Default async executor.
     */
    @Override
    public Executor getAsyncExecutor() {
        return emailTaskExecutor();
    }

    /**
     * Global async exception handler for uncaught exceptions in async methods.
     */
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return new CustomAsyncExceptionHandler();
    }

    /**
     * Custom rejected execution handler that logs rejections and tracks metrics.
     */
    public static class CustomRejectedExecutionHandler implements RejectedExecutionHandler {
        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            log.error("Task rejected from thread pool: {}, Active: {}, Queue Size: {}, Pool Size: {}",
                    r.toString(),
                    executor.getActiveCount(),
                    executor.getQueue().size(),
                    executor.getPoolSize());

            // Fallback: run in caller thread (provides backpressure)
            if (!executor.isShutdown()) {
                log.warn("Running rejected task in caller thread");
                r.run();
            }
        }
    }

    /**
     * Custom async exception handler for logging and monitoring.
     */
    public static class CustomAsyncExceptionHandler implements AsyncUncaughtExceptionHandler {
        @Override
        public void handleUncaughtException(Throwable throwable, Method method, Object... params) {
            log.error("Uncaught exception in async method '{}' with parameters {}",
                    method.getName(), params, throwable);

            // TODO: Send alert to monitoring system
            // TODO: Increment error counter metric

            // Log specific error types with more context
            if (throwable instanceof IllegalArgumentException) {
                log.error("Invalid arguments passed to async method: {}", method.getName());
            } else if (throwable instanceof NullPointerException) {
                log.error("Null pointer in async method: {}", method.getName());
            }
        }
    }
}