package com.focushive.notification.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Result of an email delivery attempt.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailDeliveryResult {
    
    /**
     * SES message ID returned after sending
     */
    private String messageId;
    
    /**
     * Original notification ID
     */
    private String notificationId;
    
    /**
     * Email recipient address
     */
    private String recipient;
    
    /**
     * Current delivery status
     */
    private EmailDeliveryStatus status;
    
    /**
     * When the email was sent to SES
     */
    private LocalDateTime sentAt;
    
    /**
     * When the email was delivered (if known)
     */
    private LocalDateTime deliveredAt;
    
    /**
     * When the email bounced (if applicable)
     */
    private LocalDateTime bouncedAt;
    
    /**
     * When a complaint was received (if applicable)
     */
    private LocalDateTime complainedAt;
    
    /**
     * When the email is scheduled to be sent (for delayed emails)
     */
    private LocalDateTime scheduledFor;
    
    /**
     * Failure reason if status is FAILED or BOUNCED
     */
    private String failureReason;
    
    /**
     * Number of retry attempts made
     */
    @Builder.Default
    private Integer retryCount = 0;
}