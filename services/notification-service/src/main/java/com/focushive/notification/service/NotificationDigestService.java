package com.focushive.notification.service;

import com.focushive.notification.entity.Notification;
import com.focushive.notification.entity.NotificationFrequency;
import com.focushive.notification.entity.NotificationPreference;
import com.focushive.notification.entity.NotificationType;
import com.focushive.notification.messaging.dto.NotificationMessage;
import com.focushive.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Collectors;

/**
 * Service for managing notification digests and batching.
 * Handles digest delivery based on user frequency preferences.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class NotificationDigestService {

    private final NotificationRepository notificationRepository;
    private final NotificationPreferenceService preferenceService;
    private final EmailNotificationService emailNotificationService;

    /**
     * Process daily digest notifications.
     * Runs every day at 8:00 AM.
     */
    @Scheduled(cron = "0 0 8 * * *")
    public void processDailyDigests() {
        log.info("Processing daily digest notifications");
        
        List<NotificationPreference> dailyPreferences = preferenceService.getDigestPreferences(NotificationFrequency.DAILY_DIGEST);
        
        int processedCount = 0;
        for (NotificationPreference preference : dailyPreferences) {
            try {
                // Check if user is in quiet hours
                if (preferenceService.isInQuietHours(preference.getUserId(), preference.getNotificationType(), LocalTime.now())) {
                    log.debug("Skipping daily digest for user {} - in quiet hours", preference.getUserId());
                    continue;
                }
                
                processDigestForUser(preference.getUserId(), NotificationFrequency.DAILY_DIGEST);
                processedCount++;
            } catch (Exception e) {
                log.error("Failed to process daily digest for user {}: {}", preference.getUserId(), e.getMessage());
            }
        }
        
        log.info("Processed daily digests for {} users", processedCount);
    }

    /**
     * Process weekly digest notifications.
     * Runs every Monday at 9:00 AM.
     */
    @Scheduled(cron = "0 0 9 * * MON")
    public void processWeeklyDigests() {
        log.info("Processing weekly digest notifications");
        
        List<NotificationPreference> weeklyPreferences = preferenceService.getDigestPreferences(NotificationFrequency.WEEKLY_DIGEST);
        
        int processedCount = 0;
        for (NotificationPreference preference : weeklyPreferences) {
            try {
                // Check if user is in quiet hours
                if (preferenceService.isInQuietHours(preference.getUserId(), preference.getNotificationType(), LocalTime.now())) {
                    log.debug("Skipping weekly digest for user {} - in quiet hours", preference.getUserId());
                    continue;
                }
                
                processDigestForUser(preference.getUserId(), NotificationFrequency.WEEKLY_DIGEST);
                processedCount++;
            } catch (Exception e) {
                log.error("Failed to process weekly digest for user {}: {}", preference.getUserId(), e.getMessage());
            }
        }
        
        log.info("Processed weekly digests for {} users", processedCount);
    }

    /**
     * Process digest notifications for a specific user.
     *
     * @param userId the user ID
     * @param frequency the digest frequency
     */
    public void processDigestForUser(String userId, NotificationFrequency frequency) {
        log.debug("Processing {} digest for user {}", frequency, userId);
        
        LocalDateTime cutoffTime = calculateCutoffTime(frequency);
        List<Notification> unreadNotifications = getUnreadNotificationsForDigest(userId, cutoffTime);
        
        if (unreadNotifications.isEmpty()) {
            log.debug("No unread notifications for {} digest for user {}", frequency, userId);
            return;
        }
        
        // Group notifications by type
        Map<NotificationType, List<Notification>> groupedNotifications = unreadNotifications.stream()
                .collect(Collectors.groupingBy(Notification::getType));
        
        // Send digest email
        sendDigestEmail(userId, frequency, groupedNotifications);
        
        // Mark notifications as processed in digest
        markNotificationsAsDigestProcessed(unreadNotifications);
        
        log.info("Processed {} digest for user {} with {} notifications", 
                frequency, userId, unreadNotifications.size());
    }

    /**
     * Get pending notifications that should be included in digest.
     *
     * @param userId the user ID
     * @param frequency the digest frequency
     * @return list of pending notifications
     */
    @Transactional(readOnly = true)
    public List<Notification> getPendingDigestNotifications(String userId, NotificationFrequency frequency) {
        LocalDateTime cutoffTime = calculateCutoffTime(frequency);
        return getUnreadNotificationsForDigest(userId, cutoffTime);
    }

    /**
     * Check if user has pending digest notifications.
     *
     * @param userId the user ID
     * @param frequency the digest frequency
     * @return true if user has pending digest notifications
     */
    @Transactional(readOnly = true)
    public boolean hasPendingDigestNotifications(String userId, NotificationFrequency frequency) {
        LocalDateTime cutoffTime = calculateCutoffTime(frequency);
        List<Notification> pending = getUnreadNotificationsForDigest(userId, cutoffTime);
        return !pending.isEmpty();
    }

    /**
     * Get digest summary for a user.
     *
     * @param userId the user ID
     * @param frequency the digest frequency
     * @return digest summary map
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getDigestSummary(String userId, NotificationFrequency frequency) {
        LocalDateTime cutoffTime = calculateCutoffTime(frequency);
        List<Notification> notifications = getUnreadNotificationsForDigest(userId, cutoffTime);
        
        Map<NotificationType, Long> typeCounts = notifications.stream()
                .collect(Collectors.groupingBy(Notification::getType, Collectors.counting()));
        
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalCount", notifications.size());
        summary.put("typeBreakdown", typeCounts);
        summary.put("frequency", frequency.name());
        summary.put("cutoffTime", cutoffTime);
        
        return summary;
    }

    // Private helper methods

    private LocalDateTime calculateCutoffTime(NotificationFrequency frequency) {
        LocalDateTime now = LocalDateTime.now();
        
        return switch (frequency) {
            case IMMEDIATE -> now.minusHours(1); // Fallback for immediate notifications
            case HOURLY -> now.minusHours(1);
            case DAILY, DAILY_DIGEST -> now.minusDays(1);
            case WEEKLY, WEEKLY_DIGEST -> now.minusDays(7);
            case OFF -> now.minusYears(1); // Include very old notifications if somehow OFF
        };
    }

    private List<Notification> getUnreadNotificationsForDigest(String userId, LocalDateTime cutoffTime) {
        // Get unread notifications that haven't been processed in a digest
        return notificationRepository.findByUserIdAndIsReadFalseAndCreatedAtAfterAndDigestProcessedAtIsNullOrderByCreatedAtDesc(
                userId, cutoffTime);
    }

    private void sendDigestEmail(String userId, NotificationFrequency frequency, 
                                Map<NotificationType, List<Notification>> groupedNotifications) {
        try {
            String subject = String.format("Your %s Notification Digest", 
                    frequency == NotificationFrequency.DAILY_DIGEST ? "Daily" : "Weekly");
            
            String content = buildDigestEmailContent(frequency, groupedNotifications);
            
            // Use the email notification service to send the digest
            Map<String, Object> variables = Map.of(
                    "userId", userId,
                    "frequency", frequency.name(),
                    "notifications", groupedNotifications,
                    "totalCount", groupedNotifications.values().stream().mapToInt(List::size).sum()
            );
            
            NotificationMessage digestMessage = NotificationMessage.builder()
                    .userId(userId)
                    .emailTo(getUserEmail(userId))
                    .emailSubject(subject)
                    .message(content)
                    .templateVariables(variables.entrySet().stream()
                            .collect(Collectors.toMap(
                                    Map.Entry::getKey,
                                    entry -> entry.getValue().toString()
                            )))
                    .timestamp(LocalDateTime.now())
                    .build();
            
            emailNotificationService.sendEmail(digestMessage);
            
            log.debug("Sent {} digest email to user {}", frequency, userId);
        } catch (Exception e) {
            log.error("Failed to send digest email to user {}: {}", userId, e.getMessage());
        }
    }

    private String buildDigestEmailContent(NotificationFrequency frequency, 
                                          Map<NotificationType, List<Notification>> groupedNotifications) {
        StringBuilder content = new StringBuilder();
        content.append(String.format("<h2>Your %s Notification Summary</h2>\n", 
                frequency == NotificationFrequency.DAILY_DIGEST ? "Daily" : "Weekly"));
        
        int totalCount = groupedNotifications.values().stream().mapToInt(List::size).sum();
        content.append(String.format("<p>You have %d new notifications:</p>\n", totalCount));
        
        for (Map.Entry<NotificationType, List<Notification>> entry : groupedNotifications.entrySet()) {
            NotificationType type = entry.getKey();
            List<Notification> notifications = entry.getValue();
            
            content.append(String.format("<h3>%s (%d)</h3>\n", 
                    formatNotificationType(type), notifications.size()));
            
            content.append("<ul>\n");
            for (Notification notification : notifications.stream().limit(5).toList()) {
                content.append(String.format("<li><strong>%s</strong>", notification.getTitle()));
                if (notification.getContent() != null && !notification.getContent().isEmpty()) {
                    content.append(String.format(": %s", truncateContent(notification.getContent(), 100)));
                }
                content.append("</li>\n");
            }
            
            if (notifications.size() > 5) {
                content.append(String.format("<li><em>...and %d more</em></li>\n", notifications.size() - 5));
            }
            content.append("</ul>\n");
        }
        
        content.append("<p><a href=\"#\">View all notifications</a></p>\n");
        return content.toString();
    }

    private String formatNotificationType(NotificationType type) {
        String formatted = type.name().toLowerCase().replace("_", " ");
        // Capitalize first letter of each word
        String[] words = formatted.split(" ");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (word.length() > 0) {
                result.append(Character.toUpperCase(word.charAt(0)));
                if (word.length() > 1) {
                    result.append(word.substring(1));
                }
                result.append(" ");
            }
        }
        return result.toString().trim();
    }

    private String truncateContent(String content, int maxLength) {
        if (content == null || content.length() <= maxLength) {
            return content;
        }
        return content.substring(0, maxLength) + "...";
    }

    private void markNotificationsAsDigestProcessed(List<Notification> notifications) {
        LocalDateTime now = LocalDateTime.now();
        notifications.forEach(notification -> {
            notification.setDigestProcessedAt(now);
        });
        notificationRepository.saveAll(notifications);
    }

    private String getUserEmail(String userId) {
        // This would typically be fetched from the user service
        // For now, return a placeholder - this should be integrated with the identity service
        return userId + "@focushive.com";
    }
}