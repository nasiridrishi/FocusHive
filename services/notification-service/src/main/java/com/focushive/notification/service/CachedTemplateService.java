package com.focushive.notification.service;

import com.focushive.notification.entity.NotificationTemplate;
import com.focushive.notification.entity.NotificationType;
import com.focushive.notification.repository.NotificationTemplateRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Service for caching notification templates.
 * Provides multi-level caching with metrics and invalidation.
 */
@Slf4j
@Service
@CacheConfig(cacheNames = "templates")
public class CachedTemplateService {

    private final NotificationTemplateRepository templateRepository;
    private final NotificationTemplateService templateService;
    private final TemplateEngine templateEngine;
    private final CacheManager cacheManager;
    private final MeterRegistry meterRegistry;

    // Cache statistics
    private final Map<String, CacheStatistics> cacheStats = new ConcurrentHashMap<>();

    // Frequently used templates tracking
    private final Map<String, Long> accessCounts = new ConcurrentHashMap<>();

    public CachedTemplateService(NotificationTemplateRepository templateRepository,
                                 NotificationTemplateService templateService,
                                 TemplateEngine templateEngine,
                                 CacheManager cacheManager,
                                 MeterRegistry meterRegistry) {
        this.templateRepository = templateRepository;
        this.templateService = templateService;
        this.templateEngine = templateEngine;
        this.cacheManager = cacheManager;
        this.meterRegistry = meterRegistry;

        // Initialize cache statistics
        initializeCacheStatistics();
    }

    /**
     * Get template with caching.
     */
    @Cacheable(value = "templates", key = "#notificationType.name() + ':' + #language", unless = "#result == null")
    public NotificationTemplate getTemplate(NotificationType notificationType, String language) {
        Timer.Sample sample = Timer.start(meterRegistry);

        try {
            // Check if we have a cache hit (this is tracked by Spring Cache)
            Optional<NotificationTemplate> template = templateService.findTemplate(notificationType, language);

            if (template.isPresent()) {
                // Track access for frequently used templates
                accessCounts.merge(notificationType.name() + ":" + language, 1L, Long::sum);
                recordCacheMiss(notificationType.name()); // This was a cache miss since we're in the method
                return template.get();
            }

            return null;
        } finally {
            sample.stop(Timer.builder("cache.template.fetch")
                .tag("type", notificationType.name())
                .tag("language", language)
                .register(meterRegistry));
        }
    }

    /**
     * Get cached template directly from cache.
     */
    public Optional<NotificationTemplate> getCachedTemplate(NotificationType notificationType, String language) {
        Cache cache = cacheManager.getCache("templates");
        if (cache != null) {
            String cacheKey = notificationType.name() + ":" + language;
            Cache.ValueWrapper wrapper = cache.get(cacheKey);
            if (wrapper != null && wrapper.get() instanceof NotificationTemplate) {
                recordCacheHit(notificationType.name());
                return Optional.of((NotificationTemplate) wrapper.get());
            }
        }
        recordCacheMiss(notificationType.name());
        return Optional.empty();
    }

    /**
     * Cache a template explicitly.
     */
    @CachePut(value = "templates", key = "#template.notificationType.name() + ':' + #template.language")
    public NotificationTemplate cacheTemplate(NotificationTemplate template) {
        log.debug("Caching template: {} in language: {}", template.getNotificationType(), template.getLanguage());
        return template;
    }

    /**
     * Render template with caching.
     */
    @Cacheable(value = "rendered-templates",
              key = "T(com.focushive.notification.config.TemplateCacheConfig.TemplateKeyGenerator).new().generate(null, null, #notificationType.name(), #language, #variables)")
    public String renderTemplate(NotificationType notificationType, String language, Map<String, Object> variables) {
        Timer.Sample sample = Timer.start(meterRegistry);

        try {
            // Get the template
            NotificationTemplate template = getTemplate(notificationType, language);
            if (template == null) {
                throw new IllegalArgumentException("Template not found: " + notificationType + "/" + language);
            }

            // Render the template using Thymeleaf
            Context context = new Context();
            context.setVariables(variables);

            // Use the HTML body if available, otherwise use text body
            String templateContent = template.hasHtmlContent() ? template.getBodyHtml() : template.getBodyText();
            String rendered = templateEngine.process(templateContent, context);

            log.debug("Rendered template: {}/{} with {} variables", notificationType, language, variables.size());
            return rendered;
        } finally {
            sample.stop(Timer.builder("template.render")
                .tag("type", notificationType.name())
                .tag("language", language)
                .register(meterRegistry));
        }
    }

    /**
     * Update template and invalidate cache.
     */
    @CacheEvict(value = {"templates", "rendered-templates"}, allEntries = false,
                key = "#template.notificationType.name() + ':' + #template.language")
    @Transactional
    public NotificationTemplate updateTemplate(NotificationTemplate template) {
        log.info("Updating template and invalidating cache: {}/{}",
            template.getNotificationType(), template.getLanguage());

        // Save the updated template
        NotificationTemplate updated = templateRepository.save(template);

        // Invalidate all rendered versions of this template
        invalidateRenderedTemplates(template.getNotificationType());

        return updated;
    }

