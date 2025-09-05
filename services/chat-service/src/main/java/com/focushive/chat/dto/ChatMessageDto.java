package com.focushive.chat.dto;

import com.focushive.chat.entity.ChatMessage;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.ZonedDateTime;
import java.util.UUID;

public class ChatMessageDto {
    
    private UUID id;
    
    @NotNull(message = "Hive ID is required")
    private UUID hiveId;
    
    private UUID senderId;
    private String senderUsername;
    
    @NotBlank(message = "Message content cannot be empty")
    @Size(max = 1000, message = "Message content cannot exceed 1000 characters")
    private String content;
    
    private ChatMessage.MessageType messageType;
    private Boolean edited;
    private ZonedDateTime editedAt;
    private ZonedDateTime createdAt;
    
    // Constructors
    public ChatMessageDto() {}
    
    public ChatMessageDto(UUID hiveId, String content) {
        this.hiveId = hiveId;
        this.content = content;
        this.messageType = ChatMessage.MessageType.TEXT;
        this.edited = false;
    }
    
    // Static factory method to convert from entity
    public static ChatMessageDto fromEntity(ChatMessage message) {
        ChatMessageDto dto = new ChatMessageDto();
        dto.setId(message.getId());
        dto.setHiveId(message.getHiveId());
        dto.setSenderId(message.getSenderId());
        dto.setSenderUsername(message.getSenderUsername());
        dto.setContent(message.getContent());
        dto.setMessageType(message.getMessageType());
        dto.setEdited(message.getEdited());
        dto.setEditedAt(message.getEditedAt());
        dto.setCreatedAt(message.getCreatedAt());
        return dto;
    }
    
    // Convert to entity
    public ChatMessage toEntity() {
        ChatMessage message = new ChatMessage();
        message.setId(this.id);
        message.setHiveId(this.hiveId);
        message.setSenderId(this.senderId);
        message.setSenderUsername(this.senderUsername);
        message.setContent(this.content);
        message.setMessageType(this.messageType != null ? this.messageType : ChatMessage.MessageType.TEXT);
        message.setEdited(this.edited != null ? this.edited : false);
        message.setEditedAt(this.editedAt);
        message.setCreatedAt(this.createdAt);
        return message;
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
    
    public ChatMessage.MessageType getMessageType() {
        return messageType;
    }
    
    public void setMessageType(ChatMessage.MessageType messageType) {
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
}