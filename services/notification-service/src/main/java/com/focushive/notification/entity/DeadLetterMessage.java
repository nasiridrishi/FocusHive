package com.focushive.notification.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entity for storing dead letter queue messages.
 * Maintains a persistent record of failed notifications for manual review and retry.
 */
@Entity
@Table(name = "dead_letter_messages", indexes = {
    @Index(name = "idx_dlq_status", columnList = "status"),
    @Index(name = "idx_dlq_created_at", columnList = "created_at"),
    @Index(name = "idx_dlq_recipient", columnList = "recipient")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"content"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class DeadLetterMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "message_id", length = 255)
    private String messageId;

    @Column(name = "recipient", nullable = false)
    private String recipient;

    @Column(name = "subject")
    private String subject;

    @Lob
    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "retry_count", nullable = false)
    @Builder.Default
    private Integer retryCount = 0;

    @Column(name = "original_queue")
    private String originalQueue;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private Status status = Status.PENDING;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "retried_at")
    private LocalDateTime retriedAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "resolution", length = 500)
    private String resolution;

    @Column(name = "notification_type")
    private String notificationType;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "priority")
    private String priority;

    /**
     * Status of dead letter message processing.
     */
    public enum Status {
        /**
         * Message is pending review/action.
         */
        PENDING,

        /**
         * Message has been retried.
         */
        RETRIED,

        /**
         * Retry attempt failed.
         */
        RETRY_FAILED,

        /**
         * Message has been manually resolved.
         */
        RESOLVED,

        /**
         * Maximum retry attempts exceeded.
         */
        MAX_RETRIES_EXCEEDED,

        /**
         * Message has expired and can be deleted.
         */
        EXPIRED,

        /**
         * Message is being processed.
         */
        PROCESSING
    }

    /**
     * Check if the message can be retried.
     */
    public boolean canRetry() {
        return status == Status.PENDING ||
               status == Status.RETRY_FAILED &&
               retryCount < 3;
    }

    /**
     * Check if the message is in a terminal state.
     */
    public boolean isTerminal() {
        return status == Status.RESOLVED ||
               status == Status.MAX_RETRIES_EXCEEDED ||
               status == Status.EXPIRED;
    }
}