    /**
     * Delete template and clear cache.
     */
    @CacheEvict(value = {"templates", "rendered-templates"}, allEntries = false,
                key = "#notificationType.name() + ':' + #language")
    @Transactional
    public void deleteTemplate(NotificationType notificationType, String language) {
        log.info("Deleting template and clearing cache: {}/{}", notificationType, language);
        templateRepository.deleteByNotificationTypeAndLanguage(notificationType, language);
    }

    /**
     * Warm cache with all templates.
     */
    public CompletableFuture<Void> warmCache() {
        return CompletableFuture.runAsync(() -> {
            log.info("Starting cache warming...");
            long startTime = System.currentTimeMillis();

            List<NotificationTemplate> allTemplates = templateRepository.findAll();
            int cached = 0;

            for (NotificationTemplate template : allTemplates) {
                try {
                    cacheTemplate(template);
                    cached++;
                } catch (Exception e) {
                    log.error("Failed to cache template: {}/{}",
                        template.getNotificationType(), template.getLanguage(), e);
                }
            }

            long duration = System.currentTimeMillis() - startTime;
            log.info("Cache warming completed: {} templates cached in {}ms", cached, duration);

            meterRegistry.gauge("cache.warmed.templates", cached);
            meterRegistry.timer("cache.warming.duration").record(Duration.ofMillis(duration));
        });
    }

    /**
     * Warm cache with frequently used templates.
     */
    public void warmFrequentTemplates(int topN) {
        List<Map.Entry<String, Long>> sortedTemplates = accessCounts.entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .limit(topN)
            .collect(Collectors.toList());

        for (Map.Entry<String, Long> entry : sortedTemplates) {
            String[] parts = entry.getKey().split(":");
            if (parts.length == 2) {
                try {
                    NotificationType type = NotificationType.valueOf(parts[0]);
                    String language = parts[1];

                    Optional<NotificationTemplate> template = templateService.findTemplate(type, language);
                    if (template.isPresent()) {
                        cacheTemplate(template.get());
                        log.debug("Cached frequent template: {} with {} accesses",
                            entry.getKey(), entry.getValue());
                    }
                } catch (Exception e) {
                    log.error("Failed to warm frequent template: {}", entry.getKey(), e);
                }
            }
        }
    }

    /**
     * Get templates in batch with caching.
     */
    public Map<NotificationType, NotificationTemplate> getTemplatesBatch(List<NotificationType> types, String language) {
        Map<NotificationType, NotificationTemplate> result = new HashMap<>();
        List<NotificationType> missingTypes = new ArrayList<>();

        // Check cache first
        for (NotificationType type : types) {
            Optional<NotificationTemplate> cached = getCachedTemplate(type, language);
            if (cached.isPresent()) {
                result.put(type, cached.get());
            } else {
                missingTypes.add(type);
            }
        }

        // Fetch missing templates from database
        if (!missingTypes.isEmpty()) {
            for (NotificationType type : missingTypes) {
                Optional<NotificationTemplate> template = templateService.findTemplate(type, language);
                template.ifPresent(t -> {
                    cacheTemplate(t);
                    result.put(type, t);
                });
            }
        }

        return result;
    }

    /**
     * Get template metadata with caching.
     */
    @Cacheable(value = "template-metadata", key = "#notificationType.name() + ':' + #language")
    public TemplateMetadata getTemplateMetadata(NotificationType notificationType, String language) {
        NotificationTemplate template = getTemplate(notificationType, language);
        if (template == null) {
            return null;
        }

        return new TemplateMetadata(
            template.getNotificationType().name(),
            template.getSubject(),
            extractVariables(template.getBodyText()),
            template.getUpdatedAt() != null ? template.getUpdatedAt() : template.getCreatedAt()
        );
    }

    /**
     * Preload frequently used templates based on notification types.
     */
    public void preloadFrequentTemplates() {
        // Common notification types that are frequently used
        List<NotificationType> frequentTypes = Arrays.asList(
            NotificationType.PASSWORD_RESET,
            NotificationType.EMAIL_VERIFICATION,
            NotificationType.SESSION_REMINDER,
            NotificationType.WELCOME
        );

        List<String> languages = Arrays.asList("en", "es", "fr");

        for (NotificationType type : frequentTypes) {
            for (String language : languages) {
                try {
                    Optional<NotificationTemplate> template = templateService.findTemplate(type, language);
                    template.ifPresent(t -> {
                        cacheTemplate(t);
                        log.debug("Preloaded template: {}/{}", type, language);
                    });
                } catch (Exception e) {
                    log.error("Failed to preload template: {}/{}", type, language, e);
                }
            }
        }
    }

    /**
     * Cache template with custom TTL.
     */
    public void cacheTemplateWithTTL(NotificationTemplate template, Duration ttl) {
        // This would require custom cache configuration per template
        // For now, using default TTL from configuration
        cacheTemplate(template);
        log.debug("Cached template {}/{} with TTL: {}",
            template.getNotificationType(), template.getLanguage(), ttl);
    }

