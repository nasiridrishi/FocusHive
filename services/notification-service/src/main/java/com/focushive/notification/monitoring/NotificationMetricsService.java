package com.focushive.notification.monitoring;

import com.focushive.notification.entity.NotificationType;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Service for tracking custom business metrics related to notifications.
 * Provides metrics for notification delivery, processing times, and system health.
 */
@Service
@Slf4j
public class NotificationMetricsService {

    private final MeterRegistry meterRegistry;
    
    // Counters for notification operations
    private final Counter notificationsCreated;
    private final Counter notificationsSent;
    private final Counter notificationsFailed;
    private final Counter emailsSent;
    private final Counter emailsFailed;
    private final Counter digestsProcessed;
    private final Counter preferencesUpdated;
    private final Counter templatesRendered;
    private final Counter templatesRenderedFailed;
    private final Counter rateLimitViolations;
    private final Counter securityAuditEvents;
    
    // Timers for performance metrics
    private final Timer notificationProcessingTime;
    private final Timer emailDeliveryTime;
    private final Timer templateRenderingTime;
    private final Timer digestProcessingTime;
    private final Timer databaseQueryTime;
    private final Timer cacheOperationTime;
    
    // Gauges for current state metrics
    private final AtomicLong activeNotifications = new AtomicLong(0);
    private final AtomicLong pendingDigestNotifications = new AtomicLong(0);
    private final AtomicLong totalUsers = new AtomicLong(0);
    private final AtomicLong rateLimitedUsers = new AtomicLong(0);
    
    // Type-specific counters
    private final ConcurrentHashMap<NotificationType, Counter> notificationsByType = new ConcurrentHashMap<>();
    
    public NotificationMetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        
        // Initialize counters
        this.notificationsCreated = Counter.builder("notifications.created.total")
                .description("Total number of notifications created")
                .register(meterRegistry);
                
        this.notificationsSent = Counter.builder("notifications.sent.total")
                .description("Total number of notifications sent successfully")
                .register(meterRegistry);
                
        this.notificationsFailed = Counter.builder("notifications.failed.total")
                .description("Total number of failed notifications")
                .register(meterRegistry);
                
        this.emailsSent = Counter.builder("emails.sent.total")
                .description("Total number of emails sent successfully")
                .register(meterRegistry);
                
        this.emailsFailed = Counter.builder("emails.failed.total")
                .description("Total number of failed email deliveries")
                .register(meterRegistry);
                
        this.digestsProcessed = Counter.builder("digests.processed.total")
                .description("Total number of digest notifications processed")
                .register(meterRegistry);
                
        this.preferencesUpdated = Counter.builder("preferences.updated.total")
                .description("Total number of user preference updates")
                .register(meterRegistry);
                
        this.templatesRendered = Counter.builder("templates.rendered.total")
                .description("Total number of templates rendered successfully")
                .register(meterRegistry);
                
        this.templatesRenderedFailed = Counter.builder("templates.rendered.failed.total")
                .description("Total number of template rendering failures")
                .register(meterRegistry);
                
        this.rateLimitViolations = Counter.builder("rate_limit.violations.total")
                .description("Total number of rate limit violations")
                .register(meterRegistry);
                
        this.securityAuditEvents = Counter.builder("security.audit.events.total")
                .description("Total number of security audit events")
                .register(meterRegistry);
        
        // Initialize timers
        this.notificationProcessingTime = Timer.builder("notifications.processing.time")
                .description("Time taken to process notifications")
                .register(meterRegistry);
                
        this.emailDeliveryTime = Timer.builder("emails.delivery.time")
                .description("Time taken to deliver emails")
                .register(meterRegistry);
                
        this.templateRenderingTime = Timer.builder("templates.rendering.time")
                .description("Time taken to render templates")
                .register(meterRegistry);
                
        this.digestProcessingTime = Timer.builder("digests.processing.time")
                .description("Time taken to process digest notifications")
                .register(meterRegistry);
                
        this.databaseQueryTime = Timer.builder("database.query.time")
                .description("Time taken for database queries")
                .register(meterRegistry);
                
        this.cacheOperationTime = Timer.builder("cache.operation.time")
                .description("Time taken for cache operations")
                .register(meterRegistry);
        
