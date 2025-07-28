package com.focushive.chat.service.impl;

import com.focushive.chat.dto.ChatMessageDto;
import com.focushive.chat.dto.MessageHistoryResponse;
import com.focushive.chat.dto.SendMessageRequest;
import com.focushive.chat.entity.ChatMessage;
import com.focushive.chat.repository.ChatMessageRepository;
import com.focushive.chat.service.ChatService;
import com.focushive.common.exception.ForbiddenException;
import com.focushive.common.exception.ResourceNotFoundException;
import com.focushive.hive.repository.HiveMemberRepository;
import com.focushive.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of ChatService.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {
    
    private final ChatMessageRepository chatMessageRepository;
    private final HiveMemberRepository hiveMemberRepository;
    private final UserService userService;
    private final SimpMessagingTemplate messagingTemplate;
    
    @Override
    @Transactional
    public ChatMessageDto sendMessage(String hiveId, String senderId, SendMessageRequest request) {
        log.debug("User {} sending message to hive {}", senderId, hiveId);
        
        // Verify user is member of hive
        if (!hiveMemberRepository.existsByHiveIdAndUserId(hiveId, senderId)) {
            throw new ForbiddenException("You must be a member of the hive to send messages");
        }
        
        // Get sender info
        var sender = userService.getUserById(senderId);
        
        // Create and save message
        ChatMessage message = ChatMessage.builder()
                .hiveId(hiveId)
                .senderId(senderId)
                .senderUsername(sender.getUsername())
                .content(request.getContent())
                .messageType(ChatMessage.MessageType.TEXT)
                .build();
        
        message = chatMessageRepository.save(message);
        
        // Convert to DTO
        ChatMessageDto messageDto = convertToDto(message);
        
        // Broadcast to hive members via WebSocket
        messagingTemplate.convertAndSend(
                "/topic/hive/" + hiveId + "/messages",
                messageDto
        );
        
        return messageDto;
    }
    
    @Override
    @Transactional(readOnly = true)
    public MessageHistoryResponse getMessageHistory(String hiveId, String userId, Pageable pageable) {
        // Verify user is member of hive
        if (!hiveMemberRepository.existsByHiveIdAndUserId(hiveId, userId)) {
            throw new ForbiddenException("You must be a member of the hive to view messages");
        }
        
        Page<ChatMessage> messagePage = chatMessageRepository.findByHiveIdOrderByCreatedAtDesc(hiveId, pageable);
        
        List<ChatMessageDto> messages = messagePage.getContent().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
        
        // Reverse to show oldest first in the page
        Collections.reverse(messages);
        
        return MessageHistoryResponse.builder()
                .messages(messages)
                .page(messagePage.getNumber())
                .size(messagePage.getSize())
                .totalElements(messagePage.getTotalElements())
                .totalPages(messagePage.getTotalPages())
                .hasNext(messagePage.hasNext())
                .hasPrevious(messagePage.hasPrevious())
                .build();
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<ChatMessageDto> getRecentMessages(String hiveId, String userId, int limit) {
        // Verify user is member of hive
        if (!hiveMemberRepository.existsByHiveIdAndUserId(hiveId, userId)) {
            throw new ForbiddenException("You must be a member of the hive to view messages");
        }
        
        List<ChatMessage> messages = chatMessageRepository.findLastMessagesInHive(hiveId, limit);
        
        // Convert and reverse to show oldest first
        List<ChatMessageDto> messageDtos = messages.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
        Collections.reverse(messageDtos);
        
        return messageDtos;
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<ChatMessageDto> getMessagesAfter(String hiveId, String userId, LocalDateTime after) {
        // Verify user is member of hive
        if (!hiveMemberRepository.existsByHiveIdAndUserId(hiveId, userId)) {
            throw new ForbiddenException("You must be a member of the hive to view messages");
        }
        
        return chatMessageRepository.findByHiveIdAndCreatedAtAfterOrderByCreatedAtAsc(hiveId, after)
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional
    public ChatMessageDto editMessage(String messageId, String userId, String newContent) {
        ChatMessage message = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found"));
        
        // Only sender can edit their message
        if (!message.getSenderId().equals(userId)) {
            throw new ForbiddenException("You can only edit your own messages");
        }
        
        // Update message
        message.setContent(newContent);
        message.setEdited(true);
        message.setEditedAt(LocalDateTime.now());
        
        message = chatMessageRepository.save(message);
        
        ChatMessageDto messageDto = convertToDto(message);
        
        // Broadcast edit to hive members
        messagingTemplate.convertAndSend(
                "/topic/hive/" + message.getHiveId() + "/messages/edit",
                messageDto
        );
        
        return messageDto;
    }
    
    @Override
    @Transactional
    public void deleteMessage(String messageId, String userId) {
        ChatMessage message = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found"));
        
        // Check if user is sender or hive moderator/owner
        boolean canDelete = message.getSenderId().equals(userId) ||
                hiveMemberRepository.isUserModeratorOrOwner(message.getHiveId(), userId);
        
        if (!canDelete) {
            throw new ForbiddenException("You don't have permission to delete this message");
        }
        
        // Soft delete by replacing content
        message.setContent("[Message deleted]");
        message.setEdited(true);
        message.setEditedAt(LocalDateTime.now());
        
        chatMessageRepository.save(message);
        
        // Broadcast deletion
        messagingTemplate.convertAndSend(
                "/topic/hive/" + message.getHiveId() + "/messages/delete",
                messageId
        );
    }
    
    @Override
    @Transactional
    public ChatMessageDto sendSystemMessage(String hiveId, String content) {
        ChatMessage message = ChatMessage.builder()
                .hiveId(hiveId)
                .senderId("system")
                .senderUsername("System")
                .content(content)
                .messageType(ChatMessage.MessageType.SYSTEM)
                .build();
        
        message = chatMessageRepository.save(message);
        
        ChatMessageDto messageDto = convertToDto(message);
        
        // Broadcast system message
        messagingTemplate.convertAndSend(
                "/topic/hive/" + hiveId + "/messages",
                messageDto
        );
        
        return messageDto;
    }
    
    @Override
    public void broadcastUserJoined(String hiveId, String userId, String username) {
        String content = username + " joined the hive";
        
        ChatMessage message = ChatMessage.builder()
                .hiveId(hiveId)
                .senderId(userId)
                .senderUsername(username)
                .content(content)
                .messageType(ChatMessage.MessageType.JOIN)
                .build();
        
        chatMessageRepository.save(message);
        
        messagingTemplate.convertAndSend(
                "/topic/hive/" + hiveId + "/messages",
                convertToDto(message)
        );
    }
    
    @Override
    public void broadcastUserLeft(String hiveId, String userId, String username) {
        String content = username + " left the hive";
        
        ChatMessage message = ChatMessage.builder()
                .hiveId(hiveId)
                .senderId(userId)
                .senderUsername(username)
                .content(content)
                .messageType(ChatMessage.MessageType.LEAVE)
                .build();
        
        chatMessageRepository.save(message);
        
        messagingTemplate.convertAndSend(
                "/topic/hive/" + hiveId + "/messages",
                convertToDto(message)
        );
    }
    
    private ChatMessageDto convertToDto(ChatMessage message) {
        return ChatMessageDto.builder()
                .id(message.getId())
                .hiveId(message.getHiveId())
                .senderId(message.getSenderId())
                .senderUsername(message.getSenderUsername())
                .content(message.getContent())
                .messageType(message.getMessageType())
                .edited(message.isEdited())
                .editedAt(message.getEditedAt())
                .createdAt(message.getCreatedAt())
                .build();
    }
}