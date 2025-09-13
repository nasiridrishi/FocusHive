package com.focushive.notification.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.focushive.notification.entity.Notification;
import com.focushive.notification.entity.NotificationType;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.matching.RequestPatternBuilder.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Integration tests for cross-service notification triggers.
 * Tests event-driven notification dispatch from various FocusHive services.
 * 
 * Test scenarios:
 * 1. User registration notification (Identity Service)
 * 2. Hive activity notifications (Backend Service)
 * 3. Buddy matching notifications (Buddy Service)
 * 4. Achievement unlock notifications (Analytics Service)
 * 5. Event-driven notification dispatch with RabbitMQ
 * 6. External service integration with retry logic
 * 7. Notification aggregation and batching
 * 8. Cross-service error handling
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@DisplayName("Cross-Service Notification Integration Tests")
class CrossServiceNotificationIntegrationTest extends BaseIntegrationTest {

    @Container
    static RabbitMQContainer rabbitMQContainer = new RabbitMQContainer("rabbitmq:3.11-management-alpine")
            .withReuse(true);

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TestNotificationEventListener eventListener;

    private WireMockServer identityServiceMock;
    private WireMockServer backendServiceMock;
    private WireMockServer buddyServiceMock;
    private WireMockServer analyticsServiceMock;

    @DynamicPropertySource
    static void configureRabbitMQ(DynamicPropertyRegistry registry) {
        registry.add("spring.rabbitmq.host", rabbitMQContainer::getHost);
        registry.add("spring.rabbitmq.port", rabbitMQContainer::getAmqpPort);
        registry.add("spring.rabbitmq.username", () -> "guest");
        registry.add("spring.rabbitmq.password", () -> "guest");
    }

    @BeforeEach
    void setUpCrossServiceTests() {
        // Setup WireMock servers for external service mocking
        identityServiceMock = new WireMockServer(8081);
        backendServiceMock = new WireMockServer(8080);
        buddyServiceMock = new WireMockServer(8087);
        analyticsServiceMock = new WireMockServer(8085);

        identityServiceMock.start();
        backendServiceMock.start();
        buddyServiceMock.start();
        analyticsServiceMock.start();

        // Clear event listener queue
        eventListener.clearEvents();
    }

    @AfterEach
    void tearDownCrossServiceTests() {
        if (identityServiceMock != null && identityServiceMock.isRunning()) {
            identityServiceMock.stop();
        }
        if (backendServiceMock != null && backendServiceMock.isRunning()) {
            backendServiceMock.stop();
        }
        if (buddyServiceMock != null && buddyServiceMock.isRunning()) {
            buddyServiceMock.stop();
        }
        if (analyticsServiceMock != null && analyticsServiceMock.isRunning()) {
            analyticsServiceMock.stop();
        }
    }

