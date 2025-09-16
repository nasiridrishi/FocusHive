package com.focushive.notification.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.focushive.notification.entity.Notification;
import com.focushive.notification.repository.NotificationRepository;
import com.focushive.notification.service.NotificationCleanupService;
import com.focushive.notification.monitoring.NotificationMetricsService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * Implementation of NotificationCleanupService providing comprehensive
 * notification data cleanup and archival functionality.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationCleanupServiceImpl implements NotificationCleanupService {

    private final NotificationRepository notificationRepository;
    private final NotificationMetricsService metricsService;
    private final MeterRegistry meterRegistry;
    private final ObjectMapper objectMapper;

    @Value("${notification.cleanup.retention-days:90}")
    private int retentionDays;

    @Value("${notification.cleanup.batch-size:1000}")
    private int batchSize;

    @Value("${notification.cleanup.enabled:true}")
    private boolean enableCleanup;

    private final AtomicBoolean cleanupInProgress = new AtomicBoolean(false);
    private volatile LocalDateTime lastCleanupTime;

    private Counter cleanupProcessedCounter;
    private Counter cleanupArchivedCounter;
    private Timer cleanupTimer;

    @PostConstruct
    public void initializeMetrics() {
        cleanupProcessedCounter = meterRegistry.counter("notification.cleanup.processed");
        cleanupArchivedCounter = meterRegistry.counter("notification.cleanup.archived");
        cleanupTimer = meterRegistry.timer("notification.cleanup.duration");
    }

    @Override
    @Transactional
    public CleanupResult cleanupOldNotifications() {
        LocalDateTime startTime = LocalDateTime.now();

        if (!enableCleanup) {
            log.warn("Notification cleanup is disabled");
            return CleanupResult.failure("Cleanup is disabled in configuration");
        }

        log.info("Starting notification cleanup process. Retention days: {}, Batch size: {}", retentionDays, batchSize);

        Timer.Sample sample = Timer.start(meterRegistry);
        int totalProcessed = 0;
        int totalArchived = 0;

        try {
            LocalDateTime cutoffDate = getRetentionCutoffDate();
            log.debug("Cleanup cutoff date: {}", cutoffDate);

            int page = 0;
            Page<Notification> notificationsPage;

            do {
                notificationsPage = notificationRepository.findOldNotificationsForCleanup(
                    cutoffDate, PageRequest.of(page, batchSize));
                
                List<Notification> notifications = notificationsPage.getContent();
                
                if (!notifications.isEmpty()) {
                    log.debug("Processing batch {} with {} notifications", page + 1, notifications.size());
                    
                    // Archive notifications
                    notifications.forEach(this::archiveNotification);
                    
                    // Save batch
                    notificationRepository.saveAll(notifications);
                    
                    totalProcessed += notifications.size();
                    totalArchived += notifications.size();
                    
                    // Update metrics
                    cleanupProcessedCounter.increment(notifications.size());
                    cleanupArchivedCounter.increment(notifications.size());
                    
                    log.debug("Archived {} notifications in batch {}", notifications.size(), page + 1);
                }
                
                page++;
            } while (notificationsPage.hasNext());

            LocalDateTime endTime = LocalDateTime.now();
            lastCleanupTime = endTime;

            log.info("Cleanup completed successfully. Processed: {}, Archived: {}, Duration: {}ms", 
                totalProcessed, totalArchived, java.time.Duration.between(startTime, endTime).toMillis());

            return CleanupResult.success(totalProcessed, totalArchived, 0, startTime, endTime);

        } catch (Exception e) {
            log.error("Error during notification cleanup", e);
            return CleanupResult.failure("Cleanup failed: " + e.getMessage());
        } finally {
            sample.stop(cleanupTimer);
        }
    }

    @Override
    @Transactional
    public CleanupResult cleanupUserNotifications(String userId) {
        if (!StringUtils.hasText(userId)) {
            throw new IllegalArgumentException("User ID cannot be null or empty");
        }

        LocalDateTime startTime = LocalDateTime.now();

        if (!enableCleanup) {
            return CleanupResult.failure("Cleanup is disabled in configuration");
        }

        log.info("Starting user notification cleanup for user: {}", userId);

        Timer.Sample sample = Timer.start(meterRegistry);
        int totalProcessed = 0;
        int totalArchived = 0;

        try {
            LocalDateTime cutoffDate = getRetentionCutoffDate();
            
            int page = 0;
            Page<Notification> notificationsPage;

            do {
                notificationsPage = notificationRepository.findOldNotificationsByUserForCleanup(
                    userId, cutoffDate, PageRequest.of(page, batchSize));
                
                List<Notification> notifications = notificationsPage.getContent();
                
                if (!notifications.isEmpty()) {
                    // Archive notifications
                    notifications.forEach(this::archiveNotification);
                    
                    // Save batch
                    notificationRepository.saveAll(notifications);
                    
                    totalProcessed += notifications.size();
                    totalArchived += notifications.size();
                    
                    // Update metrics
                    cleanupProcessedCounter.increment(notifications.size());
                    cleanupArchivedCounter.increment(notifications.size());
                }
                
                page++;
            } while (notificationsPage.hasNext());

            LocalDateTime endTime = LocalDateTime.now();

            log.info("User cleanup completed for user: {}. Processed: {}, Archived: {}", 
                userId, totalProcessed, totalArchived);

            return CleanupResult.success(totalProcessed, totalArchived, 0, startTime, endTime);

        } catch (Exception e) {
            log.error("Error during user notification cleanup for user: {}", userId, e);
            return CleanupResult.failure("User cleanup failed: " + e.getMessage());
        } finally {
            sample.stop(cleanupTimer);
        }
    }

    @Override
    @Async("taskExecutor")
    public CompletableFuture<CleanupResult> runAsyncCleanup() {
        if (!cleanupInProgress.compareAndSet(false, true)) {
            log.warn("Cleanup is already running, skipping this request");
            return CompletableFuture.completedFuture(
                CleanupResult.failure("Cleanup is already running"));
        }

        try {
            CleanupResult result = cleanupOldNotifications();
            return CompletableFuture.completedFuture(result);
        } finally {
            cleanupInProgress.set(false);
        }
    }

    @Override
    public CleanupStatistics getCleanupStatistics() {
        try {
            LocalDateTime cutoffDate = getRetentionCutoffDate();
            
            long eligibleForCleanup = notificationRepository.countNotificationsOlderThan(cutoffDate);
            long totalArchived = notificationRepository.countArchivedNotifications();
            long totalDeleted = notificationRepository.countDeletedNotifications();
            
            CleanupStatistics stats = new CleanupStatistics();
            stats.setEligibleForCleanup(eligibleForCleanup);
            stats.setTotalArchived(totalArchived);
            stats.setTotalDeleted(totalDeleted);
            stats.setLastCleanupTime(lastCleanupTime);
            stats.setRetentionDays(retentionDays);
            stats.setCleanupEnabled(enableCleanup);
            stats.setStatus(cleanupInProgress.get() ? "RUNNING" : "IDLE");
            
            return stats;
        } catch (Exception e) {
            log.error("Error getting cleanup statistics", e);
            CleanupStatistics errorStats = new CleanupStatistics();
            errorStats.setStatus("ERROR: " + e.getMessage());
            return errorStats;
        }
    }

    @Override
    public ExportResult exportArchivedData() {
        try {
            log.info("Starting archived data export");
            
            List<Notification> archivedNotifications = notificationRepository.findArchivedNotifications(
                PageRequest.of(0, 10000)).getContent(); // Limit for safety
            
            if (archivedNotifications.isEmpty()) {
                return ExportResult.success(0, "[]", "JSON");
            }

            // Convert to export format
            List<NotificationExportDto> exportData = archivedNotifications.stream()
                .map(this::toExportDto)
                .collect(Collectors.toList());
            
            String jsonData = objectMapper.writeValueAsString(exportData);
            
            log.info("Exported {} archived notifications", exportData.size());
            
            return ExportResult.success(exportData.size(), jsonData, "JSON");
            
        } catch (Exception e) {
            log.error("Error exporting archived data", e);
            return ExportResult.failure("Export failed: " + e.getMessage());
        }
    }

    /**
     * Scheduled cleanup job that runs daily at 2 AM.
     */
    @Scheduled(cron = "${notification.cleanup.schedule:0 0 2 * * *}")
    public void scheduledCleanup() {
        log.info("Running scheduled notification cleanup");
        
        if (cleanupInProgress.get()) {
            log.warn("Cleanup already in progress, skipping scheduled run");
            return;
        }
        
        CleanupResult result = cleanupOldNotifications();
        
        if (result.isSuccess()) {
            log.info("Scheduled cleanup completed successfully. Processed: {}, Archived: {}", 
                result.getProcessedCount(), result.getArchivedCount());
        } else {
            log.error("Scheduled cleanup failed: {}", result.getErrorMessage());
        }
    }

    private LocalDateTime getRetentionCutoffDate() {
        return LocalDateTime.now().minusDays(retentionDays);
    }

    private void archiveNotification(Notification notification) {
        notification.setDeletedAt(LocalDateTime.now());
        notification.archive();
    }

    private NotificationExportDto toExportDto(Notification notification) {
        NotificationExportDto dto = new NotificationExportDto();
        dto.setId(notification.getId());
        dto.setUserId(notification.getUserId());
        dto.setTitle(notification.getTitle());
        dto.setContent(notification.getContent());
        dto.setType(notification.getType().toString());
        dto.setPriority(notification.getPriority().toString());
        dto.setCreatedAt(notification.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        dto.setReadAt(notification.getReadAt() != null ? 
            notification.getReadAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null);
        dto.setArchivedAt(notification.getArchivedAt() != null ? 
            notification.getArchivedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null);
        dto.setDeletedAt(notification.getDeletedAt() != null ? 
            notification.getDeletedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null);
        return dto;
    }

    /**
     * DTO for exporting notification data.
     */
    private static class NotificationExportDto {
        private String id;
        private String userId;
        private String title;
        private String content;
        private String type;
        private String priority;
        private String createdAt;
        private String readAt;
        private String archivedAt;
        private String deletedAt;

        // Getters and setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getPriority() { return priority; }
        public void setPriority(String priority) { this.priority = priority; }
        public String getCreatedAt() { return createdAt; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
        public String getReadAt() { return readAt; }
        public void setReadAt(String readAt) { this.readAt = readAt; }
        public String getArchivedAt() { return archivedAt; }
        public void setArchivedAt(String archivedAt) { this.archivedAt = archivedAt; }
        public String getDeletedAt() { return deletedAt; }
        public void setDeletedAt(String deletedAt) { this.deletedAt = deletedAt; }
    }
}