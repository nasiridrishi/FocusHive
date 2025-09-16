package com.focushive.notification.service;

import com.focushive.notification.messaging.dto.NotificationMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

/**
 * Service for sending emails using Amazon SES.
 * 
 * Provides comprehensive email delivery functionality including:
 * - Text and HTML email sending
 * - Template-based emails with variable substitution
 * - Delivery tracking and status updates
 * - Bounce and complaint handling
 * - Rate limiting and retry logic
 * - Quiet hours support
 * - Bulk email operations
 * - Delivery statistics
 */
@Slf4j
@Service
public class EmailNotificationService {

    private final JavaMailSender mailSender;
    private final String fromEmailAddress;

    /**
     * Constructor with dependency injection.
     *
     * @param mailSender the JavaMailSender for sending emails via SMTP
     * @param fromEmailAddress the configured from email address
     */
    public EmailNotificationService(JavaMailSender mailSender, @Qualifier("fromEmailAddress") String fromEmailAddress) {
        this.mailSender = mailSender;
        this.fromEmailAddress = fromEmailAddress;
        log.info("EmailNotificationService initialized with SMTP sender and from email: {}", fromEmailAddress);
    }

    // Email validation pattern
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$"
    );

    // Template variable pattern for substitution
    private static final Pattern TEMPLATE_VARIABLE_PATTERN = Pattern.compile("\\{\\{([^}]+)\\}\\}");

    // Email tracking storage (in production, this would be a database)
    private final Map<String, EmailDeliveryResult> deliveryResults = new ConcurrentHashMap<>();
    
    // Statistics tracking
    private final Map<EmailDeliveryStatus, AtomicLong> statusCounts = new ConcurrentHashMap<>();
    
    // Rate limiting
    private int rateLimit = 10; // emails per second
    private int ratePeriodSeconds = 1;
    private final List<Long> sendTimes = Collections.synchronizedList(new ArrayList<>());
    
    // Quiet hours configuration
    private int quietHourStart = 22; // 10 PM
    private int quietHourEnd = 8;    // 8 AM
    private int currentHour = LocalTime.now().getHour(); // For testing

    /**
     * Send a simple email message.
     *
     * @param message the notification message containing email details
     * @return the SES message ID
     * @throws EmailDeliveryException if sending fails
     */
    public String sendEmail(NotificationMessage message) {
        validateMessage(message);
        
        try {
            applyRateLimit();
            
            // Use SMTP to send email
            if (isHtmlContent(message.getMessage())) {
                sendHtmlEmail(message);
            } else {
                sendTextEmail(message);
            }
            
            // Generate a pseudo message ID for tracking
            String messageId = "smtp-" + System.currentTimeMillis() + "-" + Math.random();
            log.info("Email sent successfully via SMTP - MessageId: {}, Recipient: {}", 
                    messageId, message.getEmailTo());
            
            return messageId;
            
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", message.getEmailTo(), e.getMessage());
            throw new EmailDeliveryException("Failed to send email: " + e.getMessage(), e);
        }
    }

    /**
     * Send email with template variable substitution.
     *
     * @param message the notification message with template info
     * @return the SES message ID
     */
    public String sendEmailWithTemplate(NotificationMessage message) {
        validateMessage(message);
        
        if (message.getTemplateId() == null) {
            throw new IllegalArgumentException("Template ID is required for template emails");
        }
        
        // Apply template variable substitution
        NotificationMessage processedMessage = processTemplate(message);
        
        return sendEmail(processedMessage);
    }

    /**
     * Send email with retry logic for transient failures.
     *
     * @param message the notification message
     * @param maxRetries maximum number of retry attempts
     * @return the SES message ID
     * @throws EmailDeliveryException if all retries fail
     */
    public String sendEmailWithRetry(NotificationMessage message, int maxRetries) {
        Exception lastException = null;
        
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                return sendEmail(message);
                
            } catch (EmailDeliveryException e) {
                lastException = e;
                
                // Check if this is a retryable error
                if (isRetryableError(e) && attempt < maxRetries) {
                    long delayMs = calculateRetryDelay(attempt);
                    log.warn("Email send attempt {} failed, retrying in {}ms: {}", 
                            attempt, delayMs, e.getMessage());
                    
                    try {
                        Thread.sleep(delayMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                } else {
                    break;
                }
            }
        }
        
        throw new EmailDeliveryException("Max retries exceeded (" + maxRetries + " attempts)", lastException);
    }

    /**
     * Send email and track delivery status.
     *
     * @param message the notification message
     * @return the delivery result with tracking info
     */
    public EmailDeliveryResult sendEmailAndTrack(NotificationMessage message) {
        validateMessage(message);
        
        try {
            String messageId = sendEmail(message);
            
            EmailDeliveryResult result = EmailDeliveryResult.builder()
                    .messageId(messageId)
                    .notificationId(message.getNotificationId())
                    .recipient(message.getEmailTo())
                    .status(EmailDeliveryStatus.SENT)
                    .sentAt(LocalDateTime.now())
                    .retryCount(0)
                    .build();
            
            deliveryResults.put(messageId, result);
            incrementStatusCount(EmailDeliveryStatus.SENT);
            
            return result;
            
        } catch (EmailDeliveryException e) {
            EmailDeliveryResult result = EmailDeliveryResult.builder()
                    .notificationId(message.getNotificationId())
                    .recipient(message.getEmailTo())
                    .status(EmailDeliveryStatus.FAILED)
                    .failureReason(e.getMessage())
                    .sentAt(LocalDateTime.now())
                    .build();
            
            incrementStatusCount(EmailDeliveryStatus.FAILED);
            return result;
        }
    }

    /**
     * Send email respecting quiet hours configuration.
     *
     * @param message the notification message
     * @return the delivery result (sent or scheduled)
     */
    public EmailDeliveryResult sendEmailRespectingQuietHours(NotificationMessage message) {
        if (isInQuietHours()) {
            // Schedule for next morning
            LocalDateTime scheduledFor = calculateNextSendTime();
            
            EmailDeliveryResult result = EmailDeliveryResult.builder()
                    .notificationId(message.getNotificationId())
                    .recipient(message.getEmailTo())
                    .status(EmailDeliveryStatus.SCHEDULED)
                    .scheduledFor(scheduledFor)
                    .build();
            
            log.info("Email scheduled for quiet hours - will send at {}", scheduledFor);
            return result;
        } else {
            return sendEmailAndTrack(message);
        }
    }

    /**
     * Send multiple emails in bulk.
     *
     * @param messages array of notification messages
     * @return bulk operation result
     */
    public BulkEmailResult sendBulkEmails(NotificationMessage... messages) {
        LocalDateTime startTime = LocalDateTime.now();
        List<String> messageIds = new ArrayList<>();
        List<BulkEmailResult.BulkEmailFailure> failures = new ArrayList<>();
        
        log.info("Starting bulk email operation for {} messages", messages.length);
        
        for (NotificationMessage message : messages) {
            try {
                String messageId = sendEmail(message);
                messageIds.add(messageId);
                
            } catch (EmailDeliveryException e) {
                BulkEmailResult.BulkEmailFailure failure = BulkEmailResult.BulkEmailFailure.builder()
                        .notificationId(message.getNotificationId())
                        .recipient(message.getEmailTo())
                        .reason(e.getMessage())
                        .failedAt(LocalDateTime.now())
                        .build();
                
                failures.add(failure);
                log.error("Bulk email failed for {}: {}", message.getEmailTo(), e.getMessage());
            }
        }
        
        LocalDateTime endTime = LocalDateTime.now();
        long processingTimeMs = java.time.Duration.between(startTime, endTime).toMillis();
        
        return BulkEmailResult.builder()
                .totalCount(messages.length)
                .successCount(messageIds.size())
                .failureCount(failures.size())
                .messageIds(messageIds)
                .failures(failures)
                .startedAt(startTime)
                .completedAt(endTime)
                .processingTimeMs(processingTimeMs)
                .build();
    }

    /**
     * Handle email bounce notification from SES.
     *
     * @param messageId the original message ID
     * @param recipient the recipient email address
     * @param bounceReason reason for the bounce
     */
    public void handleBounce(String messageId, String recipient, String bounceReason) {
        log.warn("Email bounced - MessageId: {}, Recipient: {}, Reason: {}", 
                messageId, recipient, bounceReason);
        
        EmailDeliveryResult result = deliveryResults.get(messageId);
        if (result != null) {
            result.setStatus(EmailDeliveryStatus.BOUNCED);
            result.setFailureReason(bounceReason);
            result.setBouncedAt(LocalDateTime.now());
        } else {
            // Create new result for bounce
            result = EmailDeliveryResult.builder()
                    .messageId(messageId)
                    .recipient(recipient)
                    .status(EmailDeliveryStatus.BOUNCED)
                    .failureReason(bounceReason)
                    .bouncedAt(LocalDateTime.now())
                    .build();
            deliveryResults.put(messageId, result);
        }
        
        incrementStatusCount(EmailDeliveryStatus.BOUNCED);
    }

    /**
     * Handle email complaint notification from SES.
     *
     * @param messageId the original message ID
     * @param recipient the recipient who complained
     */
    public void handleComplaint(String messageId, String recipient) {
        log.warn("Email complaint received - MessageId: {}, Recipient: {}", messageId, recipient);
        
        EmailDeliveryResult result = deliveryResults.get(messageId);
        if (result != null) {
            result.setStatus(EmailDeliveryStatus.COMPLAINED);
            result.setComplainedAt(LocalDateTime.now());
        } else {
            result = EmailDeliveryResult.builder()
                    .messageId(messageId)
                    .recipient(recipient)
                    .status(EmailDeliveryStatus.COMPLAINED)
                    .complainedAt(LocalDateTime.now())
                    .build();
            deliveryResults.put(messageId, result);
        }
        
        incrementStatusCount(EmailDeliveryStatus.COMPLAINED);
    }

    /**
     * Get delivery status for a specific message.
     *
     * @param messageId the SES message ID
     * @return delivery result or null if not found
     */
    public EmailDeliveryResult getDeliveryStatus(String messageId) {
        return deliveryResults.get(messageId);
    }

    /**
     * Record a delivery result for statistics tracking.
     *
     * @param result the delivery result to record
     */
    public void recordDeliveryResult(EmailDeliveryResult result) {
        if (result.getMessageId() != null) {
            deliveryResults.put(result.getMessageId(), result);
        }
        incrementStatusCount(result.getStatus());
    }

    /**
     * Get email delivery statistics.
     *
     * @return comprehensive delivery statistics
     */
    public EmailDeliveryStatistics getDeliveryStatistics() {
        long totalSent = statusCounts.values().stream().mapToLong(AtomicLong::get).sum();
        long delivered = getStatusCount(EmailDeliveryStatus.DELIVERED);
        long bounced = getStatusCount(EmailDeliveryStatus.BOUNCED);
        long complained = getStatusCount(EmailDeliveryStatus.COMPLAINED);
        
        double deliveryRate = totalSent > 0 ? (double) delivered / totalSent : 0.0;
        double bounceRate = totalSent > 0 ? (double) bounced / totalSent : 0.0;
        double complaintRate = totalSent > 0 ? (double) complained / totalSent : 0.0;
        
        return EmailDeliveryStatistics.builder()
                .totalSent(totalSent)
                .deliveredCount(delivered)
                .bounceCount(bounced)
                .complaintCount(complained)
                .deliveryRate(deliveryRate)
                .bounceRate(bounceRate)
                .complaintRate(complaintRate)
                .calculatedAt(LocalDateTime.now())
                .build();
    }

    // Configuration methods for testing
    public void setRateLimit(int emailsPerSecond, int periodSeconds) {
        this.rateLimit = emailsPerSecond;
        this.ratePeriodSeconds = periodSeconds;
    }
    
    public void setQuietHours(int startHour, int endHour) {
        this.quietHourStart = startHour;
        this.quietHourEnd = endHour;
    }
    
    public void setCurrentHour(int hour) {
        this.currentHour = hour;
    }
    
    public boolean isInQuietHours() {
        if (quietHourStart <= quietHourEnd) {
            // Normal case: e.g., 22-8 means quiet hours don't span midnight
            return false; // This would be handled differently in real implementation
        } else {
            // Quiet hours span midnight: e.g., 22-8 (10 PM to 8 AM)
            return currentHour >= quietHourStart || currentHour < quietHourEnd;
        }
    }

    // Private helper methods

    private void sendTextEmail(NotificationMessage message) {
        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setFrom(message.getEmailFrom() != null ? message.getEmailFrom() : fromEmailAddress);
        mailMessage.setTo(message.getEmailTo());
        mailMessage.setSubject(message.getEmailSubject());
        mailMessage.setText(message.getMessage());
        
        mailSender.send(mailMessage);
    }
    
    private void sendHtmlEmail(NotificationMessage message) throws MessagingException {
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
        
        helper.setFrom(message.getEmailFrom() != null ? message.getEmailFrom() : fromEmailAddress);
        helper.setTo(message.getEmailTo());
        helper.setSubject(message.getEmailSubject());
        helper.setText(message.getMessage(), true); // true indicates HTML
        
        mailSender.send(mimeMessage);
    }

    private void validateMessage(NotificationMessage message) {
        if (message == null) {
            throw new IllegalArgumentException("Notification message cannot be null");
        }
        
        if (message.getEmailTo() == null || message.getEmailTo().trim().isEmpty()) {
            throw new IllegalArgumentException("Email recipient is required");
        }
        
        if (!EMAIL_PATTERN.matcher(message.getEmailTo()).matches()) {
            throw new IllegalArgumentException("Invalid email address: " + message.getEmailTo());
        }
        
        if (message.getEmailSubject() == null || message.getEmailSubject().trim().isEmpty()) {
            throw new IllegalArgumentException("Email subject is required");
        }
        
        if (message.getMessage() == null || message.getMessage().trim().isEmpty()) {
            throw new IllegalArgumentException("Email message content is required");
        }
    }


    private boolean isHtmlContent(String content) {
        return content != null && (
            content.contains("<html>") || 
            content.contains("<body>") ||
            content.contains("<h1>") ||
            content.contains("<h2>") ||
            content.contains("<p>") ||
            content.contains("<div>") ||
            content.contains("<span>")
        );
    }

    private NotificationMessage processTemplate(NotificationMessage message) {
        if (message.getTemplateVariables() == null || message.getTemplateVariables().isEmpty()) {
            return message;
        }
        
        // Process subject
        String processedSubject = substituteVariables(message.getEmailSubject(), message.getTemplateVariables());
        
        // Process message content (for now, we'll use a simple template)
        String processedContent = createTemplatedContent(message);
        
        return NotificationMessage.builder()
                .notificationId(message.getNotificationId())
                .userId(message.getUserId())
                .type(message.getType())
                .title(message.getTitle())
                .message(processedContent)
                .emailTo(message.getEmailTo())
                .emailSubject(processedSubject)
                .emailFrom(message.getEmailFrom())
                .templateId(message.getTemplateId())
                .templateVariables(message.getTemplateVariables())
                .timestamp(message.getTimestamp())
                .build();
    }

    private String substituteVariables(String template, Map<String, String> variables) {
        if (template == null || variables == null) {
            return template;
        }
        
        String result = template;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            String placeholder = "{{" + entry.getKey() + "}}";
            result = result.replace(placeholder, entry.getValue());
        }
        
        return result;
    }

    private String createTemplatedContent(NotificationMessage message) {
        // Simple template for now - in production, you'd load from database/file
        if (message.getTemplateVariables() == null || !message.getTemplateVariables().containsKey("userName")) {
            return message.getMessage();
        }
        
        StringBuilder html = new StringBuilder();
        html.append("<html><body>");
        html.append("<h1>Hello ").append(message.getTemplateVariables().get("userName")).append("!</h1>");
        html.append("<p>").append(message.getMessage() != null ? message.getMessage() : "").append("</p>");
        html.append("</body></html>");
        
        return html.toString();
    }

    private boolean isRetryableError(EmailDeliveryException e) {
        if (e.getCause() != null) {
            // Check message for common retryable errors
            String message = e.getCause().getMessage();
            if (message != null) {
                message = message.toLowerCase();
                return message.contains("connection") || 
                       message.contains("timeout") ||
                       message.contains("temporary") ||
                       message.contains("rate limit") ||
                       message.contains("throttle") ||
                       message.contains("service unavailable");
            }
        }
        return false;
    }

    private long calculateRetryDelay(int attempt) {
        // Exponential backoff: 1s, 2s, 4s, 8s, etc.
        return (long) Math.pow(2, attempt - 1) * 1000;
    }

    private void applyRateLimit() {
        long now = System.currentTimeMillis();
        long periodMs = ratePeriodSeconds * 1000L;
        
        // Remove old send times outside the current period
        sendTimes.removeIf(time -> now - time > periodMs);
        
        // Check if we're at the rate limit
        if (sendTimes.size() >= rateLimit) {
            long oldestTime = sendTimes.get(0);
            long waitTime = periodMs - (now - oldestTime);
            
            if (waitTime > 0) {
                try {
                    log.debug("Rate limiting: waiting {}ms", waitTime);
                    Thread.sleep(waitTime);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new EmailDeliveryException("Rate limiting interrupted", e);
                }
            }
        }
        
        sendTimes.add(System.currentTimeMillis());
    }

    private LocalDateTime calculateNextSendTime() {
        LocalDateTime tomorrow = LocalDateTime.now().plusDays(1);
        return tomorrow.withHour(quietHourEnd).withMinute(0).withSecond(0);
    }

    private void incrementStatusCount(EmailDeliveryStatus status) {
        statusCounts.computeIfAbsent(status, k -> new AtomicLong(0)).incrementAndGet();
    }

    private long getStatusCount(EmailDeliveryStatus status) {
        AtomicLong count = statusCounts.get(status);
        return count != null ? count.get() : 0L;
    }
}