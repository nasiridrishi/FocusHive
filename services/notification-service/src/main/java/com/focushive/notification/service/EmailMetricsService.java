package com.focushive.notification.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Email metrics service for monitoring and tracking email performance.
 *
 * Tracks:
 * - Email throughput (emails/second)
 * - Processing time (ms)
 * - Success/failure rates
 * - Dead letter queue metrics
 *
 * Performance targets from TODO.md:
 * - Throughput: >100 emails/second
 * - Response time: <50ms for queue acceptance
 * - Error rate: <0.1%
 */
@Service
@Slf4j
public class EmailMetricsService {

    private final MeterRegistry meterRegistry;

    // Counters
    private final Counter emailsSentCounter;
    private final Counter emailsFailedCounter;
    private final Counter emailsDeadLetterCounter;
    private final Counter emailsRetriedCounter;
    private final Counter emailsBouncedCounter;

    // Timers
    private final Timer emailProcessingTimer;
    private final Timer queueAcceptanceTimer;

    // Custom metrics
    private final AtomicLong totalEmailsSent = new AtomicLong(0);
    private final AtomicLong totalEmailsFailed = new AtomicLong(0);
    private final AtomicLong lastMinuteEmails = new AtomicLong(0);
    private final AtomicLong lastMinuteTimestamp = new AtomicLong(System.currentTimeMillis());

    public EmailMetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        // Initialize counters
        this.emailsSentCounter = Counter.builder("notification.email.sent")
            .description("Total number of emails sent successfully")
            .register(meterRegistry);

        this.emailsFailedCounter = Counter.builder("notification.email.failed")
            .description("Total number of emails that failed to send")
            .register(meterRegistry);

        this.emailsDeadLetterCounter = Counter.builder("notification.email.deadletter")
            .description("Total number of emails moved to dead letter queue")
            .register(meterRegistry);

        this.emailsRetriedCounter = Counter.builder("notification.email.retried")
            .description("Total number of email retry attempts")
            .register(meterRegistry);

        this.emailsBouncedCounter = Counter.builder("notification.email.bounced")
            .description("Total number of bounced emails")
            .register(meterRegistry);

        // Initialize timers
        this.emailProcessingTimer = Timer.builder("notification.email.processing.time")
            .description("Time taken to process and send emails")
            .register(meterRegistry);

        this.queueAcceptanceTimer = Timer.builder("notification.email.queue.acceptance.time")
            .description("Time taken to accept email into queue")
            .register(meterRegistry);

        // Register custom gauges
        meterRegistry.gauge("notification.email.throughput", this, EmailMetricsService::getEmailThroughput);
        meterRegistry.gauge("notification.email.error.rate", this, EmailMetricsService::getErrorRate);
        meterRegistry.gauge("notification.email.success.rate", this, EmailMetricsService::getSuccessRate);
    }

    /**
     * Records a successfully sent email with processing time.
     */
    public void recordEmailSent(long processingTimeMs) {
        emailsSentCounter.increment();
        totalEmailsSent.incrementAndGet();
        emailProcessingTimer.record(Duration.ofMillis(processingTimeMs));
        updateThroughput();

        log.debug("Email sent - Processing time: {}ms, Total sent: {}",
            processingTimeMs, totalEmailsSent.get());
    }

    /**
     * Records a failed email attempt with error type.
     */
    public void recordEmailFailed(String errorType) {
        emailsFailedCounter.increment();
        totalEmailsFailed.incrementAndGet();

        // Track specific error types
        Counter.builder("notification.email.failed.by.type")
            .tag("error", errorType)
            .register(meterRegistry)
            .increment();

        log.warn("Email failed - Error type: {}, Total failed: {}",
            errorType, totalEmailsFailed.get());
    }

    /**
     * Records an email moved to dead letter queue.
     */
    public void recordEmailDeadLetter() {
        emailsDeadLetterCounter.increment();
        log.error("Email moved to dead letter queue");
    }

    /**
     * Records an email retry attempt.
     */
    public void recordEmailRetried() {
        emailsRetriedCounter.increment();
    }

    /**
     * Records a bounced email.
     */
    public void recordEmailBounced(String recipient) {
        emailsBouncedCounter.increment();
        log.warn("Email bounced for recipient: {}", recipient);
    }

    /**
     * Records the time taken to accept an email into the queue.
     */
    public void recordQueueAcceptanceTime(long timeMs) {
        queueAcceptanceTimer.record(Duration.ofMillis(timeMs));

        if (timeMs > 50) {
            log.warn("Queue acceptance time exceeded 50ms threshold: {}ms", timeMs);
        }
    }

    /**
     * Gets the current email throughput (emails per second).
     * Target: >100 emails/second
     */
    public double getEmailThroughput() {
        updateThroughput();
        long currentTime = System.currentTimeMillis();
        long timeDiff = currentTime - lastMinuteTimestamp.get();

        if (timeDiff > 0) {
            return (lastMinuteEmails.get() * 1000.0) / timeDiff;
        }
        return 0.0;
    }

    /**
     * Gets the current error rate.
     * Target: <0.1%
     */
    public double getErrorRate() {
        long total = totalEmailsSent.get() + totalEmailsFailed.get();
        if (total > 0) {
            return (totalEmailsFailed.get() * 100.0) / total;
        }
        return 0.0;
    }

    /**
     * Gets the current success rate.
     */
    public double getSuccessRate() {
        long total = totalEmailsSent.get() + totalEmailsFailed.get();
        if (total > 0) {
            return (totalEmailsSent.get() * 100.0) / total;
        }
        return 100.0;
    }

    /**
     * Updates the throughput calculation.
     */
    private void updateThroughput() {
        long currentTime = System.currentTimeMillis();
        long lastTime = lastMinuteTimestamp.get();

        // Reset counter every minute
        if (currentTime - lastTime > 60000) {
            lastMinuteEmails.set(1);
            lastMinuteTimestamp.set(currentTime);
        } else {
            lastMinuteEmails.incrementAndGet();
        }
    }

    /**
     * Logs current metrics summary.
     */
    public void logMetricsSummary() {
        log.info("Email Metrics Summary - Sent: {}, Failed: {}, Throughput: {:.2f}/s, Error Rate: {:.2f}%, Success Rate: {:.2f}%",
            totalEmailsSent.get(),
            totalEmailsFailed.get(),
            getEmailThroughput(),
            getErrorRate(),
            getSuccessRate());

        // Check if we're meeting performance targets
        if (getEmailThroughput() < 100) {
            log.warn("Email throughput below target (100/s): {:.2f}/s", getEmailThroughput());
        }

        if (getErrorRate() > 0.1) {
            log.error("Email error rate above target (0.1%): {:.2f}%", getErrorRate());
        }
    }

    /**
     * Resets all metrics (useful for testing).
     */
    public void resetMetrics() {
        totalEmailsSent.set(0);
        totalEmailsFailed.set(0);
        lastMinuteEmails.set(0);
        lastMinuteTimestamp.set(System.currentTimeMillis());
        log.info("Email metrics have been reset");
    }
}