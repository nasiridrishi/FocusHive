package com.focushive.chat.entity;

import com.focushive.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity representing a chat message in a hive.
 */
@Entity
@Table(name = "chat_messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessage extends BaseEntity {
    
    @Column(name = "hive_id", nullable = false)
    private String hiveId;
    
    @Column(name = "sender_id", nullable = false)
    private String senderId;
    
    @Column(name = "sender_username", nullable = false)
    private String senderUsername;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false)
    @Builder.Default
    private MessageType messageType = MessageType.TEXT;
    
    @Column(nullable = false)
    @Builder.Default
    private boolean edited = false;
    
    @Column(name = "edited_at")
    private LocalDateTime editedAt;
    
    public enum MessageType {
        TEXT,
        SYSTEM,
        JOIN,
        LEAVE,
        ANNOUNCEMENT
    }
}