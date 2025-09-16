package com.focushive.notification.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.focushive.notification.dto.CreateNotificationRequest;
import com.focushive.notification.dto.NotificationDto;
import com.focushive.notification.dto.NotificationResponse;
import com.focushive.notification.entity.Notification;
import com.focushive.notification.entity.NotificationFrequency;
import com.focushive.notification.entity.NotificationPreference;
import com.focushive.notification.entity.NotificationTemplate;
import com.focushive.notification.entity.NotificationType;
import com.focushive.notification.repository.NotificationRepository;
import com.focushive.notification.repository.NotificationTemplateRepository;
import com.focushive.notification.service.NotificationPreferenceService;
import com.focushive.notification.service.NotificationService;
import com.focushive.notification.service.NotificationStatistics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of NotificationService.
 * Provides comprehensive notification management functionality including
 * creation, retrieval, status updates, and cleanup operations.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationTemplateRepository templateRepository;
    private final NotificationPreferenceService preferenceService;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${notification.queue.exchange:focushive.notifications}")
    private String notificationExchange;

    @Value("${notification.queue.routing-key:notification.created}")
    private String notificationRoutingKey;

    @Override
    public NotificationDto createNotification(CreateNotificationRequest request) {
        // Validate request first
        validateCreateRequest(request);
        
        log.debug("Creating notification for user {} of type {}", request.getUserId(), request.getType());
        
        // Validate user preferences and check if notification should be sent
        if (!shouldSendNotification(request.getUserId(), request.getType(), request.getForceDelivery())) {
            log.info("Notification for user {} blocked by preferences or quiet hours", request.getUserId());
            throw new IllegalArgumentException("Notification blocked by user preferences");
        }
        
        // Get or create user preference
        NotificationPreference preference = getOrCreateUserPreference(request.getUserId(), request.getType());
        
        // Apply template if available
        NotificationTemplate template = findTemplate(request.getType(), request.getLanguage());
        
        // Build notification entity
        Notification notification = buildNotification(request, template);
        
        // Save notification
        notification = notificationRepository.save(notification);
        log.info("Created notification {} for user {}", notification.getId(), notification.getUserId());
        
        // Publish to message queue for async processing
        publishNotificationEvent(notification, preference);
        
        // Convert to DTO and return
        return NotificationDto.fromEntity(notification);
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationResponse getNotifications(String userId, Pageable pageable) {
        log.debug("Getting notifications for user {} - page: {}, size: {}", 
                 userId, pageable.getPageNumber(), pageable.getPageSize());
        
        validateUserId(userId);
        
        Page<Notification> notificationPage = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        
        return buildNotificationResponse(notificationPage);
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationResponse getUnreadNotifications(String userId, Pageable pageable) {
        log.debug("Getting unread notifications for user {} - page: {}, size: {}", 
                 userId, pageable.getPageNumber(), pageable.getPageSize());
        
        validateUserId(userId);
        
        Page<Notification> notificationPage = notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId, pageable);
        
        return buildNotificationResponse(notificationPage);
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationResponse getNotificationsByType(String userId, String type, Pageable pageable) {
        log.debug("Getting notifications of type {} for user {}", type, userId);
        
        validateUserId(userId);
        
        // Validate notification type
        NotificationType notificationType;
        try {
            notificationType = NotificationType.valueOf(type);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid notification type: " + type);
        }
        
        Page<Notification> notificationPage = notificationRepository.findByUserIdAndTypeOrderByCreatedAtDesc(
                userId, notificationType, pageable);
        
        return buildNotificationResponse(notificationPage);
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadCount(String userId) {
        log.debug("Getting unread count for user {}", userId);
        
        validateUserId(userId);
        
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    @Override
    public void markAsRead(String notificationId, String userId) {
        log.debug("Marking notification {} as read for user {}", notificationId, userId);
        
        validateUserId(userId);
        validateNotificationId(notificationId);
        
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found: " + notificationId));
        
        // Verify ownership
        if (!notification.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Notification does not belong to user: " + userId);
        }
        
        // Skip if already read
        if (notification.getIsRead()) {
            log.debug("Notification {} is already marked as read", notificationId);
            return;
        }
        
        // Mark as read
        notification.markAsRead();
        notificationRepository.save(notification);
        
        log.info("Marked notification {} as read for user {}", notificationId, userId);
    }

    @Override
    public int markAllAsRead(String userId) {
        log.debug("Marking all notifications as read for user {}", userId);
        
        validateUserId(userId);
        
        List<Notification> unreadNotifications = notificationRepository.findByUserIdAndIsReadFalse(userId);
        
        if (unreadNotifications.isEmpty()) {
            log.debug("No unread notifications found for user {}", userId);
            return 0;
        }
        
        // Mark all as read
        LocalDateTime now = LocalDateTime.now();
        unreadNotifications.forEach(notification -> {
            notification.setIsRead(true);
            notification.setReadAt(now);
        });
        
        notificationRepository.saveAll(unreadNotifications);
        
        int count = unreadNotifications.size();
        log.info("Marked {} notifications as read for user {}", count, userId);
        
        return count;
    }

    @Override
    public void deleteNotification(String notificationId, String userId) {
        log.debug("Deleting notification {} for user {}", notificationId, userId);
        
        validateUserId(userId);
        validateNotificationId(notificationId);
        
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found: " + notificationId));
        
        // Verify ownership
        if (!notification.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Notification does not belong to user: " + userId);
        }
        
        // Soft delete
        notification.setDeletedAt(LocalDateTime.now());
        notificationRepository.save(notification);
        
        log.info("Soft deleted notification {} for user {}", notificationId, userId);
    }

    @Override
    public void archiveNotification(String notificationId, String userId) {
        log.debug("Archiving notification {} for user {}", notificationId, userId);
        
        validateUserId(userId);
        validateNotificationId(notificationId);
        
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found: " + notificationId));
        
        // Verify ownership
        if (!notification.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Notification does not belong to user: " + userId);
        }
        
        // Skip if already archived
        if (notification.getIsArchived()) {
            log.debug("Notification {} is already archived", notificationId);
            return;
        }
        
        // Archive notification
        notification.archive();
        notificationRepository.save(notification);
        
        log.info("Archived notification {} for user {}", notificationId, userId);
    }

    @Override
    public int cleanupOldNotifications(String userId, int daysToKeep) {
        log.debug("Cleaning up notifications older than {} days for user {}", daysToKeep, userId);
        
        validateUserId(userId);
        
        if (daysToKeep <= 0) {
            throw new IllegalArgumentException("Days to keep must be positive");
        }
        
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysToKeep);
        
        List<Notification> oldNotifications = notificationRepository.findByUserIdAndIsReadTrueAndCreatedAtBefore(
                userId, cutoffDate);
        
        if (oldNotifications.isEmpty()) {
            log.debug("No old notifications to cleanup for user {}", userId);
            return 0;
        }
        
        // Soft delete old notifications
        LocalDateTime now = LocalDateTime.now();
        oldNotifications.forEach(notification -> notification.setDeletedAt(now));
        
        notificationRepository.saveAll(oldNotifications);
        
        int count = oldNotifications.size();
        log.info("Cleaned up {} old notifications for user {}", count, userId);
        
        return count;
    }

    // Helper methods

    private void validateCreateRequest(CreateNotificationRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Notification request cannot be null");
        }
        if (request.getUserId() == null || request.getUserId().trim().isEmpty()) {
            throw new IllegalArgumentException("User ID is required");
        }
        if (request.getType() == null) {
            throw new IllegalArgumentException("Notification type is required");
        }
        if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("Title is required");
        }
    }

    private void validateUserId(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be null or empty");
        }
    }

    private void validateNotificationId(String notificationId) {
        if (notificationId == null || notificationId.trim().isEmpty()) {
            throw new IllegalArgumentException("Notification ID cannot be null or empty");
        }
    }

    /**
     * Check if notification should be sent based on user preferences.
     *
     * @param userId the user ID
     * @param type the notification type
     * @param forceDelivery whether to force delivery ignoring preferences
     * @return true if notification should be sent, false otherwise
     */
    private boolean shouldSendNotification(String userId, NotificationType type, Boolean forceDelivery) {
        // Force delivery bypasses all preference checks
        if (Boolean.TRUE.equals(forceDelivery)) {
            log.debug("Force delivery requested - bypassing preference checks for user {}", userId);
            return true;
        }
        
        // Check if notifications are enabled for this type
        if (!preferenceService.isNotificationEnabled(userId, type)) {
            log.debug("Notifications disabled for user {} and type {}", userId, type);
            return false;
        }
        
        // Check quiet hours
        if (preferenceService.isInQuietHours(userId, type, LocalTime.now())) {
            log.debug("Current time is within quiet hours for user {} and type {}", userId, type);
            return false;
        }
        
        return true;
    }
    
    /**
     * Get user preference or create default if not exists.
     *
     * @param userId the user ID
     * @param type the notification type
     * @return notification preference
     */
    private NotificationPreference getOrCreateUserPreference(String userId, NotificationType type) {
        return preferenceService.getUserPreference(userId, type)
                .orElseGet(() -> {
                    log.debug("Creating default preferences for new user: {}", userId);
                    preferenceService.createDefaultPreferencesForUser(userId);
                    return preferenceService.getUserPreference(userId, type)
                            .orElseThrow(() -> new IllegalStateException("Failed to create default preference"));
                });
    }

    private boolean isInQuietHours(NotificationPreference preference) {
        if (preference == null || preference.getQuietStartTime() == null || preference.getQuietEndTime() == null) {
            return false;
        }
        return preference.isInQuietHours(LocalTime.now());
    }

    private NotificationTemplate findTemplate(NotificationType type, String language) {
        return templateRepository.findByNotificationTypeAndLanguage(type, language)
                .orElse(null);
    }

    private Notification buildNotification(CreateNotificationRequest request, NotificationTemplate template) {
        String title = request.getTitle();
        String content = request.getContent();
        
        // Apply template if available
        if (template != null && request.getVariables() != null) {
            title = template.processTemplate(title, request.getVariables());
            if (content != null) {
                content = template.processTemplate(content, request.getVariables());
            } else if (template.getBodyText() != null) {
                content = template.getProcessedBodyText(request.getVariables());
            }
        }
        
        // Convert data map to JSON string
        String dataJson = "{}";
        if (request.getData() != null && !request.getData().isEmpty()) {
            try {
                dataJson = objectMapper.writeValueAsString(request.getData());
            } catch (JsonProcessingException e) {
                log.warn("Failed to serialize notification data: {}", e.getMessage());
            }
        }
        
        Notification notification = Notification.builder()
                .userId(request.getUserId())
                .type(request.getType())
                .title(title)
                .content(content)
                .actionUrl(request.getActionUrl())
                .priority(request.getPriority())
                .data(dataJson)
                .language(request.getLanguage())
                .isRead(false)
                .isArchived(false)
                .deliveryAttempts(0)
                .build();
        
        // Let JPA handle ID generation and timestamps via @PrePersist
        // Don't manually set ID, createdAt, updatedAt - these are handled by BaseEntity
        
        return notification;
    }

    private void publishNotificationEvent(Notification notification, NotificationPreference preference) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("notificationId", notification.getId());
            event.put("userId", notification.getUserId());
            event.put("type", notification.getType().name());
            event.put("inAppEnabled", preference.getInAppEnabled());
            event.put("emailEnabled", preference.getEmailEnabled());
            event.put("pushEnabled", preference.getPushEnabled());
            
            rabbitTemplate.convertAndSend(notificationExchange, notificationRoutingKey, event);
            log.debug("Published notification event for notification {}", notification.getId());
        } catch (Exception e) {
            log.error("Failed to publish notification event: {}", e.getMessage(), e);
            // Don't fail the notification creation if queue publishing fails
        }
    }

    private NotificationResponse buildNotificationResponse(Page<Notification> notificationPage) {
        List<NotificationDto> notificationDtos = notificationPage.getContent().stream()
                .map(NotificationDto::fromEntity)
                .collect(Collectors.toList());
        
        return NotificationResponse.builder()
                .notifications(notificationDtos)
                .totalElements(notificationPage.getTotalElements())
                .totalPages(notificationPage.getTotalPages())
                .page(notificationPage.getNumber())
                .size(notificationPage.getSize())
                .first(notificationPage.isFirst())
                .last(notificationPage.isLast())
                .numberOfElements(notificationPage.getNumberOfElements())
                .empty(notificationPage.isEmpty())
                .hasNext(notificationPage.hasNext())
                .hasPrevious(notificationPage.hasPrevious())
                .build();
    }

    @Override
    @Transactional
    public int bulkMarkAsRead(List<Long> notificationIds) {
        log.info("Bulk marking {} notifications as read", notificationIds.size());

        int successCount = 0;
        for (Long id : notificationIds) {
            try {
                Optional<Notification> notificationOpt = notificationRepository.findById(String.valueOf(id));
                if (notificationOpt.isPresent()) {
                    Notification notification = notificationOpt.get();
                    if (!notification.getIsRead()) {
                        notification.setIsRead(true);
                        notification.setReadAt(LocalDateTime.now());
                        notificationRepository.save(notification);
                        successCount++;
                    }
                }
            } catch (Exception e) {
                log.error("Error marking notification {} as read", id, e);
            }
        }

        return successCount;
    }

    @Override
    @Transactional
    public int bulkDelete(List<Long> notificationIds) {
        log.info("Bulk deleting {} notifications", notificationIds.size());

        int successCount = 0;
        for (Long id : notificationIds) {
            try {
                if (notificationRepository.existsById(String.valueOf(id))) {
                    notificationRepository.deleteById(String.valueOf(id));
                    successCount++;
                }
            } catch (Exception e) {
                log.error("Error deleting notification {}", id, e);
            }
        }

        return successCount;
    }

    @Override
    public NotificationStatistics getStatistics(String userId) {
        log.info("Getting notification statistics for user {}", userId);

        // Get basic counts
        long totalCount = notificationRepository.countByUserId(userId);
        long unreadCount = notificationRepository.countByUserIdAndIsRead(userId, false);
        long readCount = totalCount - unreadCount;

        // Get counts by type
        Map<String, Long> countByType = new HashMap<>();
        for (NotificationType type : NotificationType.values()) {
            long count = notificationRepository.countByUserIdAndType(userId, type);
            if (count > 0) {
                countByType.put(type.name(), count);
            }
        }

        // Get time-based counts
        LocalDateTime now = LocalDateTime.now();
        long last24HoursCount = notificationRepository.countByUserIdAndCreatedAtAfter(
            userId, now.minusHours(24)
        );
        long last7DaysCount = notificationRepository.countByUserIdAndCreatedAtAfter(
            userId, now.minusDays(7)
        );
        long last30DaysCount = notificationRepository.countByUserIdAndCreatedAtAfter(
            userId, now.minusDays(30)
        );

        return NotificationStatistics.builder()
            .totalCount(totalCount)
            .unreadCount(unreadCount)
            .readCount(readCount)
            .archivedCount(0L) // TODO: Implement archived notifications
            .countByType(countByType)
            .countByChannel(new HashMap<>()) // TODO: Implement channel statistics
            .last24HoursCount(last24HoursCount)
            .last7DaysCount(last7DaysCount)
            .last30DaysCount(last30DaysCount)
            .averageResponseTime(0.0) // TODO: Calculate average response time
            .build();
    }

}
