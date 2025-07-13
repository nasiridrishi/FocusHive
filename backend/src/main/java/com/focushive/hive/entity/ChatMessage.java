package com.focushive.hive.entity;

import com.focushive.common.entity.BaseEntity;
import com.focushive.user.entity.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_messages", indexes = {
    @Index(name = "idx_messages_hive", columnList = "hive_id, created_at"),
    @Index(name = "idx_messages_sender", columnList = "sender_id")
})
public class ChatMessage extends BaseEntity {
    
    @NotNull(message = "Hive is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hive_id", nullable = false)
    private Hive hive;
    
    @NotNull(message = "Sender is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private ChatMessage parent;
    
    @NotBlank(message = "Content is required")
    @Size(max = 4000, message = "Content must not exceed 4000 characters")
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;
    
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private MessageType type = MessageType.TEXT;
    
    @Column(columnDefinition = "jsonb")
    private String metadata = "{}";
    
    @Column(name = "is_edited")
    private Boolean isEdited = false;
    
    @Column(name = "edited_at")
    private LocalDateTime editedAt;
    
    @Column(name = "is_pinned")
    private Boolean isPinned = false;
    
    @Column(columnDefinition = "jsonb")
    private String reactions = "{}";
    
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
    
    // Getters and setters
    public Hive getHive() {
        return hive;
    }
    
    public void setHive(Hive hive) {
        this.hive = hive;
    }
    
    public User getSender() {
        return sender;
    }
    
    public void setSender(User sender) {
        this.sender = sender;
    }
    
    public ChatMessage getParent() {
        return parent;
    }
    
    public void setParent(ChatMessage parent) {
        this.parent = parent;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public MessageType getType() {
        return type;
    }
    
    public void setType(MessageType type) {
        this.type = type;
    }
    
    public String getMetadata() {
        return metadata;
    }
    
    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }
    
    public Boolean getIsEdited() {
        return isEdited;
    }
    
    public void setIsEdited(Boolean isEdited) {
        this.isEdited = isEdited;
    }
    
    public LocalDateTime getEditedAt() {
        return editedAt;
    }
    
    public void setEditedAt(LocalDateTime editedAt) {
        this.editedAt = editedAt;
    }
    
    public Boolean getIsPinned() {
        return isPinned;
    }
    
    public void setIsPinned(Boolean isPinned) {
        this.isPinned = isPinned;
    }
    
    public String getReactions() {
        return reactions;
    }
    
    public void setReactions(String reactions) {
        this.reactions = reactions;
    }
    
    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }
    
    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }
    
    public enum MessageType {
        TEXT, IMAGE, FILE, SYSTEM, ANNOUNCEMENT
    }
}