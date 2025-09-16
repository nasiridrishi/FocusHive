package com.focushive.notification.service;

import com.focushive.notification.entity.NotificationFrequency;
import com.focushive.notification.entity.NotificationPreference;
import com.focushive.notification.entity.NotificationType;
import com.focushive.notification.exception.ResourceNotFoundException;
import com.focushive.notification.repository.NotificationPreferenceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing notification preferences.
 * Provides comprehensive CRUD operations and business logic for user notification settings.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class NotificationPreferenceService {

    private final NotificationPreferenceRepository preferenceRepository;
    private final SecurityAuditService securityAuditService;

    /**
     * Create a new notification preference for a user.
     *
     * @param userId the user ID
     * @param notificationType the notification type
     * @param inAppEnabled whether in-app notifications are enabled
     * @param emailEnabled whether email notifications are enabled
     * @param pushEnabled whether push notifications are enabled
     * @param frequency the notification frequency
     * @param quietStartTime the start time for quiet hours
     * @param quietEndTime the end time for quiet hours
     * @return the created notification preference
     * @throws IllegalArgumentException if preference already exists or invalid parameters
     */
    public NotificationPreference createPreference(String userId, NotificationType notificationType,
                                                 Boolean inAppEnabled, Boolean emailEnabled, Boolean pushEnabled,
                                                 NotificationFrequency frequency, LocalTime quietStartTime, 
                                                 LocalTime quietEndTime) {
        validateCreateParameters(userId, notificationType, frequency);
        
        if (preferenceRepository.existsByUserIdAndNotificationType(userId, notificationType)) {
            throw new IllegalArgumentException(
                String.format("Notification preference for user '%s' and type '%s' already exists", 
                    userId, notificationType)
            );
        }

        NotificationPreference preference = NotificationPreference.builder()
                .userId(userId)
                .notificationType(notificationType)
                .inAppEnabled(inAppEnabled != null ? inAppEnabled : true)
                .emailEnabled(emailEnabled != null ? emailEnabled : true)
                .pushEnabled(pushEnabled != null ? pushEnabled : true)
                .frequency(frequency)
                .quietStartTime(quietStartTime)
                .quietEndTime(quietEndTime)
                .build();

        NotificationPreference saved = preferenceRepository.save(preference);
        log.info("Created notification preference for user '{}' and type '{}'", userId, notificationType);
        
        // Audit log preference creation
        Map<String, Object> details = Map.of(
            "action", "CREATE",
            "notificationType", notificationType.name(),
            "inAppEnabled", saved.getInAppEnabled(),
            "emailEnabled", saved.getEmailEnabled(),
            "pushEnabled", saved.getPushEnabled(),
            "frequency", saved.getFrequency().name()
        );
        securityAuditService.logPreferenceChange(saved.getId(), notificationType.name(), details);
        
        return saved;
    }

    /**
     * Get a notification preference by ID.
     *
     * @param preferenceId the preference ID
     * @return the notification preference
     * @throws ResourceNotFoundException if preference not found
     */
    @Transactional(readOnly = true)
    public NotificationPreference getPreferenceById(String preferenceId) {
        return preferenceRepository.findById(preferenceId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification preference not found: " + preferenceId));
    }

    /**
     * Get all notification preferences for a user.
     *
     * @param userId the user ID
     * @return list of notification preferences
     */
    @Transactional(readOnly = true)
    public List<NotificationPreference> getUserPreferences(String userId) {
        validateUserId(userId);
        return preferenceRepository.findByUserId(userId);
    }

    /**
     * Get a specific notification preference for a user and notification type.
     *
     * @param userId the user ID
     * @param notificationType the notification type
     * @return optional notification preference
     */
    @Transactional(readOnly = true)
    public Optional<NotificationPreference> getUserPreference(String userId, NotificationType notificationType) {
        validateUserId(userId);
        validateNotificationType(notificationType);
        return preferenceRepository.findByUserIdAndNotificationType(userId, notificationType);
    }

    /**
     * Update an existing notification preference.
     *
     * @param preferenceId the preference ID
     * @param inAppEnabled whether in-app notifications are enabled
     * @param emailEnabled whether email notifications are enabled
     * @param pushEnabled whether push notifications are enabled
     * @param frequency the notification frequency
     * @param quietStartTime the start time for quiet hours
     * @param quietEndTime the end time for quiet hours
     * @return the updated notification preference
     * @throws ResourceNotFoundException if preference not found
     */
    public NotificationPreference updatePreference(String preferenceId, Boolean inAppEnabled, Boolean emailEnabled, 
                                                 Boolean pushEnabled, NotificationFrequency frequency,
                                                 LocalTime quietStartTime, LocalTime quietEndTime) {
        NotificationPreference preference = getPreferenceById(preferenceId);
        
        // Track changes for audit logging
        Map<String, Object> changes = new HashMap<>();

        if (inAppEnabled != null && !preference.getInAppEnabled().equals(inAppEnabled)) {
            changes.put("inAppEnabled", preference.getInAppEnabled() + " -> " + inAppEnabled);
            preference.setInAppEnabled(inAppEnabled);
        }
        if (emailEnabled != null && !preference.getEmailEnabled().equals(emailEnabled)) {
            changes.put("emailEnabled", preference.getEmailEnabled() + " -> " + emailEnabled);
            preference.setEmailEnabled(emailEnabled);
        }
        if (pushEnabled != null && !preference.getPushEnabled().equals(pushEnabled)) {
            changes.put("pushEnabled", preference.getPushEnabled() + " -> " + pushEnabled);
            preference.setPushEnabled(pushEnabled);
        }
        if (frequency != null && !preference.getFrequency().equals(frequency)) {
            changes.put("frequency", preference.getFrequency() + " -> " + frequency);
            preference.setFrequency(frequency);
        }
        if (quietStartTime != null && !Objects.equals(preference.getQuietStartTime(), quietStartTime)) {
            changes.put("quietStartTime", preference.getQuietStartTime() + " -> " + quietStartTime);
            preference.setQuietStartTime(quietStartTime);
        }
        if (quietEndTime != null && !Objects.equals(preference.getQuietEndTime(), quietEndTime)) {
            changes.put("quietEndTime", preference.getQuietEndTime() + " -> " + quietEndTime);
            preference.setQuietEndTime(quietEndTime);
        }

        NotificationPreference updated = preferenceRepository.save(preference);
        log.info("Updated notification preference '{}' for user '{}'", preferenceId, preference.getUserId());
        
        // Audit log preference changes if any were made
        if (!changes.isEmpty()) {
            changes.put("action", "UPDATE");
            securityAuditService.logPreferenceChange(preferenceId, preference.getNotificationType().name(), changes);
        }
        
        return updated;
    }

    /**
     * Create or update a notification preference.
     *
     * @param userId the user ID
     * @param notificationType the notification type
     * @param inAppEnabled whether in-app notifications are enabled
     * @param emailEnabled whether email notifications are enabled
     * @param pushEnabled whether push notifications are enabled
     * @param frequency the notification frequency
     * @param quietStartTime the start time for quiet hours
     * @param quietEndTime the end time for quiet hours
     * @return the created or updated notification preference
     */
    public NotificationPreference createOrUpdatePreference(String userId, NotificationType notificationType,
                                                          Boolean inAppEnabled, Boolean emailEnabled, Boolean pushEnabled,
                                                          NotificationFrequency frequency, LocalTime quietStartTime,
                                                          LocalTime quietEndTime) {
        Optional<NotificationPreference> existing = getUserPreference(userId, notificationType);
        
        if (existing.isPresent()) {
            return updatePreference(existing.get().getId(), inAppEnabled, emailEnabled, pushEnabled, 
                                  frequency, quietStartTime, quietEndTime);
        } else {
            return createPreference(userId, notificationType, inAppEnabled, emailEnabled, pushEnabled,
                                  frequency, quietStartTime, quietEndTime);
        }
    }

    /**
     * Delete a notification preference by ID.
     *
     * @param preferenceId the preference ID
     * @throws ResourceNotFoundException if preference not found
     */
    public void deletePreference(String preferenceId) {
        NotificationPreference preference = getPreferenceById(preferenceId);
        
        // Audit log before deletion
        Map<String, Object> details = Map.of(
            "action", "DELETE",
            "notificationType", preference.getNotificationType().name(),
            "userId", preference.getUserId()
        );
        securityAuditService.logPreferenceChange(preferenceId, preference.getNotificationType().name(), details);
        
        preferenceRepository.deleteById(preferenceId);
        log.info("Deleted notification preference '{}'", preferenceId);
    }

    /**
     * Get enabled notification preferences for a user.
     *
     * @param userId the user ID
     * @return list of enabled preferences
     */
    @Transactional(readOnly = true)
    public List<NotificationPreference> getEnabledPreferencesForUser(String userId) {
        validateUserId(userId);
        return preferenceRepository.findEnabledPreferencesForUser(userId);
    }

    /**
     * Check if notifications are enabled for a user and notification type.
     * Returns true by default if no preference exists.
     *
     * @param userId the user ID
     * @param notificationType the notification type
     * @return true if notifications are enabled, false otherwise
     */
    @Transactional(readOnly = true)
    public boolean isNotificationEnabled(String userId, NotificationType notificationType) {
        Optional<NotificationPreference> preference = getUserPreference(userId, notificationType);
        return preference.map(NotificationPreference::isNotificationEnabled).orElse(true);
    }

    /**
     * Check if the current time is within the user's quiet hours for a notification type.
     *
     * @param userId the user ID
     * @param notificationType the notification type
     * @param currentTime the current time
     * @return true if within quiet hours, false otherwise
     */
    @Transactional(readOnly = true)
    public boolean isInQuietHours(String userId, NotificationType notificationType, LocalTime currentTime) {
        Optional<NotificationPreference> preference = getUserPreference(userId, notificationType);
        return preference.map(pref -> pref.isInQuietHours(currentTime)).orElse(false);
    }

    /**
     * Get user IDs eligible for in-app notifications for a notification type.
     *
     * @param notificationType the notification type
     * @return list of user IDs
     */
    @Transactional(readOnly = true)
    public List<String> getUsersForInAppNotifications(NotificationType notificationType) {
        validateNotificationType(notificationType);
        return preferenceRepository.findUserIdsForInAppNotifications(notificationType);
    }

    /**
     * Get user IDs eligible for email notifications for a notification type.
     *
     * @param notificationType the notification type
     * @return list of user IDs
     */
    @Transactional(readOnly = true)
    public List<String> getUsersForEmailNotifications(NotificationType notificationType) {
        validateNotificationType(notificationType);
        return preferenceRepository.findUserIdsForEmailNotifications(notificationType);
    }

    /**
     * Get user IDs eligible for push notifications for a notification type.
     *
     * @param notificationType the notification type
     * @return list of user IDs
     */
    @Transactional(readOnly = true)
    public List<String> getUsersForPushNotifications(NotificationType notificationType) {
        validateNotificationType(notificationType);
        return preferenceRepository.findUserIdsForPushNotifications(notificationType);
    }

    /**
     * Get notification preferences for digest delivery.
     *
     * @param frequency the digest frequency
     * @return list of preferences configured for digest delivery
     */
    @Transactional(readOnly = true)
    public List<NotificationPreference> getDigestPreferences(NotificationFrequency frequency) {
        validateFrequency(frequency);
        return preferenceRepository.findDigestPreferences(frequency);
    }

    /**
     * Create default notification preferences for a new user.
     * Only creates preferences if the user doesn't have any existing ones.
     *
     * @param userId the user ID
     * @return list of created default preferences or existing preferences
     */
    public List<NotificationPreference> createDefaultPreferencesForUser(String userId) {
        validateUserId(userId);
        
        List<NotificationPreference> existingPreferences = preferenceRepository.findByUserId(userId);
        if (!existingPreferences.isEmpty()) {
            log.debug("User '{}' already has preferences, skipping default creation", userId);
            return existingPreferences;
        }

        List<NotificationPreference> defaultPreferences = Arrays.stream(NotificationType.values())
                .map(type -> NotificationPreference.builder()
                        .userId(userId)
                        .notificationType(type)
                        .inAppEnabled(true)
                        .emailEnabled(isEmailEnabledByDefault(type))
                        .pushEnabled(isPushEnabledByDefault(type))
                        .frequency(getDefaultFrequency(type))
                        .build())
                .collect(Collectors.<NotificationPreference>toList());

        List<NotificationPreference> saved = preferenceRepository.saveAll(defaultPreferences);
        log.info("Created {} default notification preferences for user '{}'", saved.size(), userId);
        return saved;
    }

    /**
     * Get preferences by frequency setting.
     *
     * @param frequency the notification frequency
     * @return list of preferences with the specified frequency
     */
    @Transactional(readOnly = true)
    public List<NotificationPreference> getPreferencesByFrequency(NotificationFrequency frequency) {
        validateFrequency(frequency);
        return preferenceRepository.findByFrequency(frequency);
    }

    // Validation methods
    private void validateCreateParameters(String userId, NotificationType notificationType, NotificationFrequency frequency) {
        validateUserId(userId);
        validateNotificationType(notificationType);
        validateFrequency(frequency);
    }

    private void validateUserId(String userId) {
        if (!StringUtils.hasText(userId)) {
            throw new IllegalArgumentException("User ID cannot be null or blank");
        }
    }

    private void validateNotificationType(NotificationType notificationType) {
        if (notificationType == null) {
            throw new IllegalArgumentException("Notification type cannot be null");
        }
    }

    private void validateFrequency(NotificationFrequency frequency) {
        if (frequency == null) {
            throw new IllegalArgumentException("Frequency cannot be null");
        }
    }

    // Default configuration methods
    private boolean isEmailEnabledByDefault(NotificationType type) {
        // Marketing and system notifications disabled by default
        return type != NotificationType.MARKETING && 
               type != NotificationType.SYSTEM_UPDATE;
    }

    private boolean isPushEnabledByDefault(NotificationType type) {
        // Only enable push for urgent notifications by default
        return type == NotificationType.BUDDY_REQUEST ||
               type == NotificationType.BUDDY_MATCHED ||
               type == NotificationType.CHAT_MESSAGE ||
               type == NotificationType.SYSTEM_ANNOUNCEMENT;
    }

    private NotificationFrequency getDefaultFrequency(NotificationType type) {
        // Use digest for less urgent notifications
        if (type == NotificationType.WEEKLY_SUMMARY ||
            type == NotificationType.HIVE_ACTIVITY ||
            type == NotificationType.MARKETING) {
            return NotificationFrequency.WEEKLY_DIGEST;
        }
        
        if (type == NotificationType.FORUM_REPLY ||
            type == NotificationType.TASK_COMPLETED ||
            type == NotificationType.ACHIEVEMENT_UNLOCKED) {
            return NotificationFrequency.DAILY_DIGEST;
        }
        
        return NotificationFrequency.IMMEDIATE;
    }
}