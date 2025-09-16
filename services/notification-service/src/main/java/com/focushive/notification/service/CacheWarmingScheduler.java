package com.focushive.notification.service;

import java.util.concurrent.CompletableFuture;

/**
 * Interface for cache warming scheduled operations.
 * Separated to resolve proxy issues with @Scheduled annotations.
 */
public interface CacheWarmingScheduler {

    /**
     * Scheduled cache warming operation.
     */
    void scheduledCacheWarming();

    /**
     * Async cache warming operation.
     * @return CompletableFuture for async execution
     */
    CompletableFuture<Void> warmCacheAsync();
}