    /**
     * Conditionally cache based on template size.
     */
    public void cacheIfAppropriate(NotificationTemplate template) {
        // Don't cache very large templates (> 100KB)
        int maxSize = 100 * 1024; // 100KB
        String content = template.hasHtmlContent() ? template.getBodyHtml() : template.getBodyText();
        if (content != null && content.length() <= maxSize) {
            cacheTemplate(template);
        } else {
            log.info("Template {}/{} too large to cache: {} bytes",
                template.getNotificationType(), template.getLanguage(), content.length());
        }
    }

    /**
     * Get cache statistics.
     */
    public CacheStatistics getCacheStatistics() {
        Cache cache = cacheManager.getCache("templates");
        if (cache == null) {
            return new CacheStatistics();
        }

        CacheStatistics stats = cacheStats.computeIfAbsent("templates", k -> new CacheStatistics());

        // Calculate hit rate
        long totalRequests = stats.getHits() + stats.getMisses();
        if (totalRequests > 0) {
            stats.setHitRate((double) stats.getHits() / totalRequests * 100);
            stats.setMissRate((double) stats.getMisses() / totalRequests * 100);
        }

        return stats;
    }

    /**
     * Invalidate all rendered templates for a specific template type.
     */
    private void invalidateRenderedTemplates(NotificationType notificationType) {
        Cache renderedCache = cacheManager.getCache("rendered-templates");
        if (renderedCache != null) {
            // In production, you might want to track keys or use pattern matching
            // For now, we'll clear the entire rendered cache for safety
            renderedCache.clear();
            log.debug("Cleared all rendered templates due to update of: {}", notificationType);
        }
    }

    /**
     * Extract variable names from template content.
     */
    private List<String> extractVariables(String content) {
        List<String> variables = new ArrayList<>();
        if (content == null) return variables;

        // Simple regex to find Thymeleaf variables ${...} or template variables {{...}}
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\$\\{([^}]+)\\}|\\{\\{([^}]+)\\}\\}");
        java.util.regex.Matcher matcher = pattern.matcher(content);

        while (matcher.find()) {
            String variable = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
            // Remove any method calls or complex expressions
            if (variable != null && !variable.contains("(") && !variable.contains("#")) {
                variables.add(variable.split("\\.")[0]); // Get root variable name
            }
        }

        return variables.stream().distinct().collect(Collectors.toList());
    }

    /**
     * Initialize cache statistics.
     */
    private void initializeCacheStatistics() {
        cacheManager.getCacheNames().forEach(cacheName -> {
            cacheStats.put(cacheName, new CacheStatistics());
        });
    }

    /**
     * Record cache hit.
     */
    private void recordCacheHit(String templateType) {
        meterRegistry.counter("cache.hit", "cache", "templates", "type", templateType).increment();
        cacheStats.get("templates").incrementHits();
    }

    /**
     * Record cache miss.
     */
    private void recordCacheMiss(String templateType) {
        meterRegistry.counter("cache.miss", "cache", "templates", "type", templateType).increment();
        cacheStats.get("templates").incrementMisses();
    }

    /**
     * Template metadata class.
     */
    public static class TemplateMetadata {
        private final String type;
        private final String subject;
        private final List<String> variables;
        private final LocalDateTime lastModified;

        public TemplateMetadata(String type, String subject, List<String> variables, LocalDateTime lastModified) {
            this.type = type;
            this.subject = subject;
            this.variables = variables;
            this.lastModified = lastModified;
        }

        // Getters
        public String getType() { return type; }
        public String getSubject() { return subject; }
        public List<String> getVariables() { return variables; }
        public LocalDateTime getLastModified() { return lastModified; }
    }

    /**
     * Cache statistics class.
     */
    public static class CacheStatistics {
        private long hits = 0;
        private long misses = 0;
        private long evictions = 0;
        private double hitRate = 0.0;
        private double missRate = 0.0;
        private long size = 0;
        private long memoryUsage = 0;

        public void incrementHits() { this.hits++; }
        public void incrementMisses() { this.misses++; }
        public void incrementEvictions() { this.evictions++; }

        // Getters and setters
        public long getHits() { return hits; }
        public void setHits(long hits) { this.hits = hits; }

        public long getMisses() { return misses; }
        public void setMisses(long misses) { this.misses = misses; }

        public long getEvictions() { return evictions; }
        public void setEvictions(long evictions) { this.evictions = evictions; }

        public double getHitRate() { return hitRate; }
        public void setHitRate(double hitRate) { this.hitRate = hitRate; }

        public double getMissRate() { return missRate; }
        public void setMissRate(double missRate) { this.missRate = missRate; }

        public long getSize() { return size; }
        public void setSize(long size) { this.size = size; }

        public long getMemoryUsage() { return memoryUsage; }
        public void setMemoryUsage(long memoryUsage) { this.memoryUsage = memoryUsage; }
    }
}