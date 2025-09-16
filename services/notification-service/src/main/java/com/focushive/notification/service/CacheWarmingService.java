package com.focushive.notification.service;

import com.focushive.notification.entity.NotificationTemplate;
import com.focushive.notification.entity.NotificationType;
import com.focushive.notification.repository.NotificationTemplateRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Service for warming template cache on application startup.
 * Ensures templates are pre-loaded for optimal performance.
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "cache.warming.enabled", havingValue = "true", matchIfMissing = true)
public class CacheWarmingService implements CacheWarmingScheduler, ApplicationRunner, ApplicationListener<ContextRefreshedEvent> {

    private final NotificationTemplateRepository templateRepository;
    private final CacheManager cacheManager;
    private final CachedTemplateService cachedTemplateService;
    private final MeterRegistry meterRegistry;

    @Value("${cache.warming.batch-size:50}")
    private int batchSize;

    @Value("${cache.warming.critical-only:false}")
    private boolean criticalOnly;

    @Value("${cache.warming.async:true}")
    private boolean asyncWarming;

    @Value("${cache.warming.schedule.enabled:false}")
    private boolean scheduleEnabled;

    @Value("${cache.warming.frequent-templates.count:10}")
    private int frequentTemplatesCount;

    private final ExecutorService warmingExecutor = Executors.newFixedThreadPool(3);
    private final Map<String, CacheWarmingProgress> warmingProgressMap = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    private volatile boolean isWarmingInProgress = false;
    private volatile LocalDateTime lastWarmingTime;
    private final WarmingStatistics statistics = new WarmingStatistics();

    public CacheWarmingService(NotificationTemplateRepository templateRepository,
                              CacheManager cacheManager,
                              CachedTemplateService cachedTemplateService,
                              MeterRegistry meterRegistry) {
        this.templateRepository = templateRepository;
        this.cacheManager = cacheManager;
        this.cachedTemplateService = cachedTemplateService;
        this.meterRegistry = meterRegistry;
    }

    /**
     * Run cache warming on application startup.
     */
    @Override
    public void run(ApplicationArguments args) {
        if (shouldWarmCache()) {
            log.info("Starting cache warming on application startup...");

            if (asyncWarming) {
                warmCacheAsync();
            } else {
                warmCache();
            }
        } else {
            log.info("Cache warming disabled by configuration");
        }
    }

