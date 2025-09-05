package com.focushive.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Service for managing custom business metrics in FocusHive.
 * 
 * This service provides:
 * - Focus session metrics (started, completed, duration)
 * - Hive metrics (created, member count, activity)
 * - User engagement metrics
 * - Performance metrics for critical operations
 * - Error tracking and monitoring
 * - WebSocket connection metrics
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MetricsService {

    private final MeterRegistry meterRegistry;

    // Gauges for current state
    private final AtomicInteger activeSessionsGauge = new AtomicInteger(0);
    private final AtomicInteger activeHiveMembers = new AtomicInteger(0);
    private final AtomicInteger activeWebSocketConnections = new AtomicInteger(0);
    private final AtomicLong cacheHitRate = new AtomicLong(0);
    private final AtomicInteger queueSize = new AtomicInteger(0);

    @PostConstruct
    public void initializeMetrics() {
        log.info("Initializing FocusHive business metrics");

        // Initialize gauges that track current state
        Gauge.builder("focushive.sessions.active", activeSessionsGauge, AtomicInteger::get)
                .description("Number of currently active focus sessions")
                .register(meterRegistry);

        Gauge.builder("focushive.hives.members.total", activeHiveMembers, AtomicInteger::get)
                .description("Total number of hive members across all hives")
                .register(meterRegistry);

        Gauge.builder("focushive.websocket.connections.active", activeWebSocketConnections, AtomicInteger::get)
                .description("Number of active WebSocket connections")
                .register(meterRegistry);

        Gauge.builder("focushive.cache.hit.rate", cacheHitRate, value -> value.get() / 100.0)
                .description("Cache hit rate percentage")
                .register(meterRegistry);

        Gauge.builder("focushive.queue.size", queueSize, AtomicInteger::get)
                .description("Number of items in processing queue")
                .register(meterRegistry);

        log.info("Successfully initialized {} business metrics gauges", 5);
    }

    // Session metrics methods
    public void recordSessionStarted(String sessionType, String hiveId) {
        Counter.builder("focushive.sessions.started.total")
                .description("Total number of focus sessions started")
                .tag("type", sessionType)
                .tag("hive_id", hiveId != null ? hiveId : "individual")
                .register(meterRegistry)
                .increment();
        
        activeSessionsGauge.incrementAndGet();
        
        log.debug("Recorded session started: type={}, hive={}", sessionType, hiveId);
    }

    public void recordSessionCompleted(String sessionType, Duration duration, String hiveId) {
        Counter.builder("focushive.sessions.completed.total")
                .description("Total number of focus sessions completed")
                .tag("type", sessionType)
                .tag("hive_id", hiveId != null ? hiveId : "individual")
                .tag("status", "completed")
                .register(meterRegistry)
                .increment();
        
        Timer.builder("focushive.session.duration")
                .description("Duration of focus sessions")
                .tag("type", sessionType)
                .tag("hive_id", hiveId != null ? hiveId : "individual")
                .register(meterRegistry)
                .record(duration);
        
        activeSessionsGauge.decrementAndGet();
        
        log.debug("Recorded session completed: type={}, duration={}, hive={}", 
                sessionType, duration, hiveId);
    }

    public void recordSessionCancelled(String sessionType, Duration partialDuration, String hiveId) {
        Counter.builder("focushive.sessions.completed.total")
                .description("Total number of focus sessions completed")
                .tag("type", sessionType)
                .tag("hive_id", hiveId != null ? hiveId : "individual")
                .tag("status", "cancelled")
                .register(meterRegistry)
                .increment();
        
        if (!partialDuration.isZero()) {
            Timer.builder("focushive.session.duration")
                    .description("Duration of focus sessions")
                    .tag("type", sessionType)
                    .tag("hive_id", hiveId != null ? hiveId : "individual")
                    .tag("status", "cancelled")
                    .register(meterRegistry)
                    .record(partialDuration);
        }
        
        activeSessionsGauge.decrementAndGet();
        
        log.debug("Recorded session cancelled: type={}, partialDuration={}, hive={}", 
                sessionType, partialDuration, hiveId);
    }

    // Hive metrics methods
    public void recordHiveCreated(String hiveType, int initialMemberCount) {
        Counter.builder("focushive.hives.created.total")
                .description("Total number of hives created")
                .tag("type", hiveType)
                .register(meterRegistry)
                .increment();
        
        activeHiveMembers.addAndGet(initialMemberCount);
        
        log.debug("Recorded hive created: type={}, initialMembers={}", hiveType, initialMemberCount);
    }

    public void recordHiveMemberJoined(String hiveId) {
        Counter.builder("focushive.hive.members.joined.total")
                .description("Total number of hive member joins")
                .tag("hive_id", hiveId)
                .register(meterRegistry)
                .increment();
        
        activeHiveMembers.incrementAndGet();
        log.debug("Recorded hive member joined: hive={}", hiveId);
    }

    public void recordHiveMemberLeft(String hiveId) {
        Counter.builder("focushive.hive.members.left.total")
                .description("Total number of hive member departures")
                .tag("hive_id", hiveId)
                .register(meterRegistry)
                .increment();
        
        activeHiveMembers.decrementAndGet();
        log.debug("Recorded hive member left: hive={}", hiveId);
    }

    // Communication metrics methods
    public void recordMessageSent(String messageType, String channel) {
        Counter.builder("focushive.messages.sent.total")
                .description("Total chat messages sent")
                .tag("type", messageType)
                .tag("channel", channel)
                .register(meterRegistry)
                .increment();
        
        log.debug("Recorded message sent: type={}, channel={}", messageType, channel);
    }

    // User metrics methods
    public void recordUserRegistered(String registrationMethod) {
        Counter.builder("focushive.users.registered.total")
                .description("Total number of users registered")
                .tag("method", registrationMethod)
                .register(meterRegistry)
                .increment();
        
        log.debug("Recorded user registered: method={}", registrationMethod);
    }

    // Performance metrics methods
    public void recordDatabaseQuery(Duration duration, String operation, boolean success) {
        Timer.builder("focushive.database.query.duration")
                .description("Database query execution time")
                .tag("operation", operation)
                .tag("status", success ? "success" : "error")
                .register(meterRegistry)
                .record(duration);
        
        if (!success) {
            recordError("database", operation, null);
        }
        
        log.debug("Recorded database query: operation={}, duration={}, success={}", 
                operation, duration, success);
    }

    public void recordIdentityServiceCall(Duration duration, String operation, boolean success) {
        Timer.builder("focushive.identity.service.call.duration")
                .description("Identity service call duration")
                .tag("operation", operation)
                .tag("status", success ? "success" : "error")
                .register(meterRegistry)
                .record(duration);
        
        if (!success) {
            recordError("identity-service", operation, null);
        }
        
        log.debug("Recorded identity service call: operation={}, duration={}, success={}", 
                operation, duration, success);
    }

    public void recordHiveOperation(Duration duration, String operation, boolean success) {
        Timer.builder("focushive.hive.operation.duration")
                .description("Hive operation processing time")
                .tag("operation", operation)
                .tag("status", success ? "success" : "error")
                .register(meterRegistry)
                .record(duration);
        
        if (!success) {
            recordError("hive-operation", operation, null);
        }
        
        log.debug("Recorded hive operation: operation={}, duration={}, success={}", 
                operation, duration, success);
    }

    // Error tracking methods
    public void recordError(String component, String operation, String errorType) {
        Counter.builder("focushive.errors.total")
                .description("Total number of errors encountered")
                .tag("component", component)
                .tag("operation", operation != null ? operation : "unknown")
                .tag("type", errorType != null ? errorType : "unknown")
                .register(meterRegistry)
                .increment();
        
        log.debug("Recorded error: component={}, operation={}, type={}", 
                component, operation, errorType);
    }

    // WebSocket metrics methods
    public void recordWebSocketConnection() {
        Counter.builder("focushive.websocket.connections.opened.total")
                .description("Total WebSocket connections opened")
                .register(meterRegistry)
                .increment();
        
        activeWebSocketConnections.incrementAndGet();
        log.debug("Recorded WebSocket connection - total active: {}", activeWebSocketConnections.get());
    }

    public void recordWebSocketDisconnection() {
        Counter.builder("focushive.websocket.connections.closed.total")
                .description("Total WebSocket connections closed")
                .register(meterRegistry)
                .increment();
        
        activeWebSocketConnections.decrementAndGet();
        log.debug("Recorded WebSocket disconnection - total active: {}", activeWebSocketConnections.get());
    }

    // Cache metrics methods
    public void recordCacheHit() {
        Counter.builder("focushive.cache.requests.total")
                .description("Total cache requests")
                .tag("result", "hit")
                .register(meterRegistry)
                .increment();
        
        // Update cache hit rate (simplified calculation)
        long currentRate = cacheHitRate.get();
        if (currentRate < 10000) { // Max 100.00%
            cacheHitRate.incrementAndGet();
        }
    }

    public void recordCacheMiss() {
        Counter.builder("focushive.cache.requests.total")
                .description("Total cache requests")
                .tag("result", "miss")
                .register(meterRegistry)
                .increment();
        
        // Decrease cache hit rate on miss
        long currentRate = cacheHitRate.get();
        if (currentRate > 0) {
            cacheHitRate.decrementAndGet();
        }
    }

    // Queue metrics methods
    public void updateQueueSize(int newSize) {
        queueSize.set(newSize);
        log.debug("Updated queue size to: {}", newSize);
    }

    // Getter methods for current values (useful for health checks)
    public int getActiveSessionsCount() {
        return activeSessionsGauge.get();
    }

    public int getActiveHiveMembersCount() {
        return activeHiveMembers.get();
    }

    public int getActiveWebSocketConnectionsCount() {
        return activeWebSocketConnections.get();
    }

    public double getCacheHitRatePercentage() {
        return cacheHitRate.get() / 100.0;
    }

    public int getCurrentQueueSize() {
        return queueSize.get();
    }
}