package com.focushive.websocket.controller;

import com.focushive.forum.dto.*;
import com.focushive.forum.service.ForumService;
import com.focushive.websocket.dto.WebSocketMessage;
import com.focushive.websocket.dto.NotificationMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ForumWebSocketController {
    
    private final SimpMessagingTemplate messagingTemplate;
    private final ForumService forumService;
    
    // New post notification
    @MessageMapping("/forum/post/create")
    @SendTo("/topic/forum/posts")
    public WebSocketMessage<ForumPostDTO> createPost(
            @Payload ForumPostDTO post,
            Principal principal) {
        
        log.info("Creating forum post via WebSocket: {}", post.getTitle());
        
        Long userId = getUserIdFromPrincipal(principal);
        ForumPostDTO created = forumService.createPost(userId, post);
        
        WebSocketMessage<ForumPostDTO> message = WebSocketMessage.<ForumPostDTO>builder()
            .id(UUID.randomUUID().toString())
            .type(WebSocketMessage.MessageType.FORUM_NEW_POST)
            .event("forum.post.created")
            .payload(created)
            .senderId(userId.toString())
            .senderUsername(principal.getName())
            .timestamp(LocalDateTime.now())
            .build();
        
        // Also send to category-specific topic
        messagingTemplate.convertAndSend(
            "/topic/forum/category/" + created.getCategoryId() + "/posts",
            message
        );
        
        // Check for mentions and notify mentioned users
        notifyMentions(created.getContent(), created.getId(), null, userId);
        
        return message;
    }
    
    // New reply notification
    @MessageMapping("/forum/reply/create")
    public void createReply(
            @Payload ForumReplyDTO reply,
            Principal principal) {
        
        Long userId = getUserIdFromPrincipal(principal);
        ForumReplyDTO created = forumService.createReply(reply.getPostId(), userId, reply);
        
        WebSocketMessage<ForumReplyDTO> message = WebSocketMessage.<ForumReplyDTO>builder()
            .id(UUID.randomUUID().toString())
            .type(WebSocketMessage.MessageType.FORUM_NEW_REPLY)
            .event("forum.reply.created")
            .payload(created)
            .senderId(userId.toString())
            .senderUsername(principal.getName())
            .timestamp(LocalDateTime.now())
            .build();
        
        // Send to post-specific topic
        messagingTemplate.convertAndSend(
            "/topic/forum/post/" + reply.getPostId() + "/replies",
            message
        );
        
        // Notify post author
        ForumPostDTO post = forumService.getPost(reply.getPostId());
        if (!post.getUserId().equals(userId)) {
            NotificationMessage notification = NotificationMessage.builder()
                .id(UUID.randomUUID().toString())
                .type(NotificationMessage.NotificationType.FORUM_REPLY)
                .title("New Reply to Your Post")
                .message(principal.getName() + " replied to \"" + post.getTitle() + "\"")
                .actionUrl("/forum/post/" + post.getId())
                .data(Map.of("postId", post.getId(), "replyId", created.getId()))
                .priority(NotificationMessage.NotificationPriority.NORMAL)
                .createdAt(LocalDateTime.now())
                .build();
            
            sendNotificationToUser(post.getUserId(), notification);
        }
        
        // Check for mentions
        notifyMentions(created.getContent(), reply.getPostId(), created.getId(), userId);
    }
    
    // Vote notification
    @MessageMapping("/forum/vote")
    public void vote(
            @Payload Map<String, Object> voteData,
            Principal principal) {
        
        Long userId = getUserIdFromPrincipal(principal);
        Long postId = voteData.get("postId") != null ? 
            Long.parseLong(voteData.get("postId").toString()) : null;
        Long replyId = voteData.get("replyId") != null ? 
            Long.parseLong(voteData.get("replyId").toString()) : null;
        Integer voteType = Integer.parseInt(voteData.get("voteType").toString());
        
        ForumVoteDTO vote;
        WebSocketMessage.MessageType messageType;
        String targetTopic;
        
        if (postId != null) {
            vote = forumService.voteOnPost(postId, userId, voteType);
            messageType = WebSocketMessage.MessageType.FORUM_POST_VOTED;
            targetTopic = "/topic/forum/post/" + postId + "/votes";
        } else {
            vote = forumService.voteOnReply(replyId, userId, voteType);
            messageType = WebSocketMessage.MessageType.FORUM_REPLY_VOTED;
            ForumReplyDTO reply = forumService.getReply(replyId);
            targetTopic = "/topic/forum/post/" + reply.getPostId() + "/votes";
        }
        
        WebSocketMessage<ForumVoteDTO> message = WebSocketMessage.<ForumVoteDTO>builder()
            .id(UUID.randomUUID().toString())
            .type(messageType)
            .event("forum.vote.cast")
            .payload(vote)
            .senderId(userId.toString())
            .senderUsername(principal.getName())
            .timestamp(LocalDateTime.now())
            .build();
        
        // Send vote update to topic
        messagingTemplate.convertAndSend(targetTopic, message);
    }
    
    // Accept reply as answer
    @MessageMapping("/forum/reply/{replyId}/accept")
    public void acceptReply(
            @DestinationVariable Long replyId,
            Principal principal) {
        
        Long userId = getUserIdFromPrincipal(principal);
        forumService.acceptReply(replyId, userId);
        
        ForumReplyDTO reply = forumService.getReply(replyId);
        
        WebSocketMessage<ForumReplyDTO> message = WebSocketMessage.<ForumReplyDTO>builder()
            .id(UUID.randomUUID().toString())
            .type(WebSocketMessage.MessageType.FORUM_REPLY_ACCEPTED)
            .event("forum.reply.accepted")
            .payload(reply)
            .senderId(userId.toString())
            .senderUsername(principal.getName())
            .timestamp(LocalDateTime.now())
            .build();
        
        // Send to post topic
        messagingTemplate.convertAndSend(
            "/topic/forum/post/" + reply.getPostId() + "/updates",
            message
        );
        
        // Notify reply author
        if (!reply.getUserId().equals(userId)) {
            NotificationMessage notification = NotificationMessage.builder()
                .id(UUID.randomUUID().toString())
                .type(NotificationMessage.NotificationType.FORUM_ACCEPTED_ANSWER)
                .title("Your Answer Was Accepted!")
                .message("Your reply was marked as the accepted answer")
                .actionUrl("/forum/post/" + reply.getPostId())
                .priority(NotificationMessage.NotificationPriority.HIGH)
                .data(Map.of("postId", reply.getPostId(), "replyId", replyId))
                .createdAt(LocalDateTime.now())
                .build();
            
            sendNotificationToUser(reply.getUserId(), notification);
        }
    }
    
    // Post edited notification
    @MessageMapping("/forum/post/{postId}/edit")
    @SendTo("/topic/forum/post/{postId}/updates")
    public WebSocketMessage<ForumPostDTO> editPost(
            @DestinationVariable Long postId,
            @Payload ForumPostDTO post,
            Principal principal) {
        
        Long userId = getUserIdFromPrincipal(principal);
        ForumPostDTO updated = forumService.updatePost(postId, userId, post);
        
        return WebSocketMessage.<ForumPostDTO>builder()
            .id(UUID.randomUUID().toString())
            .type(WebSocketMessage.MessageType.FORUM_POST_EDITED)
            .event("forum.post.edited")
            .payload(updated)
            .senderId(userId.toString())
            .senderUsername(principal.getName())
            .timestamp(LocalDateTime.now())
            .build();
    }
    
    // User typing indicator
    @MessageMapping("/forum/post/{postId}/typing")
    @SendTo("/topic/forum/post/{postId}/typing")
    public Map<String, Object> userTyping(
            @DestinationVariable Long postId,
            @Payload Map<String, Boolean> typingData,
            Principal principal) {
        
        return Map.of(
            "userId", getUserIdFromPrincipal(principal),
            "username", principal.getName(),
            "isTyping", typingData.get("isTyping"),
            "postId", postId,
            "timestamp", LocalDateTime.now()
        );
    }
    
    // Helper method to detect and notify mentions
    private void notifyMentions(String content, Long postId, Long replyId, Long authorId) {
        // Pattern to detect @username mentions
        Pattern mentionPattern = Pattern.compile("@([a-zA-Z0-9_]+)");
        Matcher matcher = mentionPattern.matcher(content);
        
        while (matcher.find()) {
            String username = matcher.group(1);
            // In a real implementation, look up user by username
            // For now, we'll skip if we can't find the user
            
            NotificationMessage notification = NotificationMessage.builder()
                .id(UUID.randomUUID().toString())
                .type(NotificationMessage.NotificationType.FORUM_MENTION)
                .title("You were mentioned")
                .message("You were mentioned in a " + (replyId != null ? "reply" : "post"))
                .actionUrl("/forum/post/" + postId)
                .priority(NotificationMessage.NotificationPriority.HIGH)
                .data(Map.of(
                    "postId", postId,
                    "replyId", replyId != null ? replyId : "",
                    "mentionedBy", authorId
                ))
                .createdAt(LocalDateTime.now())
                .build();
            
            // Send notification to mentioned user
            // In real implementation, would look up user ID by username
            // sendNotificationToUser(mentionedUserId, notification);
        }
    }
    
    // Helper method to send notification to specific user
    private void sendNotificationToUser(Long userId, NotificationMessage notification) {
        WebSocketMessage<NotificationMessage> message = WebSocketMessage.<NotificationMessage>builder()
            .id(UUID.randomUUID().toString())
            .type(WebSocketMessage.MessageType.NOTIFICATION)
            .event("notification.received")
            .payload(notification)
            .timestamp(LocalDateTime.now())
            .build();
        
        messagingTemplate.convertAndSendToUser(
            userId.toString(),
            "/queue/notifications",
            message
        );
    }
    
    // Helper method to extract user ID from principal
    private Long getUserIdFromPrincipal(Principal principal) {
        // In a real implementation, extract from authentication token
        return Long.parseLong(principal.getName());
    }
}