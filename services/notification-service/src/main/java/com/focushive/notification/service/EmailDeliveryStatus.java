package com.focushive.notification.service;

/**
 * Enum representing the various states of email delivery.
 */
public enum EmailDeliveryStatus {
    /**
     * Email has been sent to SES but delivery status is unknown
     */
    SENT,
    
    /**
     * Email has been successfully delivered to recipient
     */
    DELIVERED,
    
    /**
     * Email bounced - recipient address invalid or unreachable
     */
    BOUNCED,
    
    /**
     * Recipient marked email as spam/complaint
     */
    COMPLAINED,
    
    /**
     * Email sending failed
     */
    FAILED,
    
    /**
     * Email is scheduled to be sent later (e.g., due to quiet hours)
     */
    SCHEDULED,
    
    /**
     * Email is pending processing
     */
    PENDING
}