        // Initialize gauges
        Gauge.builder("notifications.active.count", activeNotifications, AtomicLong::doubleValue)
                .description("Current number of active notifications")
                .register(meterRegistry);
                
        Gauge.builder("notifications.digest.pending.count", pendingDigestNotifications, AtomicLong::doubleValue)
                .description("Current number of pending digest notifications")
                .register(meterRegistry);
                
        Gauge.builder("users.total.count", totalUsers, AtomicLong::doubleValue)
                .description("Total number of users in the system")
                .register(meterRegistry);
                
        Gauge.builder("users.rate_limited.count", rateLimitedUsers, AtomicLong::doubleValue)
                .description("Current number of rate-limited users")
                .register(meterRegistry);
                
        log.info("NotificationMetricsService initialized with {} metrics", meterRegistry.getMeters().size());
    }
    
    // Counter increment methods
    public void incrementNotificationsCreated(NotificationType type) {
        notificationsCreated.increment();
        getNotificationCounterByType(type).increment();
        log.debug("Incremented notifications created counter for type: {}", type);
    }
    
    public void incrementNotificationsSent() {
        notificationsSent.increment();
    }
    
    public void incrementNotificationsFailed() {
        notificationsFailed.increment();
    }
    
    public void incrementEmailsSent() {
        emailsSent.increment();
    }
    
    public void incrementEmailsFailed() {
        emailsFailed.increment();
    }
    
    public void incrementDigestsProcessed() {
        digestsProcessed.increment();
    }
    
    public void incrementPreferencesUpdated() {
        preferencesUpdated.increment();
    }
    
    public void incrementTemplatesRendered() {
        templatesRendered.increment();
    }
    
    public void incrementTemplatesRenderedFailed() {
        templatesRenderedFailed.increment();
    }
    
    public void incrementRateLimitViolations() {
        rateLimitViolations.increment();
    }
    
    public void incrementSecurityAuditEvents() {
        securityAuditEvents.increment();
    }
    
    // Timer recording methods
    public Timer.Sample startNotificationProcessingTimer() {
        return Timer.start(meterRegistry);
    }
    
    public void recordNotificationProcessingTime(Timer.Sample sample) {
        sample.stop(notificationProcessingTime);
    }
    
    public Timer.Sample startEmailDeliveryTimer() {
        return Timer.start(meterRegistry);
    }
    
    public void recordEmailDeliveryTime(Timer.Sample sample) {
        sample.stop(emailDeliveryTime);
    }
    
    public Timer.Sample startTemplateRenderingTimer() {
        return Timer.start(meterRegistry);
    }
    
    public void recordTemplateRenderingTime(Timer.Sample sample) {
        sample.stop(templateRenderingTime);
    }
    
    public Timer.Sample startDigestProcessingTimer() {
        return Timer.start(meterRegistry);
    }
    
    public void recordDigestProcessingTime(Timer.Sample sample) {
        sample.stop(digestProcessingTime);
    }
    
    public Timer.Sample startDatabaseQueryTimer() {
        return Timer.start(meterRegistry);
    }
    
    public void recordDatabaseQueryTime(Timer.Sample sample) {
        sample.stop(databaseQueryTime);
    }
    
    public Timer.Sample startCacheOperationTimer() {
        return Timer.start(meterRegistry);
    }
    
    public void recordCacheOperationTime(Timer.Sample sample) {
        sample.stop(cacheOperationTime);
    }
    
    // Gauge update methods
    public void updateActiveNotificationsCount(long count) {
        activeNotifications.set(count);
    }
    
    public void updatePendingDigestNotificationsCount(long count) {
        pendingDigestNotifications.set(count);
    }
    
    public void updateTotalUsersCount(long count) {
        totalUsers.set(count);
    }
    
    public void updateRateLimitedUsersCount(long count) {
        rateLimitedUsers.set(count);
    }
    
    // Helper methods
    private Counter getNotificationCounterByType(NotificationType type) {
        return notificationsByType.computeIfAbsent(type, t -> 
            Counter.builder("notifications.created.by_type")
                    .tag("type", t.name())
                    .description("Number of notifications created by type")
                    .register(meterRegistry));
    }
    
    /**
     * Get current metrics summary for health checks and monitoring.
     */
    public MetricsSummary getMetricsSummary() {
        return MetricsSummary.builder()
                .notificationsCreated((long) notificationsCreated.count())
                .notificationsSent((long) notificationsSent.count())
                .notificationsFailed((long) notificationsFailed.count())
                .emailsSent((long) emailsSent.count())
                .emailsFailed((long) emailsFailed.count())
                .digestsProcessed((long) digestsProcessed.count())
                .templatesRendered((long) templatesRendered.count())
                .activeNotifications(activeNotifications.get())
                .pendingDigestNotifications(pendingDigestNotifications.get())
                .totalUsers(totalUsers.get())
                .rateLimitViolations((long) rateLimitViolations.count())
                .build();
    }
    
    /**
     * Data class for metrics summary.
     */
    public static class MetricsSummary {
        public final long notificationsCreated;
        public final long notificationsSent;
        public final long notificationsFailed;
        public final long emailsSent;
        public final long emailsFailed;
        public final long digestsProcessed;
        public final long templatesRendered;
        public final long activeNotifications;
        public final long pendingDigestNotifications;
        public final long totalUsers;
        public final long rateLimitViolations;
        
        private MetricsSummary(long notificationsCreated, long notificationsSent, long notificationsFailed,
                              long emailsSent, long emailsFailed, long digestsProcessed, long templatesRendered,
                              long activeNotifications, long pendingDigestNotifications, long totalUsers, 
                              long rateLimitViolations) {
            this.notificationsCreated = notificationsCreated;
            this.notificationsSent = notificationsSent;
            this.notificationsFailed = notificationsFailed;
            this.emailsSent = emailsSent;
            this.emailsFailed = emailsFailed;
            this.digestsProcessed = digestsProcessed;
            this.templatesRendered = templatesRendered;
            this.activeNotifications = activeNotifications;
            this.pendingDigestNotifications = pendingDigestNotifications;
            this.totalUsers = totalUsers;
            this.rateLimitViolations = rateLimitViolations;
        }
        
        public static Builder builder() {
            return new Builder();
        }
        
        public static class Builder {
            private long notificationsCreated;
            private long notificationsSent;
            private long notificationsFailed;
            private long emailsSent;
            private long emailsFailed;
            private long digestsProcessed;
            private long templatesRendered;
            private long activeNotifications;
            private long pendingDigestNotifications;
            private long totalUsers;
            private long rateLimitViolations;
            
            public Builder notificationsCreated(long notificationsCreated) {
                this.notificationsCreated = notificationsCreated;
                return this;
            }
            
            public Builder notificationsSent(long notificationsSent) {
                this.notificationsSent = notificationsSent;
                return this;
            }
            
            public Builder notificationsFailed(long notificationsFailed) {
                this.notificationsFailed = notificationsFailed;
                return this;
            }
            
            public Builder emailsSent(long emailsSent) {
                this.emailsSent = emailsSent;
                return this;
            }
            
            public Builder emailsFailed(long emailsFailed) {
                this.emailsFailed = emailsFailed;
                return this;
            }
            
            public Builder digestsProcessed(long digestsProcessed) {
                this.digestsProcessed = digestsProcessed;
                return this;
            }
            
            public Builder templatesRendered(long templatesRendered) {
                this.templatesRendered = templatesRendered;
                return this;
            }
            
            public Builder activeNotifications(long activeNotifications) {
                this.activeNotifications = activeNotifications;
                return this;
            }
            
            public Builder pendingDigestNotifications(long pendingDigestNotifications) {
                this.pendingDigestNotifications = pendingDigestNotifications;
                return this;
            }
            
            public Builder totalUsers(long totalUsers) {
                this.totalUsers = totalUsers;
                return this;
            }
            
            public Builder rateLimitViolations(long rateLimitViolations) {
                this.rateLimitViolations = rateLimitViolations;
                return this;
            }
            
            public MetricsSummary build() {
                return new MetricsSummary(notificationsCreated, notificationsSent, notificationsFailed,
                        emailsSent, emailsFailed, digestsProcessed, templatesRendered,
                        activeNotifications, pendingDigestNotifications, totalUsers, rateLimitViolations);
            }
        }
    }
}