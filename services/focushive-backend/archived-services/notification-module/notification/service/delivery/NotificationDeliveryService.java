package com.focushive.notification.service.delivery;

import com.focushive.notification.dto.NotificationDto;
import com.focushive.notification.entity.NotificationPreference;

/**
 * Service interface for delivering notifications through various channels.
 * Implements the strategy pattern to handle different delivery methods.
 */
public interface NotificationDeliveryService {

    /**
     * Deliver a notification based on user preferences.
     * This method coordinates delivery across all enabled channels.
     *
     * @param notification the notification to deliver
     * @param preferences user preferences for this notification type
     */
    void deliverNotification(NotificationDto notification, NotificationPreference preferences);

    /**
     * Deliver an in-app notification via WebSocket.
     *
     * @param notification the notification to deliver
     * @param userId the user ID to deliver to
     */
    void deliverInAppNotification(NotificationDto notification, String userId);

    /**
     * Deliver an email notification.
     *
     * @param notification the notification to deliver
     * @param userEmail the user's email address
     * @param language the user's preferred language
     */
    void deliverEmailNotification(NotificationDto notification, String userEmail, String language);

    /**
     * Deliver a push notification.
     *
     * @param notification the notification to deliver
     * @param userId the user ID to deliver to
     */
    void deliverPushNotification(NotificationDto notification, String userId);

    /**
     * Check if delivery is possible for the given channel.
     *
     * @param channel the delivery channel (in_app, email, push)
     * @return true if delivery is possible, false otherwise
     */
    boolean isDeliveryAvailable(String channel);
}