    /**
     * Handle context refresh event for cache warming.
     */
    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        // Additional warming on context refresh if needed
        if (!isWarmingInProgress && shouldWarmCache()) {
            warmCacheAsync();
        }
    }

    /**
     * Warm cache synchronously.
     */
    public void warmCache() {
        if (isWarmingInProgress) {
            log.info("Cache warming already in progress, skipping...");
            return;
        }

        isWarmingInProgress = true;
        Timer.Sample sample = Timer.start(meterRegistry);
        LocalDateTime startTime = LocalDateTime.now();

        try {
            List<NotificationTemplate> templates = getTemplatesToWarm();
            int totalTemplates = templates.size();
            AtomicInteger processed = new AtomicInteger(0);
            AtomicInteger failed = new AtomicInteger(0);

            log.info("Warming cache with {} templates", totalTemplates);

            for (NotificationTemplate template : templates) {
                try {
                    cachedTemplateService.cacheTemplate(template);
                    processed.incrementAndGet();

                    // Log progress
                    if (processed.get() % 10 == 0) {
                        log.debug("Cache warming progress: {}/{}", processed.get(), totalTemplates);
                    }
                } catch (Exception e) {
                    log.error("Failed to cache template: {} in {}",
                        template.getNotificationType(), template.getLanguage(), e);
                    failed.incrementAndGet();
                }
            }

            Duration duration = Duration.between(startTime, LocalDateTime.now());
            lastWarmingTime = LocalDateTime.now();

            // Update statistics
            statistics.setTotalTemplatesCached(processed.get());
            statistics.setFailedTemplates(failed.get());
            statistics.setWarmingDuration(duration);
            statistics.setLastWarmingTime(lastWarmingTime);

            log.info("Cache warming completed: {} templates cached, {} failed in {}ms",
                processed.get(), failed.get(), duration.toMillis());

            // Record metrics
            meterRegistry.gauge("cache.warming.templates.total", processed.get());
            meterRegistry.gauge("cache.warming.templates.failed", failed.get());

        } catch (Exception e) {
            log.error("Cache warming failed", e);
            statistics.incrementFailures();
        } finally {
            isWarmingInProgress = false;
            sample.stop(Timer.builder("cache.warming.duration").register(meterRegistry));
        }
    }

    /**
     * Warm cache asynchronously.
     */
    @Override
    @Async
    public CompletableFuture<Void> warmCacheAsync() {
        return CompletableFuture.runAsync(this::warmCache, warmingExecutor);
    }

    /**
     * Warm cache with priority for critical templates.
     */
    public void warmCacheWithPriority() {
        log.info("Starting priority-based cache warming...");

        // First warm critical templates
        List<NotificationTemplate> criticalTemplates = getCriticalTemplates();
        for (NotificationTemplate template : criticalTemplates) {
            try {
                cachedTemplateService.cacheTemplate(template);
                log.debug("Cached critical template: {} in {}",
                    template.getNotificationType(), template.getLanguage());
            } catch (Exception e) {
                log.error("Failed to cache critical template: {} in {}",
                    template.getNotificationType(), template.getLanguage(), e);
            }
        }

        // Then warm normal templates
        List<NotificationTemplate> normalTemplates = getNormalTemplates();
        for (NotificationTemplate template : normalTemplates) {
            try {
                cachedTemplateService.cacheTemplate(template);
            } catch (Exception e) {
                log.warn("Failed to cache normal template: {} in {}",
                    template.getNotificationType(), template.getLanguage(), e);
            }
        }
    }

    /**
     * Warm cache in batches for large datasets.
     */
    public void warmCacheInBatches(int batchSizeOverride) {
        int effectiveBatchSize = batchSizeOverride > 0 ? batchSizeOverride : batchSize;
        log.info("Starting batch cache warming with batch size: {}", effectiveBatchSize);

        Pageable pageable = PageRequest.of(0, effectiveBatchSize);
        Page<NotificationTemplate> page;
        int totalProcessed = 0;

        do {
            page = templateRepository.findAll(pageable);
            List<NotificationTemplate> batch = page.getContent();

            for (NotificationTemplate template : batch) {
                try {
                    cachedTemplateService.cacheTemplate(template);
                    totalProcessed++;
                } catch (Exception e) {
                    log.error("Failed to cache template in batch: {} in {}",
                        template.getNotificationType(), template.getLanguage(), e);
                }
            }

            log.debug("Processed batch {} of {}", page.getNumber() + 1, page.getTotalPages());
            pageable = pageable.next();

        } while (page.hasNext());

        log.info("Batch cache warming completed: {} templates processed", totalProcessed);
    }

    /**
     * Warm cache with progress tracking.
     */
    public CacheWarmingProgress warmCacheWithProgress() {
        String progressId = UUID.randomUUID().toString();
        CacheWarmingProgress progress = new CacheWarmingProgress(progressId);
        warmingProgressMap.put(progressId, progress);

        CompletableFuture.runAsync(() -> {
            try {
                List<NotificationTemplate> templates = getTemplatesToWarm();
                progress.setTotalItems(templates.size());
                progress.setStartTime(LocalDateTime.now());

                for (NotificationTemplate template : templates) {
                    try {
                        cachedTemplateService.cacheTemplate(template);
                        progress.incrementProcessed();
                    } catch (Exception e) {
                        log.error("Failed to cache template: {} in {}",
                            template.getNotificationType(), template.getLanguage(), e);
                        progress.incrementFailed();
                    }
                }

                progress.setEndTime(LocalDateTime.now());
                progress.setComplete(true);

            } catch (Exception e) {
                log.error("Cache warming with progress failed", e);
                progress.setComplete(true);
                progress.setError(e.getMessage());
            }
        }, warmingExecutor);

        return progress;
    }

    /**
     * Warm frequently accessed templates.
     */
    public void warmFrequentlyAccessedTemplates(int topN) {
        Map<String, Long> accessCounts = getTemplateAccessCounts();

        List<String> topTemplates = accessCounts.entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .limit(topN)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());

        log.info("Warming {} frequently accessed templates", topTemplates.size());

        for (String templateKey : topTemplates) {
            // templateKey format is "TYPE:LANGUAGE"
            String[] parts = templateKey.split(":");
            if (parts.length == 2) {
                try {
                    NotificationType type = NotificationType.valueOf(parts[0]);
                    String language = parts[1];
                    Optional<NotificationTemplate> template =
                        templateRepository.findByNotificationTypeAndLanguage(type, language);
                    if (template.isPresent()) {
                        cachedTemplateService.cacheTemplate(template.get());
                        log.debug("Cached frequent template: {} with {} accesses",
                            templateKey, accessCounts.get(templateKey));
                    }
                } catch (Exception e) {
                    log.error("Failed to cache frequent template: {}", templateKey, e);
                }
            }
        }
    }

    /**
     * Schedule periodic cache warming.
     */
    @PostConstruct
    public void initializeScheduling() {
        if (scheduleEnabled) {
            schedulePeriodicWarming(1, TimeUnit.HOURS);
        }
    }

    /**
     * Schedule periodic cache warming.
     */
    public void schedulePeriodicWarming(long period, TimeUnit unit) {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                log.info("Running scheduled cache warming...");
                warmCache();
            } catch (Exception e) {
                log.error("Scheduled cache warming failed", e);
            }
        }, period, period, unit);

        log.info("Scheduled cache warming to run every {} {}", period, unit);
    }

    /**
     * Scheduled cache warming (if enabled via configuration).
     */
    @Override
    @Scheduled(cron = "${cache.warming.schedule.cron:0 0 3 * * ?}") // Default: 3 AM daily
    @ConditionalOnProperty(name = "cache.warming.schedule.enabled", havingValue = "true")
    public void scheduledCacheWarming() {
        log.info("Running scheduled cache warming via cron...");
        warmCacheAsync();
    }

    /**
     * Warm rendered templates with common variables.
     */
    public void warmRenderedTemplates(NotificationTemplate template, List<Map<String, Object>> commonVariables) {
        log.debug("Warming rendered templates for: {} in {} with {} variations",
            template.getNotificationType(), template.getLanguage(), commonVariables.size());

        for (Map<String, Object> variables : commonVariables) {
            try {
                String rendered = cachedTemplateService.renderTemplate(
                    template.getNotificationType(), template.getLanguage(), variables);
                log.trace("Cached rendered template with variables: {}", variables);
            } catch (Exception e) {
                log.error("Failed to cache rendered template: {} in {} with variables: {}",
                    template.getNotificationType(), template.getLanguage(), variables, e);
            }
        }
    }

    /**
     * Warm cache with TTL preservation.
     */
    public void warmCacheWithTTL(NotificationTemplate template) {
        Duration ttl = Duration.ofHours(24); // Default TTL
        // Could be customized per template if needed
        cachedTemplateService.cacheTemplateWithTTL(template, ttl);
    }

    /**
     * Check if cache warming should be performed.
     */
    public boolean shouldWarmCache() {
        // Check system properties or environment variables
        String enabled = System.getProperty("cache.warming.enabled",
            System.getenv("CACHE_WARMING_ENABLED"));

        if ("false".equalsIgnoreCase(enabled)) {
            return false;
        }

        // Check if critical-only mode
        if (criticalOnly) {
            String mode = System.getProperty("cache.warming.templates", "all");
            return "critical".equals(mode);
        }

        return true;
    }

    /**
     * Check if warming is scheduled.
     */
    public boolean isWarmingScheduled() {
        return !scheduler.isShutdown() && !scheduler.isTerminated();
    }

    /**
     * Get cache warming statistics.
     */
    public CacheWarmingStatistics getStatistics() {
        return new CacheWarmingStatistics(statistics);
    }

    /**
     * Get templates to warm based on configuration.
     */
    private List<NotificationTemplate> getTemplatesToWarm() {
        if (criticalOnly) {
            return getCriticalTemplates();
        }
        return templateRepository.findAll();
    }

    /**
     * Get critical templates (high priority).
     */
    private List<NotificationTemplate> getCriticalTemplates() {
        // Define critical notification types
        List<NotificationType> criticalTypes = Arrays.asList(
            NotificationType.PASSWORD_RESET,
            NotificationType.EMAIL_VERIFICATION,
            NotificationType.SESSION_REMINDER
        );

        List<NotificationTemplate> criticalTemplates = new ArrayList<>();
        for (NotificationType type : criticalTypes) {
            criticalTemplates.addAll(templateRepository.findByNotificationType(type));
        }
        return criticalTemplates;
    }

    /**
     * Get normal templates (lower priority).
     */
    private List<NotificationTemplate> getNormalTemplates() {
        List<NotificationType> criticalTypes = Arrays.asList(
            NotificationType.PASSWORD_RESET,
            NotificationType.EMAIL_VERIFICATION,
            NotificationType.SESSION_REMINDER
        );

        List<NotificationTemplate> allTemplates = templateRepository.findAll();
        return allTemplates.stream()
            .filter(t -> !criticalTypes.contains(t.getNotificationType()))
            .collect(Collectors.toList());
    }

    /**
     * Get template access counts for frequency-based warming.
     */
    private Map<String, Long> getTemplateAccessCounts() {
        // In production, this would query actual access logs or metrics
        // For now, returning mock data with TYPE:LANGUAGE format
        Map<String, Long> counts = new HashMap<>();
        counts.put("WELCOME:en", 1000L);
        counts.put("PASSWORD_RESET:en", 500L);
        counts.put("SESSION_REMINDER:en", 300L);
        counts.put("HIVE_INVITATION:en", 100L);
        return counts;
    }

    /**
     * Cache warming progress tracker.
     */
    public static class CacheWarmingProgress {
        private final String id;
        private int totalItems;
        private final AtomicInteger processedItems = new AtomicInteger(0);
        private final AtomicInteger failedItems = new AtomicInteger(0);
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private boolean complete;
        private String error;

        public CacheWarmingProgress(String id) {
            this.id = id;
        }

        public void incrementProcessed() { processedItems.incrementAndGet(); }
        public void incrementFailed() { failedItems.incrementAndGet(); }

        // Getters and setters
        public String getId() { return id; }
        public int getTotalItems() { return totalItems; }
        public void setTotalItems(int totalItems) { this.totalItems = totalItems; }
        public int getProcessedItems() { return processedItems.get(); }
        public int getFailedItems() { return failedItems.get(); }
        public LocalDateTime getStartTime() { return startTime; }
        public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
        public LocalDateTime getEndTime() { return endTime; }
        public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
        public boolean isComplete() { return complete; }
        public void setComplete(boolean complete) { this.complete = complete; }
        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
    }

    /**
     * Cache warming statistics.
     */
    public static class CacheWarmingStatistics {
        private int totalTemplatesCached;
        private int failedTemplates;
        private Duration warmingDuration;
        private LocalDateTime lastWarmingTime;
        private int totalFailures;

        public CacheWarmingStatistics() {}

        public CacheWarmingStatistics(WarmingStatistics source) {
            this.totalTemplatesCached = source.totalTemplatesCached;
            this.failedTemplates = source.failedTemplates;
            this.warmingDuration = source.warmingDuration;
            this.lastWarmingTime = source.lastWarmingTime;
            this.totalFailures = source.totalFailures;
        }

        // Getters
        public int getTotalTemplatesCached() { return totalTemplatesCached; }
        public int getFailedTemplates() { return failedTemplates; }
        public Duration getWarmingDuration() { return warmingDuration; }
        public LocalDateTime getLastWarmingTime() { return lastWarmingTime; }
        public int getTotalFailures() { return totalFailures; }
    }

    /**
     * Internal warming statistics.
     */
    private static class WarmingStatistics {
        private int totalTemplatesCached;
        private int failedTemplates;
        private Duration warmingDuration;
        private LocalDateTime lastWarmingTime;
        private int totalFailures;

        public void incrementFailures() { this.totalFailures++; }

        // Setters
        public void setTotalTemplatesCached(int count) { this.totalTemplatesCached = count; }
        public void setFailedTemplates(int count) { this.failedTemplates = count; }
        public void setWarmingDuration(Duration duration) { this.warmingDuration = duration; }
        public void setLastWarmingTime(LocalDateTime time) { this.lastWarmingTime = time; }
    }
}