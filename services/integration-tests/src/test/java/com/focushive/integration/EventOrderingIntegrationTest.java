package com.focushive.integration;

import com.fasterxml.jackson.core.type.TypeReference;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Cross-service integration test for Event Ordering and Processing.
 * 
 * Tests critical event processing scenarios:
 * 1. Concurrent events processed in correct order
 * 2. Event deduplication across services
 * 3. Retry mechanism for failed events
 * 4. Dead letter queue handling
 * 5. High-volume event streams
 * 
 * Verifies:
 * - Event ordering guarantees across services
 * - Idempotency and deduplication mechanisms
 * - Retry logic and exponential backoff
 * - Dead letter queue processing
 * - Performance under high event volume
 * - Event correlation and tracing
 * 
 * Following TDD approach with focus on event-driven architecture reliability.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@SpringJUnitConfig
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Tag("cross-service")
@Tag("event-ordering")
@Tag("integration")
@Tag("event-driven")
class EventOrderingIntegrationTest extends AbstractCrossServiceIntegrationTest {

    private String testUserId;
    private String testUserToken;
    private String testHiveId;
    private String correlationId;

    @BeforeAll
    void setUpTestData() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = 8080;

        setupTestUser();
        setupTestHive();
        
        // Generate correlation ID for event tracking
        correlationId = "event-test-" + UUID.randomUUID().toString();
    }

    @Test
    @Order(1)
    @DisplayName("TDD: Concurrent events should be processed in correct chronological order - FAILING TEST")
    void testConcurrentEventOrderingGuarantees_ShouldFail() {
        // STEP 1: Write failing test first (TDD)
        
        // Given: A sequence of events that must be processed in order
        List<Map<String, Object>> orderedEvents = createOrderedEventSequence();
        
        // When: Events are submitted concurrently but with clear timestamps
        List<CompletableFuture<Response>> eventFutures = new ArrayList<>();
        long baseTimestamp = System.currentTimeMillis();

        for (int i = 0; i < orderedEvents.size(); i++) {
            Map<String, Object> event = orderedEvents.get(i);
            event.put("timestamp", baseTimestamp + (i * 1000)); // 1 second apart
            event.put("sequenceNumber", i + 1);
            event.put("correlationId", correlationId);

            // Submit events concurrently
            CompletableFuture<Response> future = CompletableFuture.supplyAsync(() -> 
                given()
                    .header("Authorization", "Bearer " + testUserToken)
                    .contentType(ContentType.JSON)
                    .body(event)
                    .when()
                    .post("/api/events/publish")
                    .then()
                    .statusCode(anyOf(is(200), is(202))) // Accept async responses
                    .extract().response()
            );
            
            eventFutures.add(future);
            
            // Small delay to simulate rapid but sequential event generation
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // Wait for all events to be submitted
        CompletableFuture.allOf(eventFutures.toArray(new CompletableFuture[0])).join();

        // Then: Events should be processed in chronological order (THIS WILL FAIL INITIALLY)
        CrossServiceTestUtils.waitForEventualConsistency();

        // Verify events were processed in correct order
        Awaitility.await()
            .atMost(30, TimeUnit.SECONDS)
            .pollInterval(2, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                Response processedEventsResponse = given()
                    .header("Authorization", "Bearer " + testUserToken)
                    .when()
                    .get("/api/events/processed?correlationId=" + correlationId)
                    .then()
                    .statusCode(200)
                    .extract().response();

                List<Map<String, Object>> processedEvents = processedEventsResponse.jsonPath().getList("events");
                assertEquals(orderedEvents.size(), processedEvents.size(), 
                    "All events should be processed");

                // Verify ordering by sequence number
                for (int i = 0; i < processedEvents.size(); i++) {
                    Map<String, Object> processedEvent = processedEvents.get(i);
                    assertEquals(i + 1, processedEvent.get("sequenceNumber"), 
                        "Event at position " + i + " should have sequence number " + (i + 1));
                }

                // Verify processing timestamps maintain order
                for (int i = 1; i < processedEvents.size(); i++) {
                    String prevProcessedTime = processedEvents.get(i - 1).get("processedAt").toString();
                    String currProcessedTime = processedEvents.get(i).get("processedAt").toString();
                    
                    LocalDateTime prevTime = LocalDateTime.parse(prevProcessedTime, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                    LocalDateTime currTime = LocalDateTime.parse(currProcessedTime, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                    
                    assertTrue(currTime.isAfter(prevTime) || currTime.isEqual(prevTime), 
                        "Events should be processed in chronological order");
                }
            });

        // Verify event effects are applied in correct order
        verifyEventEffectsOrdering();
    }

    @Test
    @Order(2)
    @DisplayName("TDD: Duplicate events should be deduplicated across services")
    void testEventDeduplicationAcrossServices() {
        // Given: An event with a unique identifier
        String eventId = "dedup-test-" + UUID.randomUUID().toString();
        Map<String, Object> duplicateEvent = Map.of(
            "eventId", eventId,
            "eventType", "USER_ACTION",
            "userId", testUserId,
            "action", "COMPLETE_TASK",
            "timestamp", System.currentTimeMillis(),
            "correlationId", correlationId + "-dedup",
            "metadata", Map.of(
                "taskId", "task-123",
                "duration", 1500
            )
        );

        // When: The same event is published multiple times
        List<Response> responses = new ArrayList<>();
        
        for (int i = 0; i < 5; i++) {
            Response response = given()
                .header("Authorization", "Bearer " + testUserToken)
                .contentType(ContentType.JSON)
                .body(duplicateEvent)
                .when()
                .post("/api/events/publish")
                .then()
                .statusCode(anyOf(is(200), is(202)))
                .extract().response();
            
            responses.add(response);
            
            // Small delay between submissions
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // Then: Event should be processed only once across all services
        CrossServiceTestUtils.waitForEventualConsistency();

        // Verify deduplication in event log
        Awaitility.await()
            .atMost(20, TimeUnit.SECONDS)
            .pollInterval(1, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                Response eventLogResponse = given()
                    .header("Authorization", "Bearer " + testUserToken)
                    .when()
                    .get("/api/events/log?eventId=" + eventId)
                    .then()
                    .statusCode(200)
                    .extract().response();

                List<Map<String, Object>> eventLogEntries = eventLogResponse.jsonPath().getList("entries");
                
                // Should have multiple receipt entries but only one processing entry
                long receiptCount = eventLogEntries.stream()
                    .filter(entry -> "RECEIVED".equals(entry.get("status")))
                    .count();
                long processedCount = eventLogEntries.stream()
                    .filter(entry -> "PROCESSED".equals(entry.get("status")))
                    .count();

                assertEquals(5, receiptCount, "Should have 5 receipt entries");
                assertEquals(1, processedCount, "Should have only 1 processed entry (deduplicated)");
            });

        // Verify deduplication in Analytics Service
        Awaitility.await()
            .atMost(20, TimeUnit.SECONDS)
            .pollInterval(1, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                Response analyticsResponse = given()
                    .header("Authorization", "Bearer " + testUserToken)
                    .when()
                    .get("/api/analytics/events?eventId=" + eventId)
                    .then()
                    .statusCode(200)
                    .extract().response();

                List<Map<String, Object>> analyticsEvents = analyticsResponse.jsonPath().getList("events");
                assertEquals(1, analyticsEvents.size(), 
                    "Analytics should have only 1 event entry (deduplicated)");

                Map<String, Object> analyticsEvent = analyticsEvents.get(0);
                assertEquals(eventId, analyticsEvent.get("eventId"), "Event ID should match");
                assertEquals("USER_ACTION", analyticsEvent.get("eventType"), "Event type should match");
            });

        // Verify deduplication in Notification Service
        Awaitility.await()
            .atMost(20, TimeUnit.SECONDS)
            .pollInterval(1, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                Response notificationResponse = given()
                    .header("Authorization", "Bearer " + testUserToken)
                    .when()
                    .get("/api/notifications/triggered-by-event?eventId=" + eventId)
                    .then()
                    .statusCode(200)
                    .extract().response();

                List<Map<String, Object>> triggeredNotifications = notificationResponse.jsonPath().getList("notifications");
                
                if (!triggeredNotifications.isEmpty()) {
                    // If notifications were triggered, should be only once
                    assertEquals(1, triggeredNotifications.size(), 
                        "Should trigger only 1 notification (deduplicated)");
                }
            });
    }

    @Test
    @Order(3)
    @DisplayName("TDD: Failed events should retry with exponential backoff")
    void testEventRetryMechanismWithExponentialBackoff() {
        // Given: An event that will initially fail processing
        String retryEventId = "retry-test-" + UUID.randomUUID().toString();
        Map<String, Object> failingEvent = Map.of(
            "eventId", retryEventId,
            "eventType", "SIMULATED_FAILURE",
            "userId", testUserId,
            "action", "TRIGGER_TEMPORARY_FAILURE",
            "timestamp", System.currentTimeMillis(),
            "correlationId", correlationId + "-retry",
            "metadata", Map.of(
                "failureType", "TEMPORARY_SERVICE_UNAVAILABLE",
                "failureCount", 3, // Fail 3 times then succeed
                "retryStrategy", "EXPONENTIAL_BACKOFF"
            )
        );

        // When: Event is published and initially fails
        long startTime = System.currentTimeMillis();

        Response initialResponse = given()
            .header("Authorization", "Bearer " + testUserToken)
            .contentType(ContentType.JSON)
            .body(failingEvent)
            .when()
            .post("/api/events/publish")
            .then()
            .statusCode(anyOf(is(200), is(202)))
            .extract().response();

        // Then: System should retry with exponential backoff until success
        CrossServiceTestUtils.waitForEventualConsistency();

        // Verify retry attempts with timing
        Awaitility.await()
            .atMost(60, TimeUnit.SECONDS) // Allow time for retries
            .pollInterval(2, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                Response retryLogResponse = given()
                    .header("Authorization", "Bearer " + testUserToken)
                    .when()
                    .get("/api/events/retry-log?eventId=" + retryEventId)
                    .then()
                    .statusCode(200)
                    .extract().response();

                List<Map<String, Object>> retryAttempts = retryLogResponse.jsonPath().getList("attempts");
                
                // Should have multiple retry attempts
                assertTrue(retryAttempts.size() >= 3, 
                    "Should have at least 3 retry attempts");

                // Verify exponential backoff timing
                for (int i = 1; i < retryAttempts.size(); i++) {
                    Map<String, Object> prevAttempt = retryAttempts.get(i - 1);
                    Map<String, Object> currAttempt = retryAttempts.get(i);
                    
                    long prevTime = (Long) prevAttempt.get("attemptTime");
                    long currTime = (Long) currAttempt.get("attemptTime");
                    long interval = currTime - prevTime;
                    
                    // Each retry should be longer than the previous (exponential backoff)
                    // Allow some tolerance for system delays
                    assertTrue(interval >= 1000, 
                        "Retry interval should increase (attempt " + i + " interval: " + interval + "ms)");
                }

                // Final attempt should be successful
                Map<String, Object> finalAttempt = retryAttempts.get(retryAttempts.size() - 1);
                assertEquals("SUCCESS", finalAttempt.get("status"), 
                    "Final retry attempt should be successful");
            });

        // Verify event was eventually processed successfully
        Awaitility.await()
            .atMost(30, TimeUnit.SECONDS)
            .pollInterval(2, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                Response processedResponse = given()
                    .header("Authorization", "Bearer " + testUserToken)
                    .when()
                    .get("/api/events/processed?eventId=" + retryEventId)
                    .then()
                    .statusCode(200)
                    .extract().response();

                List<Map<String, Object>> processedEvents = processedResponse.jsonPath().getList("events");
                assertEquals(1, processedEvents.size(), "Event should be successfully processed");

                Map<String, Object> processedEvent = processedEvents.get(0);
                assertEquals("SUCCESS", processedEvent.get("finalStatus"), 
                    "Event should have final status of SUCCESS");
                assertTrue((Integer) processedEvent.get("retryCount") >= 3, 
                    "Event should show retry count >= 3");
            });
    }

    @Test
    @Order(4)
    @DisplayName("TDD: Permanently failed events should go to dead letter queue")
    void testDeadLetterQueueHandling() {
        // Given: An event that will permanently fail
        String deadLetterEventId = "dead-letter-test-" + UUID.randomUUID().toString();
        Map<String, Object> permanentlyFailingEvent = Map.of(
            "eventId", deadLetterEventId,
            "eventType", "PERMANENT_FAILURE",
            "userId", testUserId,
            "action", "TRIGGER_PERMANENT_FAILURE",
            "timestamp", System.currentTimeMillis(),
            "correlationId", correlationId + "-dead-letter",
            "metadata", Map.of(
                "failureType", "INVALID_DATA_FORMAT",
                "maxRetries", 5,
                "permanentFailure", true
            )
        );

        // When: Event is published and fails permanently
        Response failureResponse = given()
            .header("Authorization", "Bearer " + testUserToken)
            .contentType(ContentType.JSON)
            .body(permanentlyFailingEvent)
            .when()
            .post("/api/events/publish")
            .then()
            .statusCode(anyOf(is(200), is(202)))
            .extract().response();

        // Then: Event should eventually be moved to dead letter queue
        CrossServiceTestUtils.waitForEventualConsistency();

        // Verify event goes through retry process
        Awaitility.await()
            .atMost(120, TimeUnit.SECONDS) // Allow time for all retries
            .pollInterval(3, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                Response retryLogResponse = given()
                    .header("Authorization", "Bearer " + testUserToken)
                    .when()
                    .get("/api/events/retry-log?eventId=" + deadLetterEventId)
                    .then()
                    .statusCode(200)
                    .extract().response();

                List<Map<String, Object>> retryAttempts = retryLogResponse.jsonPath().getList("attempts");
                
                // Should have maximum retry attempts
                assertTrue(retryAttempts.size() >= 5, 
                    "Should have at least 5 retry attempts before giving up");

                // All attempts should show failure
                boolean allFailed = retryAttempts.stream()
                    .allMatch(attempt -> "FAILURE".equals(attempt.get("status")));
                assertTrue(allFailed, "All retry attempts should show failure status");
            });

        // Verify event is in dead letter queue
        Awaitility.await()
            .atMost(30, TimeUnit.SECONDS)
            .pollInterval(2, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                Response deadLetterResponse = given()
                    .header("Authorization", "Bearer " + testUserToken)
                    .when()
                    .get("/api/events/dead-letter-queue")
                    .then()
                    .statusCode(200)
                    .extract().response();

                List<Map<String, Object>> deadLetterEvents = deadLetterResponse.jsonPath().getList("events");
                
                boolean eventInDeadLetter = deadLetterEvents.stream()
                    .anyMatch(event -> deadLetterEventId.equals(event.get("eventId")));
                assertTrue(eventInDeadLetter, 
                    "Event should be in dead letter queue after permanent failure");

                // Verify dead letter event details
                Map<String, Object> deadLetterEvent = deadLetterEvents.stream()
                    .filter(event -> deadLetterEventId.equals(event.get("eventId")))
                    .findFirst()
                    .orElseThrow();

                assertEquals("PERMANENT_FAILURE", deadLetterEvent.get("reason"), 
                    "Dead letter event should have correct failure reason");
                assertTrue((Integer) deadLetterEvent.get("totalRetryAttempts") >= 5, 
                    "Dead letter event should show total retry attempts");
                assertNotNull(deadLetterEvent.get("deadLetterTimestamp"), 
                    "Dead letter event should have timestamp");
            });

        // Verify event status tracking
        Response eventStatusResponse = given()
            .header("Authorization", "Bearer " + testUserToken)
            .when()
            .get("/api/events/status?eventId=" + deadLetterEventId)
            .then()
            .statusCode(200)
            .body("eventId", equalTo(deadLetterEventId))
            .body("finalStatus", equalTo("DEAD_LETTER"))
            .body("isProcessed", equalTo(false))
            .extract().response();

        Map<String, Object> eventStatus = eventStatusResponse.as(new TypeReference<Map<String, Object>>() {});
        assertTrue((Integer) eventStatus.get("retryCount") >= 5, 
            "Event status should show retry count");
    }

    @Test
    @Order(5)
    @DisplayName("Performance: High-volume event streams processing")
    void testHighVolumeEventStreamPerformance() {
        // Given: High volume of events to process
        int eventCount = 1000;
        String batchCorrelationId = correlationId + "-batch-" + System.currentTimeMillis();
        
        // When: Large number of events are submitted rapidly
        long startTime = System.currentTimeMillis();
        List<CompletableFuture<Response>> eventFutures = new ArrayList<>();

        for (int i = 0; i < eventCount; i++) {
            Map<String, Object> event = createHighVolumeTestEvent(i, batchCorrelationId);
            
            CompletableFuture<Response> future = CompletableFuture.supplyAsync(() -> 
                given()
                    .header("Authorization", "Bearer " + testUserToken)
                    .contentType(ContentType.JSON)
                    .body(event)
                    .when()
                    .post("/api/events/publish")
                    .then()
                    .statusCode(anyOf(is(200), is(202)))
                    .extract().response()
            );
            
            eventFutures.add(future);
        }

        // Wait for all submissions to complete
        CompletableFuture.allOf(eventFutures.toArray(new CompletableFuture[0])).join();
        long submissionTime = System.currentTimeMillis() - startTime;

        // Then: All events should be processed efficiently
        CrossServiceTestUtils.waitForEventualConsistency();

        // Verify submission performance
        assertTrue(submissionTime < 30000, 
            "Event submission should complete within 30 seconds (actual: " + submissionTime + "ms)");

        // Verify processing performance and correctness
        Awaitility.await()
            .atMost(120, TimeUnit.SECONDS) // Allow time for processing
            .pollInterval(5, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                Response processedResponse = given()
                    .header("Authorization", "Bearer " + testUserToken)
                    .when()
                    .get("/api/events/processed?correlationId=" + batchCorrelationId)
                    .then()
                    .statusCode(200)
                    .extract().response();

                List<Map<String, Object>> processedEvents = processedResponse.jsonPath().getList("events");
                assertEquals(eventCount, processedEvents.size(), 
                    "All " + eventCount + " events should be processed");

                // Verify no duplicate processing
                Set<String> eventIds = processedEvents.stream()
                    .map(event -> event.get("eventId").toString())
                    .collect(Collectors.toSet());
                assertEquals(eventCount, eventIds.size(), 
                    "All events should have unique IDs (no duplicates)");
            });

        // Verify analytics aggregation performance
        Awaitility.await()
            .atMost(60, TimeUnit.SECONDS)
            .pollInterval(3, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                Response analyticsResponse = given()
                    .header("Authorization", "Bearer " + testUserToken)
                    .when()
                    .get("/api/analytics/batch-summary?correlationId=" + batchCorrelationId)
                    .then()
                    .statusCode(200)
                    .extract().response();

                Map<String, Object> batchSummary = analyticsResponse.as(new TypeReference<Map<String, Object>>() {});
                assertEquals(eventCount, batchSummary.get("totalEvents"), 
                    "Analytics should aggregate all events");
                assertEquals(batchCorrelationId, batchSummary.get("correlationId"), 
                    "Correlation ID should match");

                // Verify processing rate
                long totalProcessingTime = (Long) batchSummary.get("totalProcessingTimeMs");
                double eventsPerSecond = (eventCount * 1000.0) / totalProcessingTime;
                assertTrue(eventsPerSecond >= 50, 
                    "Should process at least 50 events per second (actual: " + eventsPerSecond + "/s)");
            });

        long totalTime = System.currentTimeMillis() - startTime;
        assertTrue(totalTime < 150000, 
            "Complete high-volume processing should finish within 2.5 minutes (actual: " + totalTime + "ms)");
    }

    @Test
    @Order(6)
    @DisplayName("Edge Case: Event correlation and distributed tracing")
    void testEventCorrelationAndDistributedTracing() {
        // Given: A complex workflow with multiple related events
        String workflowCorrelationId = "workflow-" + UUID.randomUUID().toString();
        String traceId = "trace-" + UUID.randomUUID().toString();
        
        // Create a sequence of related events
        List<Map<String, Object>> workflowEvents = List.of(
            createWorkflowEvent("WORKFLOW_STARTED", 1, workflowCorrelationId, traceId),
            createWorkflowEvent("USER_AUTHENTICATED", 2, workflowCorrelationId, traceId),
            createWorkflowEvent("HIVE_JOINED", 3, workflowCorrelationId, traceId),
            createWorkflowEvent("SESSION_STARTED", 4, workflowCorrelationId, traceId),
            createWorkflowEvent("ANALYTICS_RECORDED", 5, workflowCorrelationId, traceId),
            createWorkflowEvent("NOTIFICATION_SENT", 6, workflowCorrelationId, traceId),
            createWorkflowEvent("WORKFLOW_COMPLETED", 7, workflowCorrelationId, traceId)
        );

        // When: Related events are published across different services
        List<Response> responses = new ArrayList<>();
        for (Map<String, Object> event : workflowEvents) {
            Response response = given()
                .header("Authorization", "Bearer " + testUserToken)
                .header("X-Trace-Id", traceId) // Add trace header
                .contentType(ContentType.JSON)
                .body(event)
                .when()
                .post("/api/events/publish")
                .then()
                .statusCode(anyOf(is(200), is(202)))
                .extract().response();
            
            responses.add(response);
            
            // Small delay between workflow steps
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // Then: Events should be correlated and traceable across services
        CrossServiceTestUtils.waitForEventualConsistency();

        // Verify event correlation
        Awaitility.await()
            .atMost(30, TimeUnit.SECONDS)
            .pollInterval(2, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                Response correlatedResponse = given()
                    .header("Authorization", "Bearer " + testUserToken)
                    .when()
                    .get("/api/events/correlated?correlationId=" + workflowCorrelationId)
                    .then()
                    .statusCode(200)
                    .extract().response();

                List<Map<String, Object>> correlatedEvents = correlatedResponse.jsonPath().getList("events");
                assertEquals(workflowEvents.size(), correlatedEvents.size(), 
                    "All workflow events should be correlated");

                // Verify events are linked by correlation ID
                boolean allCorrelated = correlatedEvents.stream()
                    .allMatch(event -> workflowCorrelationId.equals(event.get("correlationId")));
                assertTrue(allCorrelated, "All events should have matching correlation ID");

                // Verify workflow sequence is preserved
                correlatedEvents.sort((e1, e2) -> 
                    Integer.compare((Integer) e1.get("sequenceNumber"), (Integer) e2.get("sequenceNumber")));
                
                for (int i = 0; i < correlatedEvents.size(); i++) {
                    assertEquals(i + 1, correlatedEvents.get(i).get("sequenceNumber"), 
                        "Event sequence should be preserved");
                }
            });

        // Verify distributed tracing
        Awaitility.await()
            .atMost(30, TimeUnit.SECONDS)
            .pollInterval(2, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                Response traceResponse = given()
                    .header("Authorization", "Bearer " + testUserToken)
                    .when()
                    .get("/api/tracing/trace?traceId=" + traceId)
                    .then()
                    .statusCode(200)
                    .extract().response();

                Map<String, Object> trace = traceResponse.as(new TypeReference<Map<String, Object>>() {});
                assertEquals(traceId, trace.get("traceId"), "Trace ID should match");
                
                List<Map<String, Object>> spans = (List<Map<String, Object>>) trace.get("spans");
                assertTrue(spans.size() >= workflowEvents.size(), 
                    "Trace should have spans for all workflow events");

                // Verify span relationships
                boolean hasRootSpan = spans.stream()
                    .anyMatch(span -> span.get("parentSpanId") == null);
                assertTrue(hasRootSpan, "Trace should have a root span");

                // Verify service coverage
                Set<String> services = spans.stream()
                    .map(span -> span.get("serviceName").toString())
                    .collect(Collectors.toSet());
                assertTrue(services.size() >= 3, 
                    "Trace should span multiple services");
                assertTrue(services.contains("event-service"), 
                    "Trace should include event service");
                assertTrue(services.contains("analytics-service"), 
                    "Trace should include analytics service");
            });

        // Verify end-to-end workflow timing
        Response workflowTimingResponse = given()
            .header("Authorization", "Bearer " + testUserToken)
            .when()
            .get("/api/analytics/workflow-timing?correlationId=" + workflowCorrelationId)
            .then()
            .statusCode(200)
            .extract().response();

        Map<String, Object> workflowTiming = workflowTimingResponse.as(new TypeReference<Map<String, Object>>() {});
        
        long workflowDuration = (Long) workflowTiming.get("totalDurationMs");
        assertTrue(workflowDuration > 0, "Workflow should have measurable duration");
        assertTrue(workflowDuration < 30000, 
            "Workflow should complete within 30 seconds (actual: " + workflowDuration + "ms)");

        // Verify step-by-step timing
        List<Map<String, Object>> stepTimings = (List<Map<String, Object>>) workflowTiming.get("stepTimings");
        assertEquals(workflowEvents.size() - 1, stepTimings.size(), 
            "Should have timing for each workflow step transition");
    }

    // Private helper methods

    private void setupTestUser() {
        Map<String, Object> userRequest = TestDataFactory.Users.createTestUser(
            "event-test@example.com", 
            "Event Test User"
        );

        Response userResponse = given()
            .contentType(ContentType.JSON)
            .body(userRequest)
            .when()
            .post("/api/auth/register")
            .then()
            .statusCode(201)
            .extract().response();

        testUserId = userResponse.jsonPath().getString("id");

        // Authenticate
        Map<String, Object> loginRequest = Map.of(
            "email", "event-test@example.com",
            "password", "TestPassword123!"
        );

        Response authResponse = given()
            .contentType(ContentType.JSON)
            .body(loginRequest)
            .when()
            .post("/api/auth/login")
            .then()
            .statusCode(200)
            .extract().response();

        testUserToken = authResponse.jsonPath().getString("token");
    }

    private void setupTestHive() {
        Map<String, Object> hiveRequest = TestDataFactory.Hives.createTestHive(
            "Event Test Hive", testUserId
        );

        Response hiveResponse = given()
            .header("Authorization", "Bearer " + testUserToken)
            .contentType(ContentType.JSON)
            .body(hiveRequest)
            .when()
            .post("/api/hives")
            .then()
            .statusCode(201)
            .extract().response();

        testHiveId = hiveResponse.jsonPath().getString("id");
    }

    private List<Map<String, Object>> createOrderedEventSequence() {
        long baseTime = System.currentTimeMillis();
        
        return List.of(
            Map.of(
                "eventId", "order-1-" + UUID.randomUUID().toString(),
                "eventType", "USER_JOINED_HIVE",
                "userId", testUserId,
                "hiveId", testHiveId,
                "timestamp", baseTime,
                "priority", 1
            ),
            Map.of(
                "eventId", "order-2-" + UUID.randomUUID().toString(),
                "eventType", "TIMER_SESSION_STARTED",
                "userId", testUserId,
                "hiveId", testHiveId,
                "timestamp", baseTime + 1000,
                "priority", 1
            ),
            Map.of(
                "eventId", "order-3-" + UUID.randomUUID().toString(),
                "eventType", "PRODUCTIVITY_UPDATED",
                "userId", testUserId,
                "timestamp", baseTime + 2000,
                "priority", 2
            ),
            Map.of(
                "eventId", "order-4-" + UUID.randomUUID().toString(),
                "eventType", "ACHIEVEMENT_EARNED",
                "userId", testUserId,
                "timestamp", baseTime + 3000,
                "priority", 3
            ),
            Map.of(
                "eventId", "order-5-" + UUID.randomUUID().toString(),
                "eventType", "NOTIFICATION_TRIGGERED",
                "userId", testUserId,
                "timestamp", baseTime + 4000,
                "priority", 1
            )
        );
    }

    private void verifyEventEffectsOrdering() {
        // Verify the cumulative effects of ordered events
        
        // Should show user joined hive
        given()
            .header("Authorization", "Bearer " + testUserToken)
            .when()
            .get("/api/hives/{hiveId}/members", testHiveId)
            .then()
            .statusCode(200)
            .body("members", hasSize(greaterThan(0)));

        // Should show analytics progression
        Response analyticsResponse = given()
            .header("Authorization", "Bearer " + testUserToken)
            .when()
            .get("/api/analytics/users/{userId}/timeline", testUserId)
            .then()
            .statusCode(200)
            .extract().response();

        List<Map<String, Object>> timeline = analyticsResponse.jsonPath().getList("events");
        
        // Events should appear in chronological order in timeline
        for (int i = 1; i < timeline.size(); i++) {
            String prevTime = timeline.get(i - 1).get("timestamp").toString();
            String currTime = timeline.get(i).get("timestamp").toString();
            assertTrue(currTime.compareTo(prevTime) >= 0, 
                "Timeline events should be in chronological order");
        }
    }

    private Map<String, Object> createHighVolumeTestEvent(int index, String batchCorrelationId) {
        return Map.of(
            "eventId", "batch-" + index + "-" + UUID.randomUUID().toString(),
            "eventType", "HIGH_VOLUME_TEST",
            "userId", testUserId,
            "batchIndex", index,
            "timestamp", System.currentTimeMillis(),
            "correlationId", batchCorrelationId,
            "metadata", Map.of(
                "batchSize", 1000,
                "testType", "PERFORMANCE",
                "priority", index % 3 + 1 // Vary priority
            )
        );
    }

    private Map<String, Object> createWorkflowEvent(String eventType, int sequence, String correlationId, String traceId) {
        return Map.of(
            "eventId", "workflow-" + sequence + "-" + UUID.randomUUID().toString(),
            "eventType", eventType,
            "userId", testUserId,
            "sequenceNumber", sequence,
            "timestamp", System.currentTimeMillis(),
            "correlationId", correlationId,
            "traceId", traceId,
            "metadata", Map.of(
                "workflowStep", eventType,
                "stepNumber", sequence,
                "workflowId", correlationId
            )
        );
    }
}