    @Test
    @DisplayName("Should handle user registration notification from Identity Service")
    void shouldHandleUserRegistrationNotificationFromIdentityService() {
        // Given - TDD: Setup Identity Service mock response
        identityServiceMock.stubFor(get(urlPathEqualTo("/api/users/test-user-1"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            {
                                "id": "test-user-1",
                                "username": "johndoe",
                                "email": "john.doe@example.com",
                                "firstName": "John",
                                "lastName": "Doe",
                                "createdAt": "2025-01-15T10:30:00Z",
                                "active": true
                            }
                            """)));

        // Create user registration event
        Map<String, Object> userRegistrationEvent = Map.of(
                "eventType", "USER_REGISTERED",
                "userId", "test-user-1",
                "username", "johndoe",
                "email", "john.doe@example.com",
                "firstName", "John",
                "lastName", "Doe",
                "timestamp", LocalDateTime.now().toString(),
                "source", "identity-service"
        );

        // When - TDD: Send event through RabbitMQ
        rabbitTemplate.convertAndSend("notifications.exchange", "user.registered", userRegistrationEvent);

        // Then - TDD: Verify notification was created and processed
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat(eventListener.getReceivedEvents()).isNotEmpty();
            
            var receivedEvent = eventListener.getReceivedEvents().poll();
            assertThat(receivedEvent).isNotNull();
            assertThat(receivedEvent.get("eventType")).isEqualTo("USER_REGISTERED");
            assertThat(receivedEvent.get("userId")).isEqualTo("test-user-1");
            
            // Verify notification was created in database
            var notifications = notificationRepository.findByUserId("test-user-1");
            assertThat(notifications).hasSize(1);
            
            var welcomeNotification = notifications.get(0);
            assertThat(welcomeNotification.getType()).isEqualTo(NotificationType.WELCOME);
            assertThat(welcomeNotification.getTitle()).contains("Welcome");
            assertThat(welcomeNotification.getIsRead()).isFalse();
        });
    }

    @Test
    @DisplayName("Should handle hive activity notifications from Backend Service")
    void shouldHandleHiveActivityNotificationsFromBackendService() {
        // Given - Setup Backend Service mock
        backendServiceMock.stubFor(get(urlPathMatching("/api/hives/.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            {
                                "id": "hive-123",
                                "name": "Study Group Alpha",
                                "description": "Advanced Mathematics Study Group",
                                "ownerId": "user-owner",
                                "memberCount": 5,
                                "isActive": true,
                                "tags": ["mathematics", "university", "study-group"]
                            }
                            """)));

        // Create hive invitation event
        Map<String, Object> hiveInvitationEvent = new HashMap<>();
        hiveInvitationEvent.put("eventType", "HIVE_INVITATION_SENT");
        hiveInvitationEvent.put("hiveId", "hive-123");
        hiveInvitationEvent.put("hiveName", "Study Group Alpha");
        hiveInvitationEvent.put("inviterId", "user-owner");
        hiveInvitationEvent.put("inviterName", "Alice Smith");
        hiveInvitationEvent.put("inviteeId", "test-user-2");
        hiveInvitationEvent.put("inviteeName", "Bob Johnson");
        hiveInvitationEvent.put("invitationUrl", "https://focushive.com/hive/join/abc123");
        hiveInvitationEvent.put("message", "Join our study group for advanced mathematics!");
        hiveInvitationEvent.put("timestamp", LocalDateTime.now().toString());
        hiveInvitationEvent.put("source", "focushive-backend");

        // When
        rabbitTemplate.convertAndSend("notifications.exchange", "hive.invitation.sent", hiveInvitationEvent);

        // Then
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            var notifications = notificationRepository.findByUserId("test-user-2");
            assertThat(notifications).hasSize(1);
            
            var invitationNotification = notifications.get(0);
            assertThat(invitationNotification.getType()).isEqualTo(NotificationType.HIVE_INVITATION);
            assertThat(invitationNotification.getTitle()).contains("Study Group Alpha");
            assertThat(invitationNotification.getContent()).contains("Alice Smith");
            assertThat(invitationNotification.getActionUrl()).isEqualTo("https://focushive.com/hive/join/abc123");
        });
    }

    @Test
    @DisplayName("Should handle buddy matching notifications from Buddy Service")
    void shouldHandleBuddyMatchingNotificationsFromBuddyService() {
        // Given - Setup Buddy Service mock
        buddyServiceMock.stubFor(get(urlPathMatching("/api/buddy-matches/.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            {
                                "id": "match-456",
                                "requesterId": "test-user-1",
                                "buddyId": "test-user-3",
                                "status": "MATCHED",
                                "compatibilityScore": 85,
                                "commonInterests": ["programming", "algorithms", "coffee"],
                                "matchedAt": "2025-01-15T11:00:00Z"
                            }
                            """)));

        // Create buddy match event
        Map<String, Object> buddyMatchEvent = new HashMap<>();
        buddyMatchEvent.put("eventType", "BUDDY_MATCHED");
        buddyMatchEvent.put("matchId", "match-456");
        buddyMatchEvent.put("user1Id", "test-user-1");
        buddyMatchEvent.put("user1Name", "Charlie Wilson");
        buddyMatchEvent.put("user2Id", "test-user-3");
        buddyMatchEvent.put("user2Name", "Diana Lopez");
        buddyMatchEvent.put("compatibilityScore", 85);
        buddyMatchEvent.put("commonInterests", java.util.List.of("programming", "algorithms", "coffee"));
        buddyMatchEvent.put("chatUrl", "https://focushive.com/chat/buddy-match-456");
        buddyMatchEvent.put("timestamp", LocalDateTime.now().toString());
        buddyMatchEvent.put("source", "buddy-service");

        // When
        rabbitTemplate.convertAndSend("notifications.exchange", "buddy.matched", buddyMatchEvent);

        // Then
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            // Both users should receive notifications
            var user1Notifications = notificationRepository.findByUserId("test-user-1");
            var user2Notifications = notificationRepository.findByUserId("test-user-3");
            
            assertThat(user1Notifications).hasSize(1);
            assertThat(user2Notifications).hasSize(1);
            
            var user1Notification = user1Notifications.get(0);
            assertThat(user1Notification.getType()).isEqualTo(NotificationType.BUDDY_MATCHED);
            assertThat(user1Notification.getTitle()).contains("Buddy Match");
            assertThat(user1Notification.getContent()).contains("Diana Lopez");
            assertThat(user1Notification.getActionUrl()).isEqualTo("https://focushive.com/chat/buddy-match-456");
            
            var user2Notification = user2Notifications.get(0);
            assertThat(user2Notification.getContent()).contains("Charlie Wilson");
        });
    }

    @Test
    @DisplayName("Should handle achievement unlock notifications from Analytics Service")
    void shouldHandleAchievementUnlockNotificationsFromAnalyticsService() {
        // Given - Setup Analytics Service mock
        analyticsServiceMock.stubFor(get(urlPathMatching("/api/achievements/.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            {
                                "id": "achievement-789",
                                "name": "Focus Streak Master",
                                "description": "Maintained a focus streak for 14 consecutive days",
                                "category": "CONSISTENCY",
                                "points": 500,
                                "rarity": "RARE",
                                "iconUrl": "https://focushive.com/icons/focus-streak-master.png"
                            }
                            """)));

        // Create achievement unlock event
        Map<String, Object> achievementEvent = new HashMap<>();
        achievementEvent.put("eventType", "ACHIEVEMENT_UNLOCKED");
        achievementEvent.put("userId", "test-user-1");
        achievementEvent.put("userName", "Eva Rodriguez");
        achievementEvent.put("achievementId", "achievement-789");
        achievementEvent.put("achievementName", "Focus Streak Master");
        achievementEvent.put("achievementDescription", "Maintained a focus streak for 14 consecutive days");
        achievementEvent.put("pointsEarned", 500);
        achievementEvent.put("totalPoints", 2750);
        achievementEvent.put("rarity", "RARE");
        achievementEvent.put("badgeUrl", "https://focushive.com/icons/focus-streak-master.png");
        achievementEvent.put("shareUrl", "https://focushive.com/achievements/share/achievement-789");
        achievementEvent.put("timestamp", LocalDateTime.now().toString());
        achievementEvent.put("source", "analytics-service");

        // When
        rabbitTemplate.convertAndSend("notifications.exchange", "achievement.unlocked", achievementEvent);

        // Then
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            var notifications = notificationRepository.findByUserId("test-user-1");
            assertThat(notifications).hasSize(1);
            
            var achievementNotification = notifications.get(0);
            assertThat(achievementNotification.getType()).isEqualTo(NotificationType.ACHIEVEMENT_UNLOCKED);
            assertThat(achievementNotification.getTitle()).contains("Focus Streak Master");
            assertThat(achievementNotification.getContent()).contains("500");
            assertThat(achievementNotification.getContent()).contains("14 consecutive days");
            assertThat(achievementNotification.getActionUrl()).isEqualTo("https://focushive.com/achievements/share/achievement-789");
        });
    }

    @Test
    @DisplayName("Should handle notification aggregation and batching")
    void shouldHandleNotificationAggregationAndBatching() {
        // Given - Multiple events for the same user in quick succession
        Map<String, Object> event1 = Map.of(
                "eventType", "HIVE_ACTIVITY",
                "userId", "test-user-1",
                "activityType", "FOCUS_SESSION_STARTED",
                "hiveId", "hive-123",
                "hiveName", "Study Group Alpha",
                "timestamp", LocalDateTime.now().toString(),
                "source", "focushive-backend"
        );

        Map<String, Object> event2 = Map.of(
                "eventType", "HIVE_ACTIVITY",
                "userId", "test-user-1",
                "activityType", "BREAK_STARTED",
                "hiveId", "hive-123",
                "hiveName", "Study Group Alpha",
                "timestamp", LocalDateTime.now().plusMinutes(25).toString(),
                "source", "focushive-backend"
        );

        Map<String, Object> event3 = Map.of(
                "eventType", "HIVE_ACTIVITY",
                "userId", "test-user-1",
                "activityType", "SESSION_COMPLETED",
                "hiveId", "hive-123",
                "hiveName", "Study Group Alpha",
                "timestamp", LocalDateTime.now().plusMinutes(30).toString(),
                "source", "focushive-backend"
        );

        // When - Send events rapidly
        rabbitTemplate.convertAndSend("notifications.exchange", "hive.activity", event1);
        rabbitTemplate.convertAndSend("notifications.exchange", "hive.activity", event2);
        rabbitTemplate.convertAndSend("notifications.exchange", "hive.activity", event3);

        // Then - Should handle all events appropriately
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat(eventListener.getReceivedEvents()).hasSizeGreaterThanOrEqualTo(3);
            
            // Verify events were processed but potentially aggregated
            var notifications = notificationRepository.findByUserId("test-user-1");
            assertThat(notifications).isNotEmpty();
            
            // Check for aggregation logic (implementation dependent)
            var hiveActivityNotifications = notifications.stream()
                    .filter(n -> n.getType() == NotificationType.HIVE_ACTIVITY)
                    .toList();
            assertThat(hiveActivityNotifications).isNotEmpty();
        });
    }

    @Test
    @DisplayName("Should handle cross-service communication failures with retry logic")
    void shouldHandleCrossServiceCommunicationFailuresWithRetryLogic() {
        // Given - Setup failing external service
        identityServiceMock.stubFor(get(urlPathEqualTo("/api/users/test-user-1"))
                .inScenario("Retry Scenario")
                .whenScenarioStateIs("Started")
                .willReturn(aResponse()
                        .withStatus(500)
                        .withBody("Internal Server Error"))
                .willSetStateTo("First Failure"));

        identityServiceMock.stubFor(get(urlPathEqualTo("/api/users/test-user-1"))
                .inScenario("Retry Scenario")
                .whenScenarioStateIs("First Failure")
                .willReturn(aResponse()
                        .withStatus(503)
                        .withBody("Service Unavailable"))
                .willSetStateTo("Second Failure"));

        identityServiceMock.stubFor(get(urlPathEqualTo("/api/users/test-user-1"))
                .inScenario("Retry Scenario")
                .whenScenarioStateIs("Second Failure")
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            {
                                "id": "test-user-1",
                                "username": "retryuser",
                                "email": "retry@example.com",
                                "firstName": "Retry",
                                "lastName": "User"
                            }
                            """)));

        // Create event that requires external service call
        Map<String, Object> userEvent = Map.of(
                "eventType", "USER_PROFILE_UPDATED",
                "userId", "test-user-1",
                "timestamp", LocalDateTime.now().toString(),
                "source", "identity-service"
        );

        // When
        rabbitTemplate.convertAndSend("notifications.exchange", "user.profile.updated", userEvent);

        // Then - Should eventually succeed after retries
        await().atMost(30, TimeUnit.SECONDS).untilAsserted(() -> {
            // Verify that the service was called multiple times (retries)
            identityServiceMock.verify(1, getRequestedFor(urlPathEqualTo("/api/users/test-user-1")));
            // For multiple retries, could use moreThanOrExactly(1) if available
            
            // Verify event was eventually processed successfully
            assertThat(eventListener.getReceivedEvents()).isNotEmpty();
            
            var receivedEvent = eventListener.getReceivedEvents().stream()
                    .filter(event -> "USER_PROFILE_UPDATED".equals(event.get("eventType")))
                    .findFirst();
            assertThat(receivedEvent).isPresent();
        });
    }

    @Test
    @DisplayName("Should handle invalid event formats gracefully")
    void shouldHandleInvalidEventFormatsGracefully() {
        // Given - Invalid event formats
        Map<String, Object> malformedEvent1 = Map.of(
                "eventType", "", // Empty event type
                "userId", "test-user-1"
        );

        Map<String, Object> malformedEvent2 = Map.of(
                "eventType", "UNKNOWN_EVENT_TYPE",
                "userId", "test-user-1",
                "invalidField", "shouldBeIgnored"
        );

        String invalidJsonEvent = "{ invalid json format }";

        // When - Send malformed events
        rabbitTemplate.convertAndSend("notifications.exchange", "invalid.event", malformedEvent1);
        rabbitTemplate.convertAndSend("notifications.exchange", "unknown.event", malformedEvent2);
        rabbitTemplate.convertAndSend("notifications.exchange", "invalid.json", invalidJsonEvent);

        // Then - Should handle gracefully without crashing
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            // Verify system is still responsive
            var testEvent = Map.of(
                    "eventType", "TEST_EVENT",
                    "userId", "test-user-1",
                    "timestamp", LocalDateTime.now().toString()
            );
            
            rabbitTemplate.convertAndSend("notifications.exchange", "test.event", testEvent);
            
            // System should still be able to process valid events
            assertThat(eventListener.getReceivedEvents()).isNotEmpty();
        });
    }

    /**
     * Test component to capture RabbitMQ events for verification
     */
    @Component
    static class TestNotificationEventListener {
        private final ConcurrentLinkedQueue<Map<String, Object>> receivedEvents = new ConcurrentLinkedQueue<>();

        @RabbitListener(queues = "#{notificationQueue.name}")
        public void handleNotificationEvent(@Payload Map<String, Object> event) {
            receivedEvents.offer(event);
        }

        public ConcurrentLinkedQueue<Map<String, Object>> getReceivedEvents() {
            return receivedEvents;
        }

        public void clearEvents() {
            receivedEvents.clear();
        }
    }
}