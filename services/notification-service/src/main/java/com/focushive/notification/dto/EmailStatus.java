package com.focushive.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Email status tracking DTO.
 * Tracks the lifecycle of an email from queuing to delivery.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailStatus {

    /**
     * Unique tracking ID for the email.
     */
    private String trackingId;

    /**
     * Current status of the email.
     */
    private Status status;

    /**
     * Timestamp when the status was last updated.
     */
    private Instant timestamp;

    /**
     * Error message if the email failed.
     */
    private String errorMessage;

    /**
     * Number of retry attempts made.
     */
    @Builder.Default
    private int retryCount = 0;

    /**
     * Processing time in milliseconds.
     */
    private Long processingTimeMs;

    /**
     * The recipient email address.
     */
    private String recipient;

    /**
     * The email subject for reference.
     */
    private String subject;

    /**
     * Constructor for basic status update.
     */
    public EmailStatus(String trackingId, Status status, Instant timestamp, String errorMessage) {
        this.trackingId = trackingId;
        this.status = status;
        this.timestamp = timestamp;
        this.errorMessage = errorMessage;
    }

    /**
     * Email status values representing the lifecycle.
     */
    public enum Status {
        /**
         * Email has been queued for processing.
         */
        QUEUED,

        /**
         * Email is currently being sent.
         */
        SENDING,

        /**
         * Email has been successfully sent.
         */
        SENT,

        /**
         * Email delivery failed after retries.
         */
        FAILED,

        /**
         * Email moved to dead letter queue.
         */
        DEAD_LETTER,

        /**
         * Email was bounced back.
         */
        BOUNCED,

        /**
         * Email status is unknown.
         */
        UNKNOWN,

        /**
         * Email is being retried.
         */
        RETRYING,

        /**
         * Email was cancelled before sending.
         */
        CANCELLED
    }

    /**
     * Checks if the email is in a terminal state (no further processing).
     */
    public boolean isTerminal() {
        return status == Status.SENT ||
               status == Status.FAILED ||
               status == Status.DEAD_LETTER ||
               status == Status.BOUNCED ||
               status == Status.CANCELLED;
    }

    /**
     * Checks if the email can be retried.
     */
    public boolean canRetry() {
        return (status == Status.FAILED || status == Status.RETRYING) && retryCount < 3;
    }
}