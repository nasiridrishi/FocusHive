package com.focushive.performance;

import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.socket.*;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * WebSocket Performance Tests for FocusHive Real-time Features.
 * 
 * Tests WebSocket performance for:
 * - Connection establishment time
 * - Message latency measurement  
 * - Concurrent connection limits
 * - Message throughput
 * - Reconnection performance
 * - Broadcasting efficiency
 * - Presence system updates
 * - Chat message delivery
 * - Timer synchronization
 * 
 * Performance Targets:
 * - WebSocket connection time: < 2 seconds
 * - Message latency P50: < 50ms
 * - Message latency P95: < 100ms
 * - Message latency P99: < 200ms
 * - Concurrent connections: 10,000+
 * - Message delivery success rate: > 99%
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("performance-test")
@DisplayName("WebSocket Performance Tests")
class WebSocketPerformanceTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("focushive_websocket_test")
            .withUsername("ws_user")
            .withPassword("ws_pass")
            .withReuse(true);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        
        // WebSocket optimizations
        registry.add("server.tomcat.max-connections", () -> "20000");
        registry.add("server.tomcat.threads.max", () -> "200");
        registry.add("spring.websocket.servlet.allowed-origins", () -> "*");
    }

    @LocalServerPort
    private int port;

    private StandardWebSocketClient webSocketClient;
    private List<WebSocketPerformanceResult> performanceResults;

    @BeforeEach
    void setUp() {
        webSocketClient = new StandardWebSocketClient();
        performanceResults = new ArrayList<>();
    }

    @AfterEach
    void generateWebSocketReport() {
        if (!performanceResults.isEmpty()) {
            generateWebSocketPerformanceReport();
        }
    }

    @Nested
    @DisplayName("Connection Performance Tests")
    class ConnectionPerformanceTests {

        @Test
        @DisplayName("Single WebSocket connection establishment time")
        void testSingleConnectionPerformance() throws Exception {
            long startTime = System.nanoTime();
            
            URI webSocketUri = URI.create("ws://localhost:" + port + "/ws");
            WebSocketSession session = connectWebSocket(webSocketUri, new TestWebSocketHandler());
            
            long connectionTime = (System.nanoTime() - startTime) / 1_000_000; // Convert to ms
            
            assertThat(session.isOpen())
                .describedAs("WebSocket connection should be established")
                .isTrue();
            
            assertThat(connectionTime)
                .describedAs("WebSocket connection should be fast")
                .isLessThanOrEqualTo(2000); // 2 seconds
            
            session.close();
            
            System.out.println("Single connection establishment time: " + connectionTime + "ms");
        }

        @Test
        @DisplayName("Concurrent connection establishment - 100 connections")
        void testConcurrentConnectionEstablishment() throws Exception {
            int connectionCount = 100;
            ExecutorService executor = Executors.newFixedThreadPool(connectionCount);
            List<CompletableFuture<ConnectionResult>> futures = new ArrayList<>();
            
            URI webSocketUri = URI.create("ws://localhost:" + port + "/ws");
            
            for (int i = 0; i < connectionCount; i++) {
                final int connectionId = i;
                CompletableFuture<ConnectionResult> future = CompletableFuture.supplyAsync(() -> {
                    try {
                        long startTime = System.nanoTime();
                        WebSocketSession session = connectWebSocket(webSocketUri, new TestWebSocketHandler());
                        long connectionTime = (System.nanoTime() - startTime) / 1_000_000;
                        
                        return new ConnectionResult(connectionId, connectionTime, session.isOpen(), session);
                    } catch (Exception e) {
                        return new ConnectionResult(connectionId, -1, false, null);
                    }
                }, executor);
                
                futures.add(future);
            }
            
            // Collect results
            List<ConnectionResult> results = new ArrayList<>();
            for (CompletableFuture<ConnectionResult> future : futures) {
                try {
                    ConnectionResult result = future.get(10, TimeUnit.SECONDS);
                    results.add(result);
                } catch (Exception e) {
                    results.add(new ConnectionResult(-1, -1, false, null));
                }
            }
            
            executor.shutdown();
            
            // Analyze results
            int successfulConnections = 0;
            List<Long> connectionTimes = new ArrayList<>();
            
            for (ConnectionResult result : results) {
                if (result.success) {
                    successfulConnections++;
                    connectionTimes.add(result.connectionTime);
                    
                    // Close successful connections
                    try {
                        if (result.session != null) {
                            result.session.close();
                        }
                    } catch (Exception e) {
                        // Ignore close errors
                    }
                }
            }
            
            double successRate = (double) successfulConnections / connectionCount;
            
            assertThat(successRate)
                .describedAs("Most concurrent connections should succeed")
                .isGreaterThanOrEqualTo(0.95); // 95% success rate
            
            if (!connectionTimes.isEmpty()) {
                Collections.sort(connectionTimes);
                long p95ConnectionTime = connectionTimes.get((int) (connectionTimes.size() * 0.95));
                
                assertThat(p95ConnectionTime)
                    .describedAs("P95 connection time should be reasonable")
                    .isLessThanOrEqualTo(5000); // 5 seconds P95
            }
            
            System.out.println(String.format(
                "Concurrent connections: %d/%d (%.1f%%), P95 time: %dms",
                successfulConnections, connectionCount, successRate * 100,
                connectionTimes.isEmpty() ? 0 : connectionTimes.get((int) (connectionTimes.size() * 0.95))
            ));
        }

        @Test
        @DisplayName("Connection limit test - find maximum concurrent connections")
        void testConnectionLimits() throws Exception {
            org.junit.jupiter.api.Assumptions.assumeFalse(
                System.getenv("CI") != null, "Skipping connection limit test in CI environment");
            
            int maxAttempts = 2000;
            int batchSize = 100;
            List<WebSocketSession> activeSessions = new ArrayList<>();
            
            URI webSocketUri = URI.create("ws://localhost:" + port + "/ws");
            
            for (int batch = 0; batch < maxAttempts; batch += batchSize) {
                int currentBatch = Math.min(batchSize, maxAttempts - batch);
                ExecutorService executor = Executors.newFixedThreadPool(currentBatch);
                List<CompletableFuture<WebSocketSession>> futures = new ArrayList<>();
                
                for (int i = 0; i < currentBatch; i++) {
                    CompletableFuture<WebSocketSession> future = CompletableFuture.supplyAsync(() -> {
                        try {
                            return connectWebSocket(webSocketUri, new TestWebSocketHandler());
                        } catch (Exception e) {
                            return null;
                        }
                    }, executor);
                    
                    futures.add(future);
                }
                
                // Collect batch results
                int successfulInBatch = 0;
                for (CompletableFuture<WebSocketSession> future : futures) {
                    try {
                        WebSocketSession session = future.get(5, TimeUnit.SECONDS);
                        if (session != null && session.isOpen()) {
                            activeSessions.add(session);
                            successfulInBatch++;
                        }
                    } catch (Exception e) {
                        // Connection failed
                    }
                }
                
                executor.shutdown();
                
                System.out.println(String.format(
                    "Batch %d-%d: %d/%d successful, Total active: %d",
                    batch, batch + currentBatch - 1, successfulInBatch, currentBatch, activeSessions.size()
                ));
                
                // If success rate drops significantly, we've likely hit the limit
                if (successfulInBatch < currentBatch * 0.5) {
                    System.out.println("Connection limit reached around " + activeSessions.size() + " connections");
                    break;
                }
                
                Thread.sleep(1000); // Brief pause between batches
            }
            
            // Cleanup
            int cleanupCount = 0;
            for (WebSocketSession session : activeSessions) {
                try {
                    session.close();
                    cleanupCount++;
                } catch (Exception e) {
                    // Ignore cleanup errors
                }
            }
            
            System.out.println("Maximum concurrent connections achieved: " + activeSessions.size());
            System.out.println("Cleaned up: " + cleanupCount + " connections");
            
            // System should handle at least 1000 concurrent connections
            assertThat(activeSessions.size())
                .describedAs("System should handle at least 1000 concurrent WebSocket connections")
                .isGreaterThanOrEqualTo(1000);
        }
    }

    @Nested
    @DisplayName("Message Latency Tests")  
    class MessageLatencyTests {

        @Test
        @DisplayName("Message round-trip latency measurement")
        void testMessageLatency() throws Exception {
            URI webSocketUri = URI.create("ws://localhost:" + port + "/ws");
            
            LatencyTestHandler handler = new LatencyTestHandler();
            WebSocketSession session = connectWebSocket(webSocketUri, handler);
            
            // Wait for connection to stabilize
            Thread.sleep(1000);
            
            int messageCount = 100;
            List<Long> latencies = new ArrayList<>();
            
            for (int i = 0; i < messageCount; i++) {
                long startTime = System.nanoTime();
                
                String message = "latency-test-" + i + "-" + startTime;
                session.sendMessage(new TextMessage(message));
                
                // Wait for response
                String response = handler.waitForMessage(5000); // 5 second timeout
                
                if (response != null && response.contains("latency-test-" + i)) {
                    long endTime = System.nanoTime();
                    long latency = (endTime - startTime) / 1_000_000; // Convert to ms
                    latencies.add(latency);
                } else {
                    System.out.println("Message " + i + " failed or timed out");
                }
                
                Thread.sleep(50); // Small delay between messages
            }
            
            session.close();
            
            // Analyze latency results
            if (!latencies.isEmpty()) {
                Collections.sort(latencies);
                long p50 = latencies.get(latencies.size() / 2);
                long p95 = latencies.get((int) (latencies.size() * 0.95));
                long p99 = latencies.get((int) (latencies.size() * 0.99));
                long avg = (long) latencies.stream().mapToLong(Long::longValue).average().orElse(0);
                
                WebSocketPerformanceResult result = new WebSocketPerformanceResult(
                    "message-latency", messageCount, latencies.size(), p50, p95, p99, avg);
                performanceResults.add(result);
                
                assertThat(p50)
                    .describedAs("P50 message latency should be low")
                    .isLessThanOrEqualTo(PerformanceTestUtils.WEBSOCKET_LATENCY_P50);
                
                assertThat(p95)
                    .describedAs("P95 message latency should be acceptable")
                    .isLessThanOrEqualTo(PerformanceTestUtils.WEBSOCKET_LATENCY_P95);
                
                assertThat(p99)
                    .describedAs("P99 message latency should be reasonable")
                    .isLessThanOrEqualTo(PerformanceTestUtils.WEBSOCKET_LATENCY_P99);
                
                System.out.println(String.format(
                    "Message latency: P50=%dms, P95=%dms, P99=%dms, Avg=%dms, Success=%d/%d",
                    p50, p95, p99, avg, latencies.size(), messageCount
                ));
            }
        }

        @Test
        @DisplayName("Concurrent message latency under load")
        void testConcurrentMessageLatency() throws Exception {
            int connectionCount = 50;
            int messagesPerConnection = 20;
            ExecutorService executor = Executors.newFixedThreadPool(connectionCount);
            List<CompletableFuture<List<Long>>> futures = new ArrayList<>();
            
            URI webSocketUri = URI.create("ws://localhost:" + port + "/ws");
            
            for (int i = 0; i < connectionCount; i++) {
                final int connectionId = i;
                CompletableFuture<List<Long>> future = CompletableFuture.supplyAsync(() -> {
                    try {
                        LatencyTestHandler handler = new LatencyTestHandler();
                        WebSocketSession session = connectWebSocket(webSocketUri, handler);
                        Thread.sleep(100); // Connection stabilization
                        
                        List<Long> connectionLatencies = new ArrayList<>();
                        
                        for (int j = 0; j < messagesPerConnection; j++) {
                            long startTime = System.nanoTime();
                            
                            String message = "concurrent-test-" + connectionId + "-" + j + "-" + startTime;
                            session.sendMessage(new TextMessage(message));
                            
                            String response = handler.waitForMessage(3000);
                            
                            if (response != null && response.contains("concurrent-test-" + connectionId + "-" + j)) {
                                long endTime = System.nanoTime();
                                long latency = (endTime - startTime) / 1_000_000;
                                connectionLatencies.add(latency);
                            }
                            
                            Thread.sleep(100);
                        }
                        
                        session.close();
                        return connectionLatencies;
                        
                    } catch (Exception e) {
                        return new ArrayList<>();
                    }
                }, executor);
                
                futures.add(future);
            }
            
            // Collect all latencies
            List<Long> allLatencies = new ArrayList<>();
            int totalMessages = 0;
            int successfulMessages = 0;
            
            for (CompletableFuture<List<Long>> future : futures) {
                try {
                    List<Long> connectionLatencies = future.get(30, TimeUnit.SECONDS);
                    allLatencies.addAll(connectionLatencies);
                    successfulMessages += connectionLatencies.size();
                } catch (Exception e) {
                    // Handle timeout or error
                }
                totalMessages += messagesPerConnection;
            }
            
            executor.shutdown();
            
            double successRate = (double) successfulMessages / (connectionCount * messagesPerConnection);
            
            assertThat(successRate)
                .describedAs("Message delivery success rate should be high under concurrent load")
                .isGreaterThanOrEqualTo(0.90); // 90% success rate under load
            
            if (!allLatencies.isEmpty()) {
                Collections.sort(allLatencies);
                long p95 = allLatencies.get((int) (allLatencies.size() * 0.95));
                
                assertThat(p95)
                    .describedAs("P95 latency should be reasonable under concurrent load")
                    .isLessThanOrEqualTo(PerformanceTestUtils.WEBSOCKET_LATENCY_P95 * 2); // Allow 2x under load
                
                System.out.println(String.format(
                    "Concurrent message test: %d connections, Success: %d/%d (%.1f%%), P95: %dms",
                    connectionCount, successfulMessages, connectionCount * messagesPerConnection, 
                    successRate * 100, p95
                ));
            }
        }
    }

    @Nested
    @DisplayName("Throughput and Broadcasting Tests")
    class ThroughputBroadcastTests {

        @Test
        @DisplayName("Message throughput test - single connection")
        void testMessageThroughput() throws Exception {
            URI webSocketUri = URI.create("ws://localhost:" + port + "/ws");
            ThroughputTestHandler handler = new ThroughputTestHandler();
            WebSocketSession session = connectWebSocket(webSocketUri, handler);
            
            Thread.sleep(1000); // Stabilization
            
            int messageCount = 1000;
            long startTime = System.currentTimeMillis();
            
            for (int i = 0; i < messageCount; i++) {
                String message = "throughput-test-" + i;
                session.sendMessage(new TextMessage(message));
            }
            
            // Wait for all messages to be processed
            Thread.sleep(5000);
            
            long endTime = System.currentTimeMillis();
            double duration = (endTime - startTime) / 1000.0; // Convert to seconds
            double throughput = messageCount / duration;
            
            session.close();
            
            assertThat(throughput)
                .describedAs("Message throughput should be high")
                .isGreaterThan(100.0); // 100 messages per second minimum
            
            System.out.println(String.format(
                "Message throughput: %.1f messages/second (%d messages in %.1fs)",
                throughput, messageCount, duration
            ));
        }

        @Test  
        @DisplayName("Broadcasting efficiency test")
        void testBroadcastingEfficiency() throws Exception {
            int subscriberCount = 100;
            int broadcastCount = 50;
            
            URI webSocketUri = URI.create("ws://localhost:" + port + "/ws");
            List<WebSocketSession> subscribers = new ArrayList<>();
            List<BroadcastTestHandler> handlers = new ArrayList<>();
            
            // Create subscriber connections
            for (int i = 0; i < subscriberCount; i++) {
                BroadcastTestHandler handler = new BroadcastTestHandler();
                WebSocketSession session = connectWebSocket(webSocketUri, handler);
                
                subscribers.add(session);
                handlers.add(handler);
            }
            
            Thread.sleep(2000); // Allow connections to stabilize
            
            // Perform broadcasts
            long startTime = System.currentTimeMillis();
            
            for (int i = 0; i < broadcastCount; i++) {
                String broadcastMessage = "broadcast-" + i + "-" + System.currentTimeMillis();
                
                // Simulate broadcast by sending to all subscribers
                for (WebSocketSession session : subscribers) {
                    if (session.isOpen()) {
                        session.sendMessage(new TextMessage(broadcastMessage));
                    }
                }
                
                Thread.sleep(100); // Small delay between broadcasts
            }
            
            // Wait for message delivery
            Thread.sleep(3000);
            
            long endTime = System.currentTimeMillis();
            
            // Analyze broadcast results
            int totalExpectedMessages = subscriberCount * broadcastCount;
            int totalReceivedMessages = handlers.stream().mapToInt(h -> h.getReceivedCount()).sum();
            double deliveryRate = (double) totalReceivedMessages / totalExpectedMessages;
            
            // Cleanup connections
            for (WebSocketSession session : subscribers) {
                try {
                    session.close();
                } catch (Exception e) {
                    // Ignore close errors
                }
            }
            
            assertThat(deliveryRate)
                .describedAs("Broadcast delivery rate should be high")
                .isGreaterThanOrEqualTo(0.95); // 95% delivery rate
            
            double broadcastDuration = (endTime - startTime) / 1000.0;
            double broadcastThroughput = (totalReceivedMessages) / broadcastDuration;
            
            System.out.println(String.format(
                "Broadcasting: %d subscribers, %d broadcasts, %.1f%% delivery, %.1f msg/s throughput",
                subscriberCount, broadcastCount, deliveryRate * 100, broadcastThroughput
            ));
        }
    }

    @Nested
    @DisplayName("Real-time Feature Performance")
    class RealTimeFeatureTests {

        @Test
        @DisplayName("Presence system update latency")
        void testPresenceUpdateLatency() throws Exception {
            URI webSocketUri = URI.create("ws://localhost:" + port + "/ws/presence");
            
            PresenceTestHandler handler = new PresenceTestHandler();
            WebSocketSession session = connectWebSocket(webSocketUri, handler);
            
            Thread.sleep(1000);
            
            int updateCount = 50;
            List<Long> latencies = new ArrayList<>();
            
            for (int i = 0; i < updateCount; i++) {
                long startTime = System.nanoTime();
                
                // Send presence update
                String presenceUpdate = "{\"type\":\"presence_update\",\"status\":\"active\",\"timestamp\":" + startTime + "}";
                session.sendMessage(new TextMessage(presenceUpdate));
                
                // Wait for acknowledgment
                String response = handler.waitForMessage(2000);
                
                if (response != null && response.contains("presence_ack")) {
                    long endTime = System.nanoTime();
                    long latency = (endTime - startTime) / 1_000_000;
                    latencies.add(latency);
                }
                
                Thread.sleep(200); // Realistic presence update interval
            }
            
            session.close();
            
            if (!latencies.isEmpty()) {
                Collections.sort(latencies);
                long p95 = latencies.get((int) (latencies.size() * 0.95));
                
                assertThat(p95)
                    .describedAs("Presence update P95 latency should be low")
                    .isLessThanOrEqualTo(200); // 200ms for presence updates
                
                System.out.println(String.format(
                    "Presence updates: %d/%d successful, P95 latency: %dms",
                    latencies.size(), updateCount, p95
                ));
            }
        }

        @Test
        @DisplayName("Timer synchronization performance")
        void testTimerSynchronizationPerformance() throws Exception {
            int participantCount = 20;
            URI webSocketUri = URI.create("ws://localhost:" + port + "/ws/timer");
            
            List<WebSocketSession> participants = new ArrayList<>();
            List<TimerTestHandler> handlers = new ArrayList<>();
            
            // Create participant connections
            for (int i = 0; i < participantCount; i++) {
                TimerTestHandler handler = new TimerTestHandler();
                WebSocketSession session = connectWebSocket(webSocketUri, handler);
                
                participants.add(session);
                handlers.add(handler);
            }
            
            Thread.sleep(2000);
            
            // Simulate timer synchronization events
            long startTime = System.currentTimeMillis();
            
            String[] timerEvents = {"start", "pause", "resume", "stop"};
            
            for (String event : timerEvents) {
                long eventTime = System.currentTimeMillis();
                String timerMessage = "{\"type\":\"timer_sync\",\"event\":\"" + event + "\",\"timestamp\":" + eventTime + "}";
                
                // Broadcast timer event to all participants
                for (WebSocketSession session : participants) {
                    if (session.isOpen()) {
                        session.sendMessage(new TextMessage(timerMessage));
                    }
                }
                
                Thread.sleep(2000); // Time between timer events
            }
            
            long endTime = System.currentTimeMillis();
            
            // Analyze synchronization
            int expectedMessages = participantCount * timerEvents.length;
            int receivedMessages = handlers.stream().mapToInt(h -> h.getReceivedCount()).sum();
            double syncRate = (double) receivedMessages / expectedMessages;
            
            // Cleanup
            for (WebSocketSession session : participants) {
                try {
                    session.close();
                } catch (Exception e) {
                    // Ignore
                }
            }
            
            assertThat(syncRate)
                .describedAs("Timer synchronization rate should be high")
                .isGreaterThanOrEqualTo(0.98); // 98% synchronization rate
            
            System.out.println(String.format(
                "Timer sync: %d participants, %d/%d messages delivered (%.1f%%), Duration: %dms",
                participantCount, receivedMessages, expectedMessages, syncRate * 100, endTime - startTime
            ));
        }
    }

    // Helper methods and classes

    private WebSocketSession connectWebSocket(URI uri, WebSocketHandler handler) throws Exception {
        return webSocketClient.doHandshake(handler, null, uri).get(10, TimeUnit.SECONDS);
    }

    private void generateWebSocketPerformanceReport() {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("WEBSOCKET PERFORMANCE TEST REPORT");
        System.out.println("=".repeat(60));
        
        for (WebSocketPerformanceResult result : performanceResults) {
            System.out.println("Test: " + result.testName);
            System.out.println("  Messages: " + result.successful + "/" + result.total);
            System.out.println("  Success Rate: " + String.format("%.1f%%", (double) result.successful / result.total * 100));
            System.out.println("  Latency P50: " + result.p50Latency + "ms");
            System.out.println("  Latency P95: " + result.p95Latency + "ms");
            System.out.println("  Latency P99: " + result.p99Latency + "ms");
            System.out.println("  Average: " + result.avgLatency + "ms");
            System.out.println();
        }
        
        System.out.println("=".repeat(60));
    }

    // Test handler implementations

    static class TestWebSocketHandler implements WebSocketHandler {
        @Override
        public void afterConnectionEstablished(WebSocketSession session) throws Exception {
            // Connection established
        }

        @Override
        public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
            // Echo back the message
            if (message instanceof TextMessage) {
                String payload = ((TextMessage) message).getPayload();
                session.sendMessage(new TextMessage("echo: " + payload));
            }
        }

        @Override
        public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
            // Handle transport error
        }

        @Override
        public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
            // Connection closed
        }

        @Override
        public boolean supportsPartialMessages() {
            return false;
        }
    }

    static class LatencyTestHandler implements WebSocketHandler {
        private final BlockingQueue<String> messageQueue = new LinkedBlockingQueue<>();

        @Override
        public void afterConnectionEstablished(WebSocketSession session) throws Exception {}

        @Override
        public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
            if (message instanceof TextMessage) {
                String payload = ((TextMessage) message).getPayload();
                messageQueue.offer(payload);
            }
        }

        @Override
        public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {}

        @Override
        public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {}

        @Override
        public boolean supportsPartialMessages() {
            return false;
        }

        public String waitForMessage(long timeoutMs) throws InterruptedException {
            return messageQueue.poll(timeoutMs, TimeUnit.MILLISECONDS);
        }
    }

    static class ThroughputTestHandler implements WebSocketHandler {
        private final AtomicInteger receivedCount = new AtomicInteger(0);

        @Override
        public void afterConnectionEstablished(WebSocketSession session) throws Exception {}

        @Override
        public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
            receivedCount.incrementAndGet();
        }

        @Override
        public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {}

        @Override
        public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {}

        @Override
        public boolean supportsPartialMessages() {
            return false;
        }

        public int getReceivedCount() {
            return receivedCount.get();
        }
    }

    static class BroadcastTestHandler implements WebSocketHandler {
        private final AtomicInteger receivedCount = new AtomicInteger(0);

        @Override
        public void afterConnectionEstablished(WebSocketSession session) throws Exception {}

        @Override
        public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
            receivedCount.incrementAndGet();
        }

        @Override
        public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {}

        @Override
        public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {}

        @Override
        public boolean supportsPartialMessages() {
            return false;
        }

        public int getReceivedCount() {
            return receivedCount.get();
        }
    }

    static class PresenceTestHandler implements WebSocketHandler {
        private final BlockingQueue<String> messageQueue = new LinkedBlockingQueue<>();

        @Override
        public void afterConnectionEstablished(WebSocketSession session) throws Exception {}

        @Override
        public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
            if (message instanceof TextMessage) {
                String payload = ((TextMessage) message).getPayload();
                messageQueue.offer(payload);
            }
        }

        @Override
        public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {}

        @Override
        public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {}

        @Override
        public boolean supportsPartialMessages() {
            return false;
        }

        public String waitForMessage(long timeoutMs) throws InterruptedException {
            return messageQueue.poll(timeoutMs, TimeUnit.MILLISECONDS);
        }
    }

    static class TimerTestHandler implements WebSocketHandler {
        private final AtomicInteger receivedCount = new AtomicInteger(0);

        @Override
        public void afterConnectionEstablished(WebSocketSession session) throws Exception {}

        @Override
        public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
            receivedCount.incrementAndGet();
        }

        @Override
        public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {}

        @Override
        public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {}

        @Override
        public boolean supportsPartialMessages() {
            return false;
        }

        public int getReceivedCount() {
            return receivedCount.get();
        }
    }

    // Helper classes

    static class ConnectionResult {
        final int connectionId;
        final long connectionTime;
        final boolean success;
        final WebSocketSession session;

        ConnectionResult(int connectionId, long connectionTime, boolean success, WebSocketSession session) {
            this.connectionId = connectionId;
            this.connectionTime = connectionTime;
            this.success = success;
            this.session = session;
        }
    }

    static class WebSocketPerformanceResult {
        final String testName;
        final int total;
        final int successful;
        final long p50Latency;
        final long p95Latency;
        final long p99Latency;
        final long avgLatency;

        WebSocketPerformanceResult(String testName, int total, int successful, 
                                 long p50, long p95, long p99, long avg) {
            this.testName = testName;
            this.total = total;
            this.successful = successful;
            this.p50Latency = p50;
            this.p95Latency = p95;
            this.p99Latency = p99;
            this.avgLatency = avg;
        }
    }
}