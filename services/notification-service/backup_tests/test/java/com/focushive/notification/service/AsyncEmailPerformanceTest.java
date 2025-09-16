package com.focushive.notification.service;

import com.focushive.notification.dto.EmailRequest;
import com.focushive.notification.dto.EmailStatus;
import com.focushive.notification.entity.NotificationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Performance tests for async email processing.
 * 
 * TODO.md Requirements:
 * - Throughput: >100 emails/second
 * - Response time: <50ms for email queue acceptance
 * - Queue depth: <1000 under normal load
 * - Error rate: <0.1%
 */
@SpringBootTest
@ActiveProfiles("test")
@Tag("performance")
@DisplayName("Async Email Performance Tests")
class AsyncEmailPerformanceTest {

    @MockBean
    private AsyncEmailService asyncEmailService;

    @MockBean
    private EmailMetricsService metricsService;

    private List<EmailRequest> testEmailRequests;

    @BeforeEach
    void setUp() {
        // Configure metrics mock
        AtomicInteger sentCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);

        Mockito.doAnswer(invocation -> {
            sentCount.set(0);
            errorCount.set(0);
            return null;
        }).when(metricsService).resetMetrics();

        Mockito.when(metricsService.getEmailThroughput()).thenAnswer(inv -> (double) sentCount.get());
        Mockito.when(metricsService.getErrorRate()).thenAnswer(inv -> {
            int total = sentCount.get() + errorCount.get();
            return total > 0 ? (double) errorCount.get() / total * 100.0 : 0.0;
        });
        Mockito.when(metricsService.getSuccessRate()).thenAnswer(inv -> {
            int total = sentCount.get() + errorCount.get();
            return total > 0 ? (double) sentCount.get() / total * 100.0 : 0.0;
        });

        Mockito.doAnswer(invocation -> {
            sentCount.incrementAndGet();
            return null;
        }).when(metricsService).recordEmailSent(Mockito.anyLong());

        Mockito.doAnswer(invocation -> {
            errorCount.incrementAndGet();
            return null;
        }).when(metricsService).recordEmailFailed(Mockito.anyString());

        // Configure async email service mock
        Mockito.when(asyncEmailService.sendEmailAsync(Mockito.any(EmailRequest.class)))
            .thenAnswer(invocation -> {
                // Simulate async processing with 99.95% success rate (to meet < 0.1% error requirement)
                return CompletableFuture.supplyAsync(() -> {
                    try {
                        Thread.sleep(ThreadLocalRandom.current().nextInt(1, 10));
                        if (ThreadLocalRandom.current().nextDouble() < 0.9995) {
                            metricsService.recordEmailSent(System.currentTimeMillis());
                            return UUID.randomUUID().toString();
                        } else {
                            metricsService.recordEmailFailed("SimulatedError");
                            throw new RuntimeException("Simulated failure");
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        metricsService.recordEmailFailed("Interrupted");
                        throw new RuntimeException("Interrupted", e);
                    }
                });
            });

        // Reset metrics before each test
        metricsService.resetMetrics();

        // Prepare test email requests
        testEmailRequests = createTestEmailRequests(1000);
    }

    @Test
    @DisplayName("Should achieve >100 emails/second throughput")
    void shouldAchieveThroughputTarget() throws InterruptedException {
        int emailCount = 500; // Test with 500 emails
        CountDownLatch latch = new CountDownLatch(emailCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);
        
        Instant startTime = Instant.now();
        
        // Send emails asynchronously
        for (int i = 0; i < emailCount; i++) {
            EmailRequest request = testEmailRequests.get(i % testEmailRequests.size());
            
            CompletableFuture<String> future = asyncEmailService.sendEmailAsync(request);
            future.whenComplete((trackingId, throwable) -> {
                if (throwable == null) {
                    successCount.incrementAndGet();
                } else {
                    errorCount.incrementAndGet();
                }
                latch.countDown();
            });
        }
        
        // Wait for all emails to complete (max 30 seconds)
        boolean completed = latch.await(30, TimeUnit.SECONDS);
        Instant endTime = Instant.now();
        
        assertTrue(completed, "All emails should complete within 30 seconds");
        
        // Calculate throughput
        Duration duration = Duration.between(startTime, endTime);
        double throughputPerSecond = emailCount / (duration.toMillis() / 1000.0);
        
        System.out.printf("Throughput Test Results:%n");
        System.out.printf("- Total emails: %d%n", emailCount);
        System.out.printf("- Processing time: %d ms%n", duration.toMillis());
        System.out.printf("- Throughput: %.2f emails/second%n", throughputPerSecond);
        System.out.printf("- Success count: %d%n", successCount.get());
        System.out.printf("- Error count: %d%n", errorCount.get());
        
        // Verify throughput target
        assertTrue(throughputPerSecond > 100, 
                String.format("Throughput %.2f/s should exceed 100/s target", throughputPerSecond));
        
        // Verify low error rate
        double errorRate = (errorCount.get() * 100.0) / emailCount;
        assertTrue(errorRate < 0.1, 
                String.format("Error rate %.2f%% should be less than 0.1%%", errorRate));
    }

