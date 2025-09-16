package com.focushive.notification.service;

import com.focushive.notification.exception.EmailDeliveryException;
import com.focushive.notification.messaging.dto.NotificationMessage;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailSendException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

/**
 * Resilient email service with circuit breaker protection.
 * Provides fallback mechanisms for email delivery failures.
 */
@Slf4j
@Service
public class ResilientEmailService {

    private final EmailNotificationService emailService;
    private final CircuitBreaker emailServiceCircuitBreaker;
    private final MeterRegistry meterRegistry;

    // Queue for failed messages
    private final ConcurrentLinkedQueue<NotificationMessage> messageQueue = new ConcurrentLinkedQueue<>();

    // Cache for email metrics
    private final ConcurrentHashMap<String, EmailServiceMetrics> metricsCache = new ConcurrentHashMap<>();

    public ResilientEmailService(EmailNotificationService emailService,
                                CircuitBreaker emailServiceCircuitBreaker,
                                MeterRegistry meterRegistry) {
        this.emailService = emailService;
        this.emailServiceCircuitBreaker = emailServiceCircuitBreaker;
        this.meterRegistry = meterRegistry;
    }

    /**
     * Send email with circuit breaker protection.
     */
    public String sendEmail(NotificationMessage message) {
        try {
            return emailServiceCircuitBreaker.executeSupplier(() -> {
                String messageId = emailService.sendEmail(message);
                recordSuccess();
                return messageId;
            });
        } catch (CallNotPermittedException e) {
            log.warn("Circuit breaker is open for email service, using fallback");
            return sendEmailWithFallback(message);
        } catch (Exception e) {
            log.error("Failed to send email", e);
            recordFailure(e);
            if (shouldRetry(e)) {
                return queueForRetry(message);
            }
            throw new EmailDeliveryException("Failed to send email", e);
        }
    }

    /**
     * Send email with fallback mechanism.
     */
    public String sendEmailWithFallback(NotificationMessage message) {
        log.info("Using fallback mechanism for email: {}", message.getEmailTo());

        // Record fallback usage
        meterRegistry.counter("circuit.breaker.fallback.usage",
            "service", "email-service").increment();

        // Queue the message for later delivery
        messageQueue.offer(message);
        String fallbackId = "fallback-" + UUID.randomUUID().toString();

        log.info("Email queued with fallback ID: {}", fallbackId);
        return fallbackId;
    }

    /**
     * Send email with queueing when circuit is open.
     */
    public String sendEmailWithQueue(NotificationMessage message) {
        if (emailServiceCircuitBreaker.getState() == CircuitBreaker.State.OPEN) {
            return queueMessage(message);
        }
        return sendEmail(message);
    }

    /**
     * Send email asynchronously with circuit breaker.
     */
    public CompletableFuture<String> sendEmailAsync(NotificationMessage message) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return emailServiceCircuitBreaker.executeSupplier(() -> {
                    String messageId = emailService.sendEmail(message);
                    emailServiceCircuitBreaker.onSuccess(System.currentTimeMillis(), TimeUnit.MILLISECONDS);
                    return messageId;
                });
            } catch (Exception e) {
                log.error("Async email send failed", e);
                emailServiceCircuitBreaker.onError(System.currentTimeMillis(), TimeUnit.MILLISECONDS, e);
                return sendEmailWithFallback(message);
            }
        });
    }

    /**
     * Get circuit breaker metrics.
     */
    public EmailServiceMetrics getMetrics() {
        CircuitBreaker.Metrics cbMetrics = emailServiceCircuitBreaker.getMetrics();

        EmailServiceMetrics metrics = new EmailServiceMetrics();
        metrics.setSuccessfulCalls((int) cbMetrics.getNumberOfSuccessfulCalls());
        metrics.setFailedCalls((int) cbMetrics.getNumberOfFailedCalls());
        metrics.setFailureRate(cbMetrics.getFailureRate());
        metrics.setSlowCallRate(cbMetrics.getSlowCallRate());
        metrics.setCircuitState(emailServiceCircuitBreaker.getState().toString());
        metrics.setQueuedMessages(messageQueue.size());

        return metrics;
    }

    /**
     * Queue message for later delivery.
     */
    private String queueMessage(NotificationMessage message) {
        messageQueue.offer(message);
        String queuedId = "queued-" + UUID.randomUUID().toString();

        meterRegistry.counter("circuit.breaker.message.queued",
            "service", "email-service").increment();

        log.info("Message queued: {}", queuedId);
        return queuedId;
    }

    /**
     * Check if exception is retryable.
     */
    private boolean shouldRetry(Exception e) {
        // Don't retry on client errors
        if (e instanceof IllegalArgumentException || e instanceof NullPointerException) {
            return false;
        }
        // Retry on network and server errors
        return e instanceof MailSendException ||
               e instanceof EmailDeliveryException ||
               e.getCause() instanceof java.net.SocketTimeoutException;
    }

    /**
     * Queue message for retry.
     */
    private String queueForRetry(NotificationMessage message) {
        messageQueue.offer(message);
        return "retry-" + UUID.randomUUID().toString();
    }

    /**
     * Record successful call.
     */
    private void recordSuccess() {
        meterRegistry.counter("email.send.success").increment();
    }

    /**
     * Record failed call.
     */
    private void recordFailure(Exception e) {
        meterRegistry.counter("email.send.failure",
            "exception", e.getClass().getSimpleName()).increment();
    }

    /**
     * Process queued messages (called by scheduled task).
     */
    public void processQueuedMessages() {
        if (emailServiceCircuitBreaker.getState() != CircuitBreaker.State.CLOSED) {
            log.debug("Circuit breaker not closed, skipping queue processing");
            return;
        }

        int processed = 0;
        int maxBatch = 10;

        while (!messageQueue.isEmpty() && processed < maxBatch) {
            NotificationMessage message = messageQueue.poll();
            if (message != null) {
                try {
                    sendEmail(message);
                    processed++;
                } catch (Exception e) {
                    log.error("Failed to process queued message", e);
                    // Re-queue the message
                    messageQueue.offer(message);
                    break; // Stop processing if we hit errors
                }
            }
        }

        if (processed > 0) {
            log.info("Processed {} queued messages", processed);
        }
    }

    /**
     * Metrics class for email service.
     */
    public static class EmailServiceMetrics {
        private int successfulCalls;
        private int failedCalls;
        private float failureRate;
        private float slowCallRate;
        private String circuitState;
        private int queuedMessages;

        // Getters and setters
        public int getSuccessfulCalls() { return successfulCalls; }
        public void setSuccessfulCalls(int successfulCalls) { this.successfulCalls = successfulCalls; }

        public int getFailedCalls() { return failedCalls; }
        public void setFailedCalls(int failedCalls) { this.failedCalls = failedCalls; }

        public float getFailureRate() { return failureRate; }
        public void setFailureRate(float failureRate) { this.failureRate = failureRate; }

        public float getSlowCallRate() { return slowCallRate; }
        public void setSlowCallRate(float slowCallRate) { this.slowCallRate = slowCallRate; }

        public String getCircuitState() { return circuitState; }
        public void setCircuitState(String circuitState) { this.circuitState = circuitState; }

        public int getQueuedMessages() { return queuedMessages; }
        public void setQueuedMessages(int queuedMessages) { this.queuedMessages = queuedMessages; }
    }
}