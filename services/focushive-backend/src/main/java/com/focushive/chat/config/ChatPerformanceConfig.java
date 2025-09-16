package com.focushive.chat.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Performance configuration for chat operations.
 * Optimizes threading, caching, and resource management.
 */
@Configuration
@EnableAsync
@ConfigurationProperties(prefix = "focushive.chat.performance")
public class ChatPerformanceConfig {

    private int corePoolSize = 10;
    private int maxPoolSize = 50;
    private int queueCapacity = 100;
    private String threadNamePrefix = "ChatAsync-";

    private int messageBatchSize = 50;
    private int reactionBatchSize = 100;
    private int searchResultLimit = 1000;
    private int attachmentMaxSize = 50 * 1024 * 1024; // 50MB

    /**
     * Async executor for chat operations.
     */
    @Bean(name = "chatAsyncExecutor")
    public Executor chatAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix(threadNamePrefix);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }

    /**
     * Async executor for WebSocket broadcasting.
     */
    @Bean(name = "websocketBroadcastExecutor")
    public Executor websocketBroadcastExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("WSBroadcast-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }

    // Getters and setters for configuration properties

    public int getCorePoolSize() {
        return corePoolSize;
    }

    public void setCorePoolSize(int corePoolSize) {
        this.corePoolSize = corePoolSize;
    }

    public int getMaxPoolSize() {
        return maxPoolSize;
    }

    public void setMaxPoolSize(int maxPoolSize) {
        this.maxPoolSize = maxPoolSize;
    }

    public int getQueueCapacity() {
        return queueCapacity;
    }

    public void setQueueCapacity(int queueCapacity) {
        this.queueCapacity = queueCapacity;
    }

    public String getThreadNamePrefix() {
        return threadNamePrefix;
    }

    public void setThreadNamePrefix(String threadNamePrefix) {
        this.threadNamePrefix = threadNamePrefix;
    }

    public int getMessageBatchSize() {
        return messageBatchSize;
    }

    public void setMessageBatchSize(int messageBatchSize) {
        this.messageBatchSize = messageBatchSize;
    }

    public int getReactionBatchSize() {
        return reactionBatchSize;
    }

    public void setReactionBatchSize(int reactionBatchSize) {
        this.reactionBatchSize = reactionBatchSize;
    }

    public int getSearchResultLimit() {
        return searchResultLimit;
    }

    public void setSearchResultLimit(int searchResultLimit) {
        this.searchResultLimit = searchResultLimit;
    }

    public int getAttachmentMaxSize() {
        return attachmentMaxSize;
    }

    public void setAttachmentMaxSize(int attachmentMaxSize) {
        this.attachmentMaxSize = attachmentMaxSize;
    }
}