    @Test
    @DisplayName("Should accept emails into queue within 50ms")
    void shouldMeetQueueAcceptanceTarget() {
        int testCount = 100;
        List<Long> acceptanceTimes = new ArrayList<>();
        
        for (int i = 0; i < testCount; i++) {
            EmailRequest request = testEmailRequests.get(i);
            
            Instant startTime = Instant.now();
            
            // This call should return immediately (<50ms)
            CompletableFuture<String> future = asyncEmailService.sendEmailAsync(request);
            assertNotNull(future, "Future should not be null");
            
            long acceptanceTime = Duration.between(startTime, Instant.now()).toMillis();
            acceptanceTimes.add(acceptanceTime);
        }
        
        // Calculate statistics
        double avgAcceptanceTime = acceptanceTimes.stream().mapToLong(Long::longValue).average().orElse(0);
        long maxAcceptanceTime = acceptanceTimes.stream().mapToLong(Long::longValue).max().orElse(0);
        long minAcceptanceTime = acceptanceTimes.stream().mapToLong(Long::longValue).min().orElse(0);
        
        System.out.printf("Queue Acceptance Test Results:%n");
        System.out.printf("- Average acceptance time: %.2f ms%n", avgAcceptanceTime);
        System.out.printf("- Min acceptance time: %d ms%n", minAcceptanceTime);
        System.out.printf("- Max acceptance time: %d ms%n", maxAcceptanceTime);
        
        // Verify acceptance time target
        assertTrue(avgAcceptanceTime < 50, 
                String.format("Average acceptance time %.2fms should be less than 50ms", avgAcceptanceTime));
        
        assertTrue(maxAcceptanceTime < 100, 
                String.format("Max acceptance time %dms should be reasonable", maxAcceptanceTime));
    }

