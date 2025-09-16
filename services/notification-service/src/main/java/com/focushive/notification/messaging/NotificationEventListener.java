package com.focushive.notification.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.focushive.notification.dto.CreateNotificationRequest;
import com.focushive.notification.entity.Notification;
import com.focushive.notification.entity.NotificationType;
import com.focushive.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * RabbitMQ message listener for processing notification events from other services.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    @RabbitListener(bindings = @QueueBinding(
        value = @Queue(value = "notification.queue", durable = "true"),
        exchange = @Exchange(value = "notification.exchange", type = "topic", durable = "true"),
        key = "notification.#"
    ))
    public void handleNotificationEvent(String message) {
        try {
            log.debug("Received message: {}", message);
            Map<String, Object> event = objectMapper.readValue(message, Map.class);
            processEvent(event);
        } catch (Exception e) {
            log.error("Error processing notification event: {}", e.getMessage(), e);
            // Consider implementing retry logic or dead letter queue
        }
    }

    private void processEvent(Map<String, Object> event) {
        String eventType = (String) event.get("eventType");
        log.debug("Processing event type: {}", eventType);

        switch (eventType) {
            case "USER_REGISTERED" -> handleUserRegistered(event);
            case "HIVE_INVITATION_SENT" -> handleHiveInvitation(event);
            case "BUDDY_MATCHED" -> handleBuddyMatched(event);
            case "ACHIEVEMENT_UNLOCKED" -> handleAchievementUnlocked(event);
            case "HIVE_ACTIVITY" -> handleHiveActivity(event);
            case "USER_PROFILE_UPDATED" -> handleUserProfileUpdated(event);
            default -> log.warn("Unknown event type: {}", eventType);
        }
    }

    private void handleUserRegistered(Map<String, Object> event) {
        String userId = (String) event.get("userId");
        String username = (String) event.get("username");
        String email = (String) event.get("email");
        
        Map<String, Object> data = new HashMap<>();
        data.put("username", username);
        data.put("email", email);
        data.put("source", event.get("source"));
        
        CreateNotificationRequest request = CreateNotificationRequest.builder()
            .userId(userId)
            .type(NotificationType.WELCOME)
            .title("Welcome to FocusHive!")
            .content("Welcome " + username + "! We're excited to have you join our community.")
            .actionUrl("https://focushive.com/getting-started")
            .data(data)
            .priority(Notification.NotificationPriority.HIGH)
            .language("en")
            .build();
        
        notificationService.createNotification(request);
        
        log.info("Created welcome notification for user: {}", userId);
    }

    private void handleHiveInvitation(Map<String, Object> event) {
        String inviteeId = (String) event.get("inviteeId");
        String inviterName = (String) event.get("inviterName");
        String hiveName = (String) event.get("hiveName");
        String invitationUrl = (String) event.get("invitationUrl");
        
        Map<String, Object> data = new HashMap<>(event);
        
        CreateNotificationRequest request = CreateNotificationRequest.builder()
            .userId(inviteeId)
            .type(NotificationType.HIVE_INVITATION)
            .title("Invitation to " + hiveName)
            .content("You've been invited by " + inviterName + " to join " + hiveName)
            .actionUrl(invitationUrl)
            .data(data)
            .priority(Notification.NotificationPriority.HIGH)
            .language("en")
            .build();
        
        notificationService.createNotification(request);
        
        log.info("Created hive invitation notification for user: {}", inviteeId);
    }

    private void handleBuddyMatched(Map<String, Object> event) {
        String user1Id = (String) event.get("user1Id");
        String user2Id = (String) event.get("user2Id");
        String user1Name = (String) event.get("user1Name");
        String user2Name = (String) event.get("user2Name");
        String chatUrl = (String) event.get("chatUrl");
        
        Map<String, Object> data = new HashMap<>(event);
        
        // Notify user 1
        CreateNotificationRequest request1 = CreateNotificationRequest.builder()
            .userId(user1Id)
            .type(NotificationType.BUDDY_MATCHED)
            .title("New Buddy Match!")
            .content("You've been matched with " + user2Name + "! Start chatting now.")
            .actionUrl(chatUrl)
            .data(data)
            .priority(Notification.NotificationPriority.HIGH)
            .language("en")
            .build();
        
        notificationService.createNotification(request1);
        
        // Notify user 2
        CreateNotificationRequest request2 = CreateNotificationRequest.builder()
            .userId(user2Id)
            .type(NotificationType.BUDDY_MATCHED)
            .title("New Buddy Match!")
            .content("You've been matched with " + user1Name + "! Start chatting now.")
            .actionUrl(chatUrl)
            .data(data)
            .priority(Notification.NotificationPriority.HIGH)
            .language("en")
            .build();
        
        notificationService.createNotification(request2);
        
        log.info("Created buddy match notifications for users: {} and {}", user1Id, user2Id);
    }

    private void handleAchievementUnlocked(Map<String, Object> event) {
        String userId = (String) event.get("userId");
        String achievementName = (String) event.get("achievementName");
        String achievementDescription = (String) event.get("achievementDescription");
        Integer pointsEarned = (Integer) event.get("pointsEarned");
        String shareUrl = (String) event.get("shareUrl");
        
        Map<String, Object> data = new HashMap<>(event);
        
        CreateNotificationRequest request = CreateNotificationRequest.builder()
            .userId(userId)
            .type(NotificationType.ACHIEVEMENT_UNLOCKED)
            .title("Achievement Unlocked: " + achievementName)
            .content("You earned " + pointsEarned + " points! " + achievementDescription)
            .actionUrl(shareUrl)
            .data(data)
            .priority(Notification.NotificationPriority.NORMAL)
            .language("en")
            .build();
        
        notificationService.createNotification(request);
        
        log.info("Created achievement notification for user: {}", userId);
    }

    private void handleHiveActivity(Map<String, Object> event) {
        String userId = (String) event.get("userId");
        String hiveName = (String) event.get("hiveName");
        String activityType = (String) event.get("activityType");
        String hiveId = (String) event.get("hiveId");
        
        Map<String, Object> data = new HashMap<>(event);
        
        String title = "Activity in " + hiveName;
        String content = switch (activityType) {
            case "SESSION_STARTED" -> "A focus session has started in " + hiveName;
            case "SESSION_COMPLETED" -> "A focus session was completed in " + hiveName;
            case "MEMBER_JOINED" -> "A new member joined " + hiveName;
            default -> "New activity in " + hiveName;
        };
        
        CreateNotificationRequest request = CreateNotificationRequest.builder()
            .userId(userId)
            .type(NotificationType.HIVE_ACTIVITY)
            .title(title)
            .content(content)
            .actionUrl("https://focushive.com/hives/" + hiveId)
            .data(data)
            .priority(Notification.NotificationPriority.LOW)
            .language("en")
            .build();
        
        notificationService.createNotification(request);
        
        log.info("Created hive activity notification for user: {}", userId);
    }

    private void handleUserProfileUpdated(Map<String, Object> event) {
        String userId = (String) event.get("userId");
        
        Map<String, Object> data = new HashMap<>(event);
        
        CreateNotificationRequest request = CreateNotificationRequest.builder()
            .userId(userId)
            .type(NotificationType.WELCOME)
            .title("Profile Updated")
            .content("Your profile has been successfully updated.")
            .actionUrl("https://focushive.com/profile")
            .data(data)
            .priority(Notification.NotificationPriority.LOW)
            .language("en")
            .build();
        
        notificationService.createNotification(request);
        
        log.info("Created profile update notification for user: {}", userId);
    }
}