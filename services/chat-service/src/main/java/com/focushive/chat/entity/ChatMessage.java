package com.focushive.chat.entity;

import jakarta.persistence.*;
import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Table(name = "chat_messages")
public class ChatMessage {
    
    @Id
    @GeneratedValue
    private UUID id;
    
    @Column(name = "hive_id", nullable = false)
    private UUID hiveId;
    
    @Column(name = "sender_id", nullable = false)
    private UUID senderId;
    
    @Column(name = "sender_username", nullable = false)
    private String senderUsername;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false)
    private MessageType messageType = MessageType.TEXT;
    
    @Column(nullable = false)
    private Boolean edited = false;
    
    @Column(name = "edited_at")
    private ZonedDateTime editedAt;
    
    @Column(name = "created_at", nullable = false)
    private ZonedDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = ZonedDateTime.now();
        }
    }
    
    // Constructors
    public ChatMessage() {}
    
    public ChatMessage(UUID hiveId, UUID senderId, String senderUsername, String content) {
        this.hiveId = hiveId;
        this.senderId = senderId;
        this.senderUsername = senderUsername;
        this.content = content;
        this.messageType = MessageType.TEXT;
        this.edited = false;
        this.createdAt = ZonedDateTime.now();
    }
    
    // Getters and Setters
    public UUID getId() {
        return id;
    }
    
    public void setId(UUID id) {
        this.id = id;
    }
    
    public UUID getHiveId() {
        return hiveId;
    }
    
    public void setHiveId(UUID hiveId) {
        this.hiveId = hiveId;
    }
    
    public UUID getSenderId() {
        return senderId;
    }
    
    public void setSenderId(UUID senderId) {
        this.senderId = senderId;
    }
    
    public String getSenderUsername() {
        return senderUsername;
    }
    
    public void setSenderUsername(String senderUsername) {
        this.senderUsername = senderUsername;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public MessageType getMessageType() {
        return messageType;
    }
    
    public void setMessageType(MessageType messageType) {
        this.messageType = messageType;
    }
    
    public Boolean getEdited() {
        return edited;
    }
    
    public void setEdited(Boolean edited) {
        this.edited = edited;
    }
    
    public ZonedDateTime getEditedAt() {
        return editedAt;
    }
    
    public void setEditedAt(ZonedDateTime editedAt) {
        this.editedAt = editedAt;
    }
    
    public ZonedDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(ZonedDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public enum MessageType {
        TEXT,
        IMAGE,
        FILE,
        SYSTEM
    }
}