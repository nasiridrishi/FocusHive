package com.focushive.notification.repository;

import com.focushive.notification.entity.NotificationFrequency;
import com.focushive.notification.entity.NotificationPreference;
import com.focushive.notification.entity.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for managing notification preferences.
 * Provides CRUD operations and custom queries for user notification settings.
 */
@Repository
public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, String> {

    /**
     * Find all notification preferences for a specific user.
     *
     * @param userId the user ID
     * @return list of notification preferences for the user
     */
    List<NotificationPreference> findByUserId(String userId);

    /**
     * Find a specific notification preference for a user and notification type.
     *
     * @param userId the user ID
     * @param notificationType the notification type
     * @return optional notification preference
     */
    Optional<NotificationPreference> findByUserIdAndNotificationType(String userId, NotificationType notificationType);

    /**
     * Find all enabled notification preferences for a user.
     * A preference is considered enabled if frequency is not OFF and at least one channel is enabled.
     *
     * @param userId the user ID
     * @return list of enabled notification preferences
     */
    @Query("SELECT np FROM NotificationPreference np WHERE np.userId = :userId " +
           "AND np.frequency != 'OFF' " +
           "AND (np.inAppEnabled = true OR np.emailEnabled = true OR np.pushEnabled = true)")
    List<NotificationPreference> findEnabledPreferencesForUser(@Param("userId") String userId);

    /**
     * Find notification preferences for a user where in-app notifications are enabled.
     *
     * @param userId the user ID
     * @return list of preferences with in-app notifications enabled
     */
    List<NotificationPreference> findByUserIdAndInAppEnabledTrue(String userId);

    /**
     * Find notification preferences for a user where email notifications are enabled.
     *
     * @param userId the user ID
     * @return list of preferences with email notifications enabled
     */
    List<NotificationPreference> findByUserIdAndEmailEnabledTrue(String userId);

    /**
     * Find notification preferences for a user where push notifications are enabled.
     *
     * @param userId the user ID
     * @return list of preferences with push notifications enabled
     */
    List<NotificationPreference> findByUserIdAndPushEnabledTrue(String userId);

    /**
     * Find all notification preferences with a specific frequency setting.
     *
     * @param frequency the notification frequency
     * @return list of preferences with the specified frequency
     */
    List<NotificationPreference> findByFrequency(NotificationFrequency frequency);

    /**
     * Check if a notification preference exists for a user and notification type.
     *
     * @param userId the user ID
     * @param notificationType the notification type
     * @return true if preference exists, false otherwise
     */
    boolean existsByUserIdAndNotificationType(String userId, NotificationType notificationType);

    /**
     * Delete a specific notification preference for a user and notification type.
     *
     * @param userId the user ID
     * @param notificationType the notification type
     */
    void deleteByUserIdAndNotificationType(String userId, NotificationType notificationType);

    /**
     * Delete all notification preferences for a user.
     *
     * @param userId the user ID
     */
    void deleteByUserId(String userId);

    /**
     * Find preferences that should receive digest notifications.
     * Used by the batch notification scheduler.
     *
     * @param frequency the digest frequency (DAILY_DIGEST or WEEKLY_DIGEST)
     * @return list of preferences configured for digest delivery
     */
    @Query("SELECT np FROM NotificationPreference np WHERE np.frequency = :frequency " +
           "AND (np.emailEnabled = true OR np.inAppEnabled = true)")
    List<NotificationPreference> findDigestPreferences(@Param("frequency") NotificationFrequency frequency);

    /**
     * Find preferences for in-app notifications by notification type.
     * Used for real-time WebSocket delivery.
     *
     * @param notificationType the notification type
     * @return list of user IDs who should receive in-app notifications for this type
     */
    @Query("SELECT np.userId FROM NotificationPreference np WHERE np.notificationType = :notificationType " +
           "AND np.inAppEnabled = true AND np.frequency = 'IMMEDIATE'")
    List<String> findUserIdsForInAppNotifications(@Param("notificationType") NotificationType notificationType);

    /**
     * Find preferences for email notifications by notification type.
     * Used for email delivery.
     *
     * @param notificationType the notification type
     * @return list of user IDs who should receive email notifications for this type
     */
    @Query("SELECT np.userId FROM NotificationPreference np WHERE np.notificationType = :notificationType " +
           "AND np.emailEnabled = true AND np.frequency = 'IMMEDIATE'")
    List<String> findUserIdsForEmailNotifications(@Param("notificationType") NotificationType notificationType);

    /**
     * Find preferences for push notifications by notification type.
     * Used for push notification delivery.
     *
     * @param notificationType the notification type
     * @return list of user IDs who should receive push notifications for this type
     */
    @Query("SELECT np.userId FROM NotificationPreference np WHERE np.notificationType = :notificationType " +
           "AND np.pushEnabled = true AND np.frequency = 'IMMEDIATE'")
    List<String> findUserIdsForPushNotifications(@Param("notificationType") NotificationType notificationType);
}