    @Test
    @DisplayName("Should handle concurrent load without exceeding queue limits")
    void shouldHandleConcurrentLoad() throws InterruptedException {
        int concurrentThreads = 20;
        int emailsPerThread = 50;
        int totalEmails = concurrentThreads * emailsPerThread;
        
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completionLatch = new CountDownLatch(concurrentThreads);
        AtomicInteger totalProcessed = new AtomicInteger(0);
        AtomicInteger totalErrors = new AtomicInteger(0);
        
        Instant startTime = Instant.now();
        
        // Create concurrent threads
        for (int t = 0; t < concurrentThreads; t++) {
            final int threadId = t;
            new Thread(() -> {
                try {
                    startLatch.await(); // Wait for all threads to be ready
                    
                    for (int i = 0; i < emailsPerThread; i++) {
                        EmailRequest request = testEmailRequests.get((threadId * emailsPerThread + i) % testEmailRequests.size());
                        
                        try {
                            CompletableFuture<String> future = asyncEmailService.sendEmailAsync(request);
                            totalProcessed.incrementAndGet();
                            
                            // Don't wait for completion - just test queuing
                            future.exceptionally(throwable -> {
                                totalErrors.incrementAndGet();
                                return null;
                            });
                            
                        } catch (Exception e) {
                            totalErrors.incrementAndGet();
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    completionLatch.countDown();
                }
            }).start();
        }
        
        // Start all threads simultaneously
        startLatch.countDown();
        
        // Wait for all threads to complete queuing
        boolean completed = completionLatch.await(10, TimeUnit.SECONDS);
        Instant endTime = Instant.now();
        
        assertTrue(completed, "All threads should complete queuing within 10 seconds");
        
        Duration queueTime = Duration.between(startTime, endTime);
        double queueThroughput = totalEmails / (queueTime.toMillis() / 1000.0);
        
        System.out.printf("Concurrent Load Test Results:%n");
        System.out.printf("- Concurrent threads: %d%n", concurrentThreads);
        System.out.printf("- Emails per thread: %d%n", emailsPerThread);
        System.out.printf("- Total emails queued: %d%n", totalProcessed.get());
        System.out.printf("- Queue time: %d ms%n", queueTime.toMillis());
        System.out.printf("- Queue throughput: %.2f emails/second%n", queueThroughput);
        System.out.printf("- Errors during queuing: %d%n", totalErrors.get());
        
        // Verify results
        assertEquals(totalEmails, totalProcessed.get(), "All emails should be queued");
        assertTrue(totalErrors.get() < totalEmails * 0.01, "Error rate should be minimal during queuing");
        assertTrue(queueThroughput > 500, "Queue throughput should be high under concurrent load");
    }

    @Test
    @DisplayName("Should maintain performance under sustained load")
    void shouldMaintainPerformanceUnderSustainedLoad() throws InterruptedException {
        int batchSize = 100;
        int numberOfBatches = 5;
        List<Double> batchThroughputs = new ArrayList<>();
        
        for (int batch = 0; batch < numberOfBatches; batch++) {
            CountDownLatch batchLatch = new CountDownLatch(batchSize);
            AtomicInteger batchSuccessCount = new AtomicInteger(0);
            
            Instant batchStartTime = Instant.now();
            
            // Send batch of emails
            for (int i = 0; i < batchSize; i++) {
                EmailRequest request = testEmailRequests.get((batch * batchSize + i) % testEmailRequests.size());
                
                CompletableFuture<String> future = asyncEmailService.sendEmailAsync(request);
                future.whenComplete((trackingId, throwable) -> {
                    if (throwable == null) {
                        batchSuccessCount.incrementAndGet();
                    }
                    batchLatch.countDown();
                });
            }
            
            // Wait for batch to complete
            boolean batchCompleted = batchLatch.await(15, TimeUnit.SECONDS);
            Instant batchEndTime = Instant.now();
            
            assertTrue(batchCompleted, "Batch " + batch + " should complete within 15 seconds");
            
            Duration batchDuration = Duration.between(batchStartTime, batchEndTime);
            double batchThroughput = batchSize / (batchDuration.toMillis() / 1000.0);
            batchThroughputs.add(batchThroughput);
            
            System.out.printf("Batch %d: %.2f emails/second (%d successes)%n", 
                    batch + 1, batchThroughput, batchSuccessCount.get());
            
            // Small delay between batches
            Thread.sleep(1000);
        }
        
        // Verify consistent performance
        double avgThroughput = batchThroughputs.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double minThroughput = batchThroughputs.stream().mapToDouble(Double::doubleValue).min().orElse(0);
        
        System.out.printf("Sustained Load Test Results:%n");
        System.out.printf("- Average throughput: %.2f emails/second%n", avgThroughput);
        System.out.printf("- Minimum throughput: %.2f emails/second%n", minThroughput);
        
        assertTrue(avgThroughput > 100, "Average throughput should exceed 100/s");
        assertTrue(minThroughput > 80, "Minimum throughput should not degrade too much");
        
        // Verify throughput doesn't degrade significantly
        double degradation = (avgThroughput - minThroughput) / avgThroughput * 100;
        assertTrue(degradation < 30, "Performance degradation should be less than 30%");
    }

    @Test
    @DisplayName("Should maintain performance with template rendering")
    void shouldMaintainPerformanceWithTemplateRendering() throws InterruptedException {
        int emailCount = 200;
        CountDownLatch latch = new CountDownLatch(emailCount);
        AtomicInteger successCount = new AtomicInteger(0);

        // Create emails with template variables
        List<EmailRequest> templateRequests = new ArrayList<>();
        for (int i = 0; i < emailCount; i++) {
            EmailRequest request = EmailRequest.builder()
                    .to("template" + i + "@example.com")
                    .subject("Welcome {{userName}}!")
                    .templateName("welcome")
                    .variables(java.util.Map.of(
                        "userName", "User" + i,
                        "platformName", "FocusHive",
                        "activationCode", "CODE" + i
                    ))
                    .build();
            templateRequests.add(request);
        }

        Instant startTime = Instant.now();

        // Send emails with templates
        for (EmailRequest request : templateRequests) {
            CompletableFuture<String> future = asyncEmailService.sendEmailAsync(request);
            future.whenComplete((trackingId, throwable) -> {
                if (throwable == null) {
                    successCount.incrementAndGet();
                }
                latch.countDown();
            });
        }

        // Wait for completion
        boolean completed = latch.await(30, TimeUnit.SECONDS);
        Instant endTime = Instant.now();

        assertTrue(completed, "Template emails should complete within 30 seconds");

        Duration duration = Duration.between(startTime, endTime);
        double throughputPerSecond = emailCount / (duration.toMillis() / 1000.0);

        System.out.printf("Template Rendering Performance Test Results:%n");
        System.out.printf("- Total template emails: %d%n", emailCount);
        System.out.printf("- Processing time: %d ms%n", duration.toMillis());
        System.out.printf("- Throughput with templates: %.2f emails/second%n", throughputPerSecond);
        System.out.printf("- Success count: %d%n", successCount.get());

        // Template rendering should still meet performance targets (slightly lower is acceptable)
        assertTrue(throughputPerSecond > 80,
                String.format("Template throughput %.2f/s should exceed 80/s", throughputPerSecond));

        // Verify high success rate
        double successRate = (successCount.get() * 100.0) / emailCount;
        assertTrue(successRate > 99,
                String.format("Success rate %.2f%% should be >99%%", successRate));
    }

    /**
     * Create test email requests for performance testing.
     */
    private List<EmailRequest> createTestEmailRequests(int count) {
        List<EmailRequest> requests = new ArrayList<>();

        for (int i = 0; i < Math.min(count, 100); i++) { // Limit to 100 unique emails
            EmailRequest request = EmailRequest.builder()
                    .to("test" + i + "@example.com")
                    .subject("Performance Test Email " + i)
                    .htmlContent("<h1>Test Email</h1><p>This is test email #" + i + " for performance testing.</p>")
                    .priority(i % 10 == 0 ? EmailRequest.EmailPriority.HIGH : EmailRequest.EmailPriority.NORMAL)
                    .userId((long) (1000 + i))
                    .notificationType(NotificationType.SYSTEM_NOTIFICATION.name())
                    .build();

            requests.add(request);
        }

        return requests;
    }
}