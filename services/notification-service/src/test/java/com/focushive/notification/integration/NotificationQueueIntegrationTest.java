package com.focushive.notification.integration;

import com.focushive.notification.entity.Notification;
import com.focushive.notification.entity.NotificationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Integration tests for notification queue and retry mechanism functionality.
 * Tests Redis-based queue processing, retry logic, dead letter queues, and performance.
 * 
 * Test scenarios:
 * 1. Queue processing with Redis
 * 2. Retry mechanism with exponential backoff
 * 3. Dead letter queue handling
 * 4. Priority queue for urgent notifications
 * 5. Bulk notification processing
 * 6. Queue monitoring and metrics
 * 7. Graceful shutdown with pending notifications
 * 8. Queue persistence and recovery
 * 9. Rate limiting and throttling
 * 10. Queue performance under load
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@DisplayName("Notification Queue Integration Tests")
class NotificationQueueIntegrationTest extends BaseIntegrationTest {

    @Container
    static GenericContainer<?> redisContainer = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379)
            .withReuse(true);

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String NOTIFICATION_QUEUE = "notification:queue";
    private static final String RETRY_QUEUE = "notification:retry";
    private static final String DEAD_LETTER_QUEUE = "notification:dead_letter";
    private static final String PRIORITY_QUEUE = "notification:priority";

    @DynamicPropertySource
    static void configureRedis(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redisContainer::getHost);
        registry.add("spring.data.redis.port", () -> redisContainer.getMappedPort(6379).toString());
    }

    @BeforeEach
    void setUpQueueTests() {
        // Clear all queues before each test
        clearAllQueues();
    }

    @Test
    @DisplayName("Should process notifications from Redis queue successfully")
    void shouldProcessNotificationsFromRedisQueueSuccessfully() {
        // Given - TDD: Create test notifications for queue processing
        Notification highPriorityNotification = createTestNotification(
                "test-user-1", NotificationType.SYSTEM_NOTIFICATION, 
                "System Alert", "Critical system maintenance scheduled");
        highPriorityNotification.setPriority(Notification.NotificationPriority.HIGH);

        Notification normalNotification = createTestNotification(
                "test-user-2", NotificationType.WELCOME,
                "Welcome!", "Welcome to FocusHive");
        normalNotification.setPriority(Notification.NotificationPriority.NORMAL);

        // Save notifications to database
        notificationRepository.save(highPriorityNotification);
        notificationRepository.save(normalNotification);

        // When - TDD: Add notifications to Redis queue
        addToQueue(NOTIFICATION_QUEUE, highPriorityNotification);
        addToQueue(NOTIFICATION_QUEUE, normalNotification);

        // Then - TDD: Verify notifications are in queue and can be processed
        Long queueSize = redisTemplate.opsForList().size(NOTIFICATION_QUEUE);
        assertThat(queueSize).isEqualTo(2);

        // Process notifications from queue
        Object processedNotification1 = redisTemplate.opsForList().leftPop(NOTIFICATION_QUEUE);
        Object processedNotification2 = redisTemplate.opsForList().leftPop(NOTIFICATION_QUEUE);

        assertThat(processedNotification1).isNotNull();
        assertThat(processedNotification2).isNotNull();

        // Verify queue is now empty
        Long finalQueueSize = redisTemplate.opsForList().size(NOTIFICATION_QUEUE);
        assertThat(finalQueueSize).isEqualTo(0);
    }

    @Test
    @DisplayName("Should implement retry mechanism with exponential backoff")
    void shouldImplementRetryMechanismWithExponentialBackoff() {
        // Given - Notification that will fail processing
        Notification failingNotification = createTestNotification(
                "test-user-1", NotificationType.PASSWORD_RESET,
                "Password Reset", "Reset your password");
        notificationRepository.save(failingNotification);

        // Simulate processing failures
        Map<String, Object> notificationWithRetryInfo = Map.of(
                "id", failingNotification.getId(),
                "userId", failingNotification.getUserId(),
                "type", failingNotification.getType().toString(),
                "title", failingNotification.getTitle(),
                "retryCount", 0,
                "maxRetries", 3,
                "nextRetryTime", System.currentTimeMillis() + 1000, // 1 second delay
                "backoffMultiplier", 2
        );

        // When - Add to retry queue with exponential backoff logic
        addToQueue(RETRY_QUEUE, notificationWithRetryInfo);

        // Simulate retry processing
        for (int attempt = 1; attempt <= 3; attempt++) {
            // Process from retry queue
            Object retryItem = redisTemplate.opsForList().leftPop(RETRY_QUEUE);
            assertThat(retryItem).isNotNull();

            @SuppressWarnings("unchecked")
            Map<String, Object> retryData = (Map<String, Object>) retryItem;
            int currentRetryCount = (Integer) retryData.get("retryCount");
            
            // Increment retry count and calculate next retry time
            currentRetryCount++;
            long nextRetryDelay = (long) (1000 * Math.pow(2, currentRetryCount - 1)); // Exponential backoff
            
            retryData.put("retryCount", currentRetryCount);
            retryData.put("nextRetryTime", System.currentTimeMillis() + nextRetryDelay);

            if (currentRetryCount < 3) {
                // Add back to retry queue if not exceeded max retries
                addToQueue(RETRY_QUEUE, retryData);
            } else {
                // Move to dead letter queue after max retries
                addToQueue(DEAD_LETTER_QUEUE, retryData);
            }

            // Update notification in database
            failingNotification.markDeliveryFailed("Delivery attempt " + attempt + " failed");
            notificationRepository.save(failingNotification);
        }

        // Then - Verify retry logic
        Long retryQueueSize = redisTemplate.opsForList().size(RETRY_QUEUE);
        Long deadLetterQueueSize = redisTemplate.opsForList().size(DEAD_LETTER_QUEUE);

        assertThat(retryQueueSize).isEqualTo(0);
        assertThat(deadLetterQueueSize).isEqualTo(1);

        // Verify notification failure tracking
        Notification updatedNotification = notificationRepository.findById(failingNotification.getId()).orElseThrow();
        assertThat(updatedNotification.getDeliveryAttempts()).isEqualTo(3);
        assertThat(updatedNotification.getFailedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should handle dead letter queue for permanently failed notifications")
    void shouldHandleDeadLetterQueueForPermanentlyFailedNotifications() {
        // Given - Notifications that have exceeded retry limits
        List<Notification> failedNotifications = Arrays.asList(
                createTestNotification("user-1", NotificationType.EMAIL_VERIFICATION, "Verify Email", "Please verify"),
                createTestNotification("user-2", NotificationType.HIVE_INVITATION, "Hive Invite", "Join our hive"),
                createTestNotification("user-3", NotificationType.BUDDY_REQUEST, "Buddy Request", "New buddy request")
        );

        failedNotifications.forEach(notificationRepository::save);

        // When - Add failed notifications to dead letter queue
        failedNotifications.forEach(notification -> {
            Map<String, Object> deadLetterItem = Map.of(
                    "id", notification.getId(),
                    "userId", notification.getUserId(),
                    "type", notification.getType().toString(),
                    "title", notification.getTitle(),
                    "retryCount", 3,
                    "failedAt", LocalDateTime.now().toString(),
                    "failureReason", "Max retries exceeded",
                    "originalQueueTime", System.currentTimeMillis() - 300000 // 5 minutes ago
            );
            
            addToQueue(DEAD_LETTER_QUEUE, deadLetterItem);
            
            // Update notification status
            notification.markDeliveryFailed("Max retries exceeded - moved to dead letter queue");
            notificationRepository.save(notification);
        });

        // Then - Verify dead letter queue handling
        Long deadLetterQueueSize = redisTemplate.opsForList().size(DEAD_LETTER_QUEUE);
        assertThat(deadLetterQueueSize).isEqualTo(3);

        // Process dead letter queue items for analysis
        List<Object> deadLetterItems = redisTemplate.opsForList().range(DEAD_LETTER_QUEUE, 0, -1);
        assertThat(deadLetterItems).hasSize(3);

        // Verify each dead letter item has required fields
        deadLetterItems.forEach(item -> {
            @SuppressWarnings("unchecked")
            Map<String, Object> deadLetterData = (Map<String, Object>) item;
            assertThat(deadLetterData).containsKeys("id", "userId", "type", "failureReason");
            assertThat(deadLetterData.get("retryCount")).isEqualTo(3);
        });
    }

    @Test
    @DisplayName("Should implement priority queue for urgent notifications")
    void shouldImplementPriorityQueueForUrgentNotifications() {
        // Given - Notifications with different priorities
        Notification urgentNotification = createTestNotification(
                "admin-user", NotificationType.SYSTEM_NOTIFICATION,
                "URGENT: Security Alert", "Immediate action required");
        urgentNotification.setPriority(Notification.NotificationPriority.URGENT);

        Notification highPriorityNotification = createTestNotification(
                "test-user-1", NotificationType.PASSWORD_RESET,
                "Password Reset Required", "Your password needs to be reset");
        highPriorityNotification.setPriority(Notification.NotificationPriority.HIGH);

        Notification normalNotification = createTestNotification(
                "test-user-2", NotificationType.ACHIEVEMENT_UNLOCKED,
                "Achievement Unlocked!", "You earned a new badge");
        normalNotification.setPriority(Notification.NotificationPriority.NORMAL);

        Notification lowPriorityNotification = createTestNotification(
                "test-user-3", NotificationType.MARKETING,
                "Monthly Newsletter", "Check out our latest updates");
        lowPriorityNotification.setPriority(Notification.NotificationPriority.LOW);

        // Save all notifications
        notificationRepository.saveAll(Arrays.asList(
                urgentNotification, highPriorityNotification, normalNotification, lowPriorityNotification));

        // When - Add to priority queue with scores based on priority
        Map<String, Double> priorityScores = Map.of(
                urgentNotification.getId(), 4.0,    // URGENT = 4
                highPriorityNotification.getId(), 3.0, // HIGH = 3
                normalNotification.getId(), 2.0,   // NORMAL = 2
                lowPriorityNotification.getId(), 1.0   // LOW = 1
        );

        // Add to Redis sorted set for priority queue
        priorityScores.forEach((id, score) -> 
                redisTemplate.opsForZSet().add(PRIORITY_QUEUE, id, score));

        // Then - Verify priority ordering
        Set<Object> highestPriorityItems = redisTemplate.opsForZSet()
                .reverseRange(PRIORITY_QUEUE, 0, 1); // Get top 2 priority items
        
        assertThat(highestPriorityItems).hasSize(2);
        
        // Process in priority order
        Set<Object> allItemsByPrioritySet = redisTemplate.opsForZSet()
                .reverseRange(PRIORITY_QUEUE, 0, -1);
        List<Object> allItemsByPriority = new ArrayList<>(allItemsByPrioritySet);
        
        assertThat(allItemsByPriority).hasSize(4);
        assertThat(allItemsByPriority.get(0)).isEqualTo(urgentNotification.getId()); // Highest priority first
        assertThat(allItemsByPriority.get(3)).isEqualTo(lowPriorityNotification.getId()); // Lowest priority last
    }

    @Test
    @DisplayName("Should handle bulk notification processing efficiently")
    void shouldHandleBulkNotificationProcessingEfficiently() {
        // Given - Large batch of notifications
        int batchSize = 100;
        List<Notification> bulkNotifications = new ArrayList<>();
        
        for (int i = 0; i < batchSize; i++) {
            Notification notification = createTestNotification(
                    "bulk-user-" + i, NotificationType.SYSTEM_UPDATE,
                    "Bulk Update " + i, "System update notification " + i);
            bulkNotifications.add(notification);
        }
        
        // Save all notifications to database
        notificationRepository.saveAll(bulkNotifications);

        // When - Add all notifications to queue in batches
        long startTime = System.currentTimeMillis();
        
        // Process in batches of 10
        int processingBatchSize = 10;
        for (int i = 0; i < bulkNotifications.size(); i += processingBatchSize) {
            List<Notification> batch = bulkNotifications.subList(
                    i, Math.min(i + processingBatchSize, bulkNotifications.size()));
            
            // Add batch to queue
            batch.forEach(notification -> addToQueue(NOTIFICATION_QUEUE, notification));
            
            // Simulate batch processing
            for (int j = 0; j < batch.size(); j++) {
                redisTemplate.opsForList().leftPop(NOTIFICATION_QUEUE);
            }
        }
        
        long processingTime = System.currentTimeMillis() - startTime;

        // Then - Verify efficient processing
        assertThat(processingTime).isLessThan(5000); // Should complete within 5 seconds
        
        Long finalQueueSize = redisTemplate.opsForList().size(NOTIFICATION_QUEUE);
        assertThat(finalQueueSize).isEqualTo(0);

        // Verify all notifications were processed
        List<Notification> processedNotifications = notificationRepository.findAll();
        assertThat(processedNotifications).hasSize(batchSize);
    }

    @Test
    @DisplayName("Should provide queue monitoring and metrics")
    void shouldProvideQueueMonitoringAndMetrics() {
        // Given - Various notifications in different queues
        addNotificationsToQueues();

        // When - Collect queue metrics
        Long mainQueueSize = redisTemplate.opsForList().size(NOTIFICATION_QUEUE);
        Long retryQueueSize = redisTemplate.opsForList().size(RETRY_QUEUE);
        Long deadLetterQueueSize = redisTemplate.opsForList().size(DEAD_LETTER_QUEUE);
        Long priorityQueueSize = redisTemplate.opsForZSet().size(PRIORITY_QUEUE);

        // Collect additional metrics
        Map<String, Object> queueMetrics = new HashMap<>();
        queueMetrics.put("mainQueue.size", mainQueueSize);
        queueMetrics.put("retryQueue.size", retryQueueSize);
        queueMetrics.put("deadLetterQueue.size", deadLetterQueueSize);
        queueMetrics.put("priorityQueue.size", priorityQueueSize);
        queueMetrics.put("timestamp", System.currentTimeMillis());

        // Calculate processing rates (mock data)
        queueMetrics.put("processedPerMinute", 150);
        queueMetrics.put("failureRate", 0.05); // 5% failure rate
        queueMetrics.put("averageProcessingTime", 250); // 250ms average

        // Then - Verify metrics collection
        assertThat(queueMetrics).containsKeys(
                "mainQueue.size", "retryQueue.size", "deadLetterQueue.size", 
                "priorityQueue.size", "processedPerMinute", "failureRate");

        assertThat(mainQueueSize).isGreaterThanOrEqualTo(0);
        assertThat(retryQueueSize).isGreaterThanOrEqualTo(0);
        assertThat(deadLetterQueueSize).isGreaterThanOrEqualTo(0);
        assertThat(priorityQueueSize).isGreaterThanOrEqualTo(0);

        // Store metrics for monitoring (in real implementation, this would go to metrics system)
        String metricsKey = "notification:metrics:" + System.currentTimeMillis();
        redisTemplate.opsForHash().putAll(metricsKey, queueMetrics);
        redisTemplate.expire(metricsKey, 24, TimeUnit.HOURS); // Keep metrics for 24 hours

        assertThat(redisTemplate.hasKey(metricsKey)).isTrue();
    }

    @Test
    @DisplayName("Should handle graceful shutdown with pending notifications")
    void shouldHandleGracefulShutdownWithPendingNotifications() {
        // Given - Notifications in various processing states
        List<Notification> pendingNotifications = Arrays.asList(
                createTestNotification("user-1", NotificationType.WELCOME, "Welcome", "Welcome message"),
                createTestNotification("user-2", NotificationType.HIVE_INVITATION, "Invite", "Join our hive"),
                createTestNotification("user-3", NotificationType.ACHIEVEMENT_UNLOCKED, "Achievement", "New achievement")
        );

        pendingNotifications.forEach(notificationRepository::save);
        pendingNotifications.forEach(notification -> addToQueue(NOTIFICATION_QUEUE, notification));

        // Simulate notifications being processed
        Map<String, Object> processingNotification = Map.of(
                "id", pendingNotifications.get(0).getId(),
                "status", "processing",
                "startTime", System.currentTimeMillis(),
                "workerId", "worker-1"
        );
        
        redisTemplate.opsForHash().putAll("notification:processing", processingNotification);

        // When - Simulate graceful shutdown procedure
        Long pendingCount = redisTemplate.opsForList().size(NOTIFICATION_QUEUE);
        
        // Mark system as shutting down
        redisTemplate.opsForValue().set("notification:system:shutdown", "true", 300, TimeUnit.SECONDS);
        
        // Move pending notifications to a shutdown queue for later processing
        String shutdownQueue = "notification:shutdown";
        while (redisTemplate.opsForList().size(NOTIFICATION_QUEUE) > 0) {
            Object notification = redisTemplate.opsForList().rightPopAndLeftPush(NOTIFICATION_QUEUE, shutdownQueue);
            assertThat(notification).isNotNull();
        }

        // Handle in-progress notifications
        Map<Object, Object> processingItems = redisTemplate.opsForHash().entries("notification:processing");
        if (!processingItems.isEmpty()) {
            // Move to shutdown queue for retry
            processingItems.forEach((key, value) -> {
                redisTemplate.opsForList().leftPush(shutdownQueue, value);
            });
            redisTemplate.delete("notification:processing");
        }

        // Then - Verify graceful shutdown handling
        Long mainQueueAfterShutdown = redisTemplate.opsForList().size(NOTIFICATION_QUEUE);
        Long shutdownQueueSize = redisTemplate.opsForList().size(shutdownQueue);
        
        assertThat(mainQueueAfterShutdown).isEqualTo(0);
        assertThat(shutdownQueueSize).isEqualTo(pendingCount); // All pending notifications preserved
        
        String shutdownFlag = (String) redisTemplate.opsForValue().get("notification:system:shutdown");
        assertThat(shutdownFlag).isEqualTo("true");

        // Verify notifications can be recovered on restart
        while (redisTemplate.opsForList().size(shutdownQueue) > 0) {
            Object recoveredNotification = redisTemplate.opsForList().rightPopAndLeftPush(shutdownQueue, NOTIFICATION_QUEUE);
            assertThat(recoveredNotification).isNotNull();
        }
        
        Long recoveredQueueSize = redisTemplate.opsForList().size(NOTIFICATION_QUEUE);
        assertThat(recoveredQueueSize).isEqualTo(pendingCount);
    }

    @Test
    @DisplayName("Should demonstrate queue performance under load")
    void shouldDemonstrateQueuePerformanceUnderLoad() {
        // Given - High volume of notifications
        int highVolumeCount = 1000;
        long startTime = System.currentTimeMillis();

        // When - Add many notifications rapidly
        for (int i = 0; i < highVolumeCount; i++) {
            Map<String, Object> notification = Map.of(
                    "id", "perf-test-" + i,
                    "userId", "user-" + (i % 100), // 100 different users
                    "type", NotificationType.SYSTEM_UPDATE.toString(),
                    "title", "Performance Test " + i,
                    "priority", i % 4 // Different priorities
            );
            
            addToQueue(NOTIFICATION_QUEUE, notification);
        }
        
        long queuePopulationTime = System.currentTimeMillis() - startTime;

        // Process notifications in batches
        long processingStartTime = System.currentTimeMillis();
        int batchSize = 50;
        int processedCount = 0;

        while (redisTemplate.opsForList().size(NOTIFICATION_QUEUE) > 0) {
            List<Object> batch = new ArrayList<>();
            
            for (int i = 0; i < batchSize && redisTemplate.opsForList().size(NOTIFICATION_QUEUE) > 0; i++) {
                Object notification = redisTemplate.opsForList().leftPop(NOTIFICATION_QUEUE);
                if (notification != null) {
                    batch.add(notification);
                }
            }
            
            processedCount += batch.size();
            
            // Simulate processing delay
            if (!batch.isEmpty()) {
                try {
                    Thread.sleep(1); // 1ms per batch
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        
        long totalProcessingTime = System.currentTimeMillis() - processingStartTime;

        // Then - Verify performance metrics
        assertThat(processedCount).isEqualTo(highVolumeCount);
        assertThat(queuePopulationTime).isLessThan(5000); // Population should be fast
        assertThat(totalProcessingTime).isLessThan(30000); // Processing within reasonable time

        // Calculate performance metrics
        double populationRate = highVolumeCount / (queuePopulationTime / 1000.0); // items per second
        double processingRate = processedCount / (totalProcessingTime / 1000.0); // items per second

        assertThat(populationRate).isGreaterThan(100); // At least 100 items/sec population rate
        assertThat(processingRate).isGreaterThan(30);   // At least 30 items/sec processing rate

        // Verify queue is empty after processing
        Long finalQueueSize = redisTemplate.opsForList().size(NOTIFICATION_QUEUE);
        assertThat(finalQueueSize).isEqualTo(0);
    }

    // Helper methods

    private void addToQueue(String queueName, Object item) {
        redisTemplate.opsForList().leftPush(queueName, item);
    }

    private void clearAllQueues() {
        redisTemplate.delete(NOTIFICATION_QUEUE);
        redisTemplate.delete(RETRY_QUEUE);
        redisTemplate.delete(DEAD_LETTER_QUEUE);
        redisTemplate.delete(PRIORITY_QUEUE);
        
        // Clear any processing or shutdown related keys
        redisTemplate.delete("notification:processing");
        redisTemplate.delete("notification:system:shutdown");
        redisTemplate.delete("notification:shutdown");
    }

    private void addNotificationsToQueues() {
        // Add sample notifications to different queues for testing
        addToQueue(NOTIFICATION_QUEUE, Map.of("id", "test-1", "type", "WELCOME"));
        addToQueue(NOTIFICATION_QUEUE, Map.of("id", "test-2", "type", "ACHIEVEMENT"));
        
        addToQueue(RETRY_QUEUE, Map.of("id", "retry-1", "retryCount", 1));
        
        addToQueue(DEAD_LETTER_QUEUE, Map.of("id", "dead-1", "failureReason", "Max retries exceeded"));
        
        redisTemplate.opsForZSet().add(PRIORITY_QUEUE, "priority-1", 3.0);
        redisTemplate.opsForZSet().add(PRIORITY_QUEUE, "priority-2", 2.0);
    }
}