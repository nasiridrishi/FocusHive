package com.focushive.notification.service;

import com.focushive.notification.dto.EmailRequest;
import com.focushive.notification.dto.EmailStatus;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Async Email Service with production-grade features.
 *
 * Features:
 * - Async processing with dedicated thread pool
 * - Retry mechanism with exponential backoff
 * - Email status tracking
 * - Template rendering support
 * - Error handling and recovery
 *
 * Performance Targets (from TODO.md):
 * - Throughput: >100 emails/second
 * - Response time: <50ms for queue acceptance
 * - Error rate: <0.1%
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AsyncEmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final EmailMetricsService metricsService;

    // In-memory status tracking (should be replaced with Redis in production)
    private final Map<String, EmailStatus> emailStatusMap = new ConcurrentHashMap<>();

    /**
     * Sends email asynchronously with retry mechanism.
     * Returns immediately with a tracking ID (<50ms as per requirement).
     *
     * @param emailRequest The email request containing recipient, subject, and content
     * @return CompletableFuture with email tracking ID
     */
    @Async("emailTaskExecutor")
    @Retryable(
        value = {MailException.class, MessagingException.class},
        maxAttempts = 3,
        backoff = @Backoff(
            delay = 1000,      // Initial delay: 1 second
            multiplier = 2,     // Exponential multiplier
            maxDelay = 10000    // Max delay: 10 seconds
        )
    )
    public CompletableFuture<String> sendEmailAsync(EmailRequest emailRequest) {
        String trackingId = UUID.randomUUID().toString();
        Instant startTime = Instant.now();

        log.debug("Processing email async: trackingId={}, to={}, subject={}",
                trackingId, emailRequest.getTo(), emailRequest.getSubject());

        // Update status to QUEUED
        updateEmailStatus(trackingId, EmailStatus.Status.QUEUED, null);

        try {
            // Render template if template name is provided
            String htmlContent;
            if (emailRequest.getTemplateName() != null) {
                htmlContent = renderTemplate(emailRequest.getTemplateName(), emailRequest.getVariables());
            } else {
                htmlContent = emailRequest.getHtmlContent();
            }

            // Create and send email
            MimeMessage message = createMimeMessage(
                emailRequest.getTo(),
                emailRequest.getCc(),
                emailRequest.getBcc(),
                emailRequest.getSubject(),
                htmlContent,
                emailRequest.isHtml()
            );

            // Update status to SENDING
            updateEmailStatus(trackingId, EmailStatus.Status.SENDING, null);

            // Send the email
            mailSender.send(message);

            // Update status to SENT
            updateEmailStatus(trackingId, EmailStatus.Status.SENT, null);

            // Record success metrics
            long processingTime = Instant.now().toEpochMilli() - startTime.toEpochMilli();
            metricsService.recordEmailSent(processingTime);

            log.info("Email sent successfully: trackingId={}, processingTime={}ms", trackingId, processingTime);

            return CompletableFuture.completedFuture(trackingId);

        } catch (Exception e) {
            log.error("Failed to send email: trackingId={}, error={}", trackingId, e.getMessage(), e);

            // Update status to FAILED
            updateEmailStatus(trackingId, EmailStatus.Status.FAILED, e.getMessage());

            // Record failure metrics
            metricsService.recordEmailFailed(e.getClass().getSimpleName());

            throw new EmailDeliveryException("Failed to send email", e);
        }
    }

    /**
     * Recovery method called when all retry attempts fail.
     * Sends the email to dead letter queue for manual processing.
     */
    @Recover
    public CompletableFuture<String> recoverFromEmailFailure(Exception e, EmailRequest emailRequest) {
        String deadLetterId = UUID.randomUUID().toString();

        log.error("All retry attempts failed for email to: {}, moving to dead letter queue with ID: {}",
                emailRequest.getTo(), deadLetterId, e);

        // Send to dead letter queue (to be implemented)
        sendToDeadLetterQueue(emailRequest, deadLetterId, e);

        // Record dead letter metrics
        metricsService.recordEmailDeadLetter();

        // Update status to DEAD_LETTER
        updateEmailStatus(deadLetterId, EmailStatus.Status.DEAD_LETTER, "Moved to dead letter queue after retries failed");

        return CompletableFuture.completedFuture(deadLetterId);
    }

    /**
     * Sends batch emails asynchronously.
     * Processes emails in parallel for better throughput.
     */
    @Async("emailTaskExecutor")
    public CompletableFuture<Map<String, String>> sendBatchEmailsAsync(List<EmailRequest> emailRequests) {
        log.info("Processing batch of {} emails", emailRequests.size());

        Map<String, String> results = new ConcurrentHashMap<>();

        // Process emails in parallel
        List<CompletableFuture<String>> futures = emailRequests.stream()
            .map(request -> sendEmailAsync(request)
                .thenApply(trackingId -> {
                    results.put(request.getTo(), trackingId);
                    return trackingId;
                })
                .exceptionally(throwable -> {
                    log.error("Failed to send email to: {}", request.getTo(), throwable);
                    results.put(request.getTo(), "FAILED");
                    return "FAILED";
                })
            )
            .collect(Collectors.toList());

        // Wait for all emails to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .join();

        log.info("Batch email processing complete. Success: {}, Failed: {}",
                results.values().stream().filter(id -> !"FAILED".equals(id)).count(),
                results.values().stream().filter("FAILED"::equals).count());

        return CompletableFuture.completedFuture(results);
    }

    /**
     * Gets the current status of an email by tracking ID.
     */
    public EmailStatus getEmailStatus(String trackingId) {
        return emailStatusMap.getOrDefault(trackingId,
            new EmailStatus(trackingId, EmailStatus.Status.UNKNOWN, Instant.now(), null));
    }

    /**
     * Creates a MIME message with the provided parameters.
     */
    private MimeMessage createMimeMessage(String to, List<String> cc, List<String> bcc,
                                         String subject, String content, boolean isHtml) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(content, isHtml);
        helper.setFrom("noreply@focushive.com"); // Should be configurable

        if (cc != null && !cc.isEmpty()) {
            helper.setCc(cc.toArray(new String[0]));
        }

        if (bcc != null && !bcc.isEmpty()) {
            helper.setBcc(bcc.toArray(new String[0]));
        }

        return message;
    }

    /**
     * Renders email template with variables.
     */
    private String renderTemplate(String templateName, Map<String, Object> variables) {
        Context context = new Context();
        if (variables != null) {
            context.setVariables(variables);
        }
        return templateEngine.process(templateName, context);
    }

    /**
     * Updates email status in the tracking system.
     */
    private void updateEmailStatus(String trackingId, EmailStatus.Status status, String errorMessage) {
        EmailStatus emailStatus = new EmailStatus(trackingId, status, Instant.now(), errorMessage);
        emailStatusMap.put(trackingId, emailStatus);

        // TODO: Store in Redis for distributed tracking
        log.debug("Email status updated: trackingId={}, status={}", trackingId, status);
    }

    /**
     * Sends failed email to dead letter queue for manual processing.
     */
    private void sendToDeadLetterQueue(EmailRequest emailRequest, String deadLetterId, Exception error) {
        // TODO: Implement dead letter queue using RabbitMQ
        log.warn("Dead letter queue not yet implemented. Email lost: {}", emailRequest.getTo());

        // For now, just log the failure
        log.error("DEAD LETTER: id={}, to={}, subject={}, error={}",
                deadLetterId, emailRequest.getTo(), emailRequest.getSubject(), error.getMessage());
    }

    /**
     * Exception thrown when email delivery fails.
     */
    public static class EmailDeliveryException extends RuntimeException {
        public EmailDeliveryException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}