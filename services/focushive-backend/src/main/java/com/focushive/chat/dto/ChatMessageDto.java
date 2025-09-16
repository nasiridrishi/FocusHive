package com.focushive.chat.dto;

import com.focushive.chat.enums.MessageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for chat message responses.
 * Enhanced with threading, reactions, and attachment support.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageDto {
    private String id;
    private String hiveId;
    private String senderId;
    private String senderUsername;
    private String content;
    private MessageType messageType;
    private boolean edited;
    private LocalDateTime editedAt;
    private LocalDateTime createdAt;

    // Threading support
    private String parentMessageId;
    private String threadId;
    private Integer replyCount;
    private boolean isReply;
    private boolean hasReplies;

    // Reactions
    private Integer reactionCount;
    private boolean hasReactions;
    private List<MessageReactionResponse> reactions;
    private List<ReactionSummary> reactionSummary;

    // Attachments
    private Integer attachmentCount;
    private boolean hasAttachments;
    private List<MessageAttachmentResponse> attachments;

    // Read receipts
    private Integer readByCount;

    // Pinned status
    private Boolean pinned;
    private LocalDateTime pinnedAt;
    private String pinnedByUserId;

    // Parent message preview (for threaded messages)
    private ChatMessagePreview parentMessage;

    /**
     * Summary of reactions grouped by emoji.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReactionSummary {
        private String emoji;
        private Long count;
        private List<String> usernames; // Sample usernames who reacted
        private boolean currentUserReacted;
    }

    /**
     * Preview of a parent message for threading context.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChatMessagePreview {
        private String id;
        private String senderId;
        private String senderUsername;
        private String content;
        private MessageType messageType;
        private LocalDateTime createdAt;
        private boolean hasAttachments;
    }
}