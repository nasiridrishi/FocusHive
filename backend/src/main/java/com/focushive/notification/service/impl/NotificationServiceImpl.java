package com.focushive.notification.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.focushive.notification.dto.CreateNotificationRequest;
import com.focushive.notification.dto.NotificationDto;
import com.focushive.notification.dto.NotificationResponse;
import com.focushive.notification.entity.NotificationFrequency;
import com.focushive.notification.entity.NotificationPreference;
import com.focushive.notification.repository.NotificationPreferenceRepository;
import com.focushive.notification.service.NotificationService;
import com.focushive.notification.service.delivery.NotificationDeliveryService;
import com.focushive.user.entity.Notification;
import com.focushive.user.entity.User;
import com.focushive.user.repository.NotificationRepository;
import com.focushive.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Implementation of the notification service.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationPreferenceRepository notificationPreferenceRepository;
    private final NotificationDeliveryService notificationDeliveryService;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public NotificationDto createNotification(CreateNotificationRequest request) {
        return createNotification(request, LocalTime.now());
    }

    @Override
    @Transactional
    public NotificationDto createNotification(CreateNotificationRequest request, LocalTime currentTime) {
        log.debug("Creating notification for user {} of type {}", request.getUserId(), request.getType());

        // Validate user exists
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + request.getUserId()));

        // Get user preferences for this notification type
        NotificationPreference preferences = notificationPreferenceRepository
                .findByUserIdAndNotificationType(request.getUserId(), request.getType())
                .orElse(createDefaultPreferences(request.getUserId(), request.getType()));

        // Create and save the notification
        Notification notification = createNotificationEntity(request, user);
        Notification savedNotification = notificationRepository.save(notification);

        // Convert to DTO
        NotificationDto notificationDto = NotificationDto.fromEntity(savedNotification);

        // Check if we should deliver now (not in quiet hours or force delivery)
        boolean shouldDeliver = request.getForceDelivery() || 
                               !preferences.isInQuietHours(currentTime);

        if (shouldDeliver && preferences.isNotificationEnabled()) {
            try {
                notificationDeliveryService.deliverNotification(notificationDto, preferences);
                log.debug("Notification {} delivered successfully", savedNotification.getId());
            } catch (Exception e) {
                log.error("Failed to deliver notification {}: {}", savedNotification.getId(), e.getMessage(), e);
                // Don't fail the whole operation if delivery fails
            }
        } else {
            log.debug("Notification {} not delivered: quiet hours={}, enabled={}", 
                     savedNotification.getId(), 
                     preferences.isInQuietHours(currentTime),
                     preferences.isNotificationEnabled());
        }

        return notificationDto;
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationResponse getNotifications(String userId, Pageable pageable) {
        log.debug("Getting notifications for user {} with page {}", userId, pageable.getPageNumber());
        
        Page<Notification> notificationPage = notificationRepository
                .findByUserIdOrderByCreatedAtDesc(userId, pageable);

        return createNotificationResponse(notificationPage);
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationResponse getUnreadNotifications(String userId, Pageable pageable) {
        log.debug("Getting unread notifications for user {} with page {}", userId, pageable.getPageNumber());
        
        Page<Notification> notificationPage = notificationRepository
                .findUnreadByUserId(userId, pageable);

        return createNotificationResponse(notificationPage);
    }

    @Override
    @Transactional
    public void markAsRead(String notificationId, String userId) {
        log.debug("Marking notification {} as read for user {}", notificationId, userId);
        
        // Verify notification exists and belongs to user
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found: " + notificationId));

        if (!notification.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Notification does not belong to user: " + userId);
        }

        notificationRepository.markAsRead(notificationId, userId, LocalDateTime.now());
    }

    @Override
    @Transactional
    public int markAllAsRead(String userId) {
        log.debug("Marking all notifications as read for user {}", userId);
        
        return notificationRepository.markAllAsRead(userId, LocalDateTime.now());
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadCount(String userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    @Override
    @Transactional
    public void deleteNotification(String notificationId, String userId) {
        log.debug("Deleting notification {} for user {}", notificationId, userId);
        
        // Verify notification exists and belongs to user
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found: " + notificationId));

        if (!notification.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Notification does not belong to user: " + userId);
        }

        notificationRepository.deleteById(notificationId);
    }

    @Override
    @Transactional
    public void archiveNotification(String notificationId, String userId) {
        log.debug("Archiving notification {} for user {}", notificationId, userId);
        
        // Verify notification exists and belongs to user
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found: " + notificationId));

        if (!notification.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Notification does not belong to user: " + userId);
        }

        notificationRepository.markAsArchived(notificationId, userId);
    }

    @Override
    @Transactional
    public int cleanupOldNotifications(String userId, int daysToKeep) {
        log.debug("Cleaning up old notifications for user {}, keeping {} days", userId, daysToKeep);
        
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysToKeep);
        return notificationRepository.deleteOldReadNotifications(userId, cutoffDate);
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationResponse getNotificationsByType(String userId, String type, Pageable pageable) {
        log.debug("Getting notifications of type {} for user {} with page {}", type, userId, pageable.getPageNumber());
        
        Page<Notification> notificationPage = notificationRepository
                .findByUserIdAndTypeOrderByCreatedAtDesc(userId, type, pageable);

        return createNotificationResponse(notificationPage);
    }

    /**
     * Create a notification entity from the request.
     */
    private Notification createNotificationEntity(CreateNotificationRequest request, User user) {
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setType(request.getType().name());
        notification.setTitle(request.getTitle());
        notification.setContent(request.getContent());
        notification.setActionUrl(request.getActionUrl());
        notification.setPriority(request.getPriority());
        notification.setData(serializeData(request.getData()));
        
        return notification;
    }

    /**
     * Create default notification preferences for a user and type.
     */
    private NotificationPreference createDefaultPreferences(String userId, com.focushive.notification.entity.NotificationType type) {
        return NotificationPreference.builder()
                .userId(userId)
                .notificationType(type)
                .inAppEnabled(true)
                .emailEnabled(true)
                .pushEnabled(true)
                .frequency(NotificationFrequency.IMMEDIATE)
                .build();
    }

    /**
     * Serialize data map to JSON string.
     */
    private String serializeData(Map<String, Object> data) {
        if (data == null || data.isEmpty()) {
            return "{}";
        }
        
        try {
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize notification data: {}", e.getMessage());
            return "{}";
        }
    }

    /**
     * Create a notification response from a page of notifications.
     */
    private NotificationResponse createNotificationResponse(Page<Notification> notificationPage) {
        return NotificationResponse.builder()
                .notifications(notificationPage.getContent().stream()
                        .map(NotificationDto::fromEntity)
                        .collect(Collectors.toList()))
                .page(notificationPage.getNumber())
                .size(notificationPage.getSize())
                .totalElements(notificationPage.getTotalElements())
                .totalPages(notificationPage.getTotalPages())
                .first(notificationPage.isFirst())
                .last(notificationPage.isLast())
                .numberOfElements(notificationPage.getNumberOfElements())
                .empty(notificationPage.isEmpty())
                .build();
    }
}