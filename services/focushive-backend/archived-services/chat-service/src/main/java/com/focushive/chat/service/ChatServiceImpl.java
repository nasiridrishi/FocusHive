package com.focushive.chat.service;

import com.focushive.chat.dto.ChatMessageDto;
import com.focushive.chat.dto.TypingIndicatorDto;
import com.focushive.chat.entity.ChatMessage;
import com.focushive.chat.entity.TypingIndicator;
import com.focushive.chat.repository.ChatMessageRepository;
import com.focushive.chat.repository.TypingIndicatorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Transactional
public class ChatServiceImpl implements ChatService {
    
    private static final Logger logger = LoggerFactory.getLogger(ChatServiceImpl.class);
    
    @Autowired
    private ChatMessageRepository messageRepository;
    
    @Autowired
    private TypingIndicatorRepository typingIndicatorRepository;
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    private static final String TYPING_CHANNEL_PREFIX = "chat:typing:";
    private static final String MESSAGE_CHANNEL_PREFIX = "chat:message:";
    
    @Override
    public ChatMessageDto sendMessage(ChatMessageDto messageDto, UUID senderId, String senderUsername) {
        logger.info("Sending message from user {} to hive {}", senderId, messageDto.getHiveId());
        
        // Set sender information
        messageDto.setSenderId(senderId);
        messageDto.setSenderUsername(senderUsername);
        messageDto.setCreatedAt(ZonedDateTime.now());
        
        // Save to database
        ChatMessage savedMessage = messageRepository.save(messageDto.toEntity());
        ChatMessageDto savedDto = ChatMessageDto.fromEntity(savedMessage);
        
        // Remove typing indicator if exists
        removeTypingIndicator(messageDto.getHiveId(), senderId);
        
        // Broadcast message via WebSocket
        broadcastMessage(savedDto);
        
        // Cache recent messages
        invalidateHiveMessagesCache(messageDto.getHiveId());
        
        logger.info("Message sent successfully with ID: {}", savedMessage.getId());
        return savedDto;
    }
    
    @Override
    @Cacheable(value = "hiveMessages", key = "#hiveId + '_' + #pageable.pageNumber + '_' + #pageable.pageSize")
    public Page<ChatMessageDto> getHiveMessages(UUID hiveId, Pageable pageable) {
        logger.debug("Fetching messages for hive {} with pagination", hiveId);
        
        Page<ChatMessage> messages = messageRepository.findByHiveIdOrderByCreatedAtDesc(hiveId, pageable);
        return messages.map(ChatMessageDto::fromEntity);
    }
    
    @Override
    @Cacheable(value = "recentMessages", key = "#hiveId + '_' + #limit")
    public List<ChatMessageDto> getRecentMessages(UUID hiveId, int limit) {
        logger.debug("Fetching {} recent messages for hive {}", limit, hiveId);
        
        List<ChatMessage> messages = messageRepository.findRecentMessagesInHive(hiveId, limit);
        return messages.stream()
                .map(ChatMessageDto::fromEntity)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<ChatMessageDto> getMessagesAfter(UUID hiveId, ZonedDateTime after) {
        logger.debug("Fetching messages for hive {} after timestamp {}", hiveId, after);
        
        List<ChatMessage> messages = messageRepository.findByHiveIdAndCreatedAtAfterOrderByCreatedAtAsc(hiveId, after);
        return messages.stream()
                .map(ChatMessageDto::fromEntity)
                .collect(Collectors.toList());
    }
    
    @Override
    public Page<ChatMessageDto> searchMessages(UUID hiveId, String searchTerm, Pageable pageable) {
        logger.debug("Searching messages in hive {} for term: {}", hiveId, searchTerm);
        
        Page<ChatMessage> messages = messageRepository.searchMessagesInHive(hiveId, searchTerm, pageable);
        return messages.map(ChatMessageDto::fromEntity);
    }
    
    @Override
    public ChatMessageDto editMessage(UUID messageId, String newContent, UUID userId) {
        logger.info("Editing message {} by user {}", messageId, userId);
        
        ChatMessage message = messageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Message not found"));
        
        // Verify user can edit this message
        if (!message.getSenderId().equals(userId)) {
            throw new SecurityException("User not authorized to edit this message");
        }
        
        message.setContent(newContent);
        message.setEdited(true);
        message.setEditedAt(ZonedDateTime.now());
        
        ChatMessage savedMessage = messageRepository.save(message);
        ChatMessageDto editedDto = ChatMessageDto.fromEntity(savedMessage);
        
        // Broadcast edited message
        broadcastMessageEdit(editedDto);
        
        // Invalidate cache
        invalidateHiveMessagesCache(message.getHiveId());
        
        return editedDto;
    }
    
    @Override
    public void deleteMessage(UUID messageId, UUID userId) {
        logger.info("Deleting message {} by user {}", messageId, userId);
        
        ChatMessage message = messageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Message not found"));
        
        // Verify user can delete this message
        if (!message.getSenderId().equals(userId)) {
            throw new SecurityException("User not authorized to delete this message");
        }
        
        // Soft delete - replace content with deletion marker
        message.setContent("[Message deleted]");
        message.setMessageType(ChatMessage.MessageType.SYSTEM);
        message.setEdited(true);
        message.setEditedAt(ZonedDateTime.now());
        
        messageRepository.save(message);
        
        // Broadcast deletion
        broadcastMessageDeletion(ChatMessageDto.fromEntity(message));
        
        // Invalidate cache
        invalidateHiveMessagesCache(message.getHiveId());
    }
    
    @Override
    public long getMessageCount(UUID hiveId) {
        return messageRepository.countByHiveId(hiveId);
    }
    
    @Override
    public void setTypingIndicator(UUID hiveId, UUID userId, String username) {
        logger.debug("Setting typing indicator for user {} in hive {}", userId, hiveId);
        
        // Check if typing indicator already exists
        TypingIndicator existingIndicator = typingIndicatorRepository
                .findByHiveIdAndUserId(hiveId, userId)
                .orElse(null);
        
        if (existingIndicator != null) {
            // Extend existing indicator
            existingIndicator.setExpiresAt(ZonedDateTime.now().plusSeconds(10));
            typingIndicatorRepository.save(existingIndicator);
        } else {
            // Create new indicator
            TypingIndicator indicator = new TypingIndicator(hiveId, userId, username);
            typingIndicatorRepository.save(indicator);
        }
        
        // Broadcast typing indicator
        TypingIndicatorDto dto = new TypingIndicatorDto(hiveId, userId, username, true);
        broadcastTypingIndicator(dto);
        
        // Store in Redis with expiry
        String key = TYPING_CHANNEL_PREFIX + hiveId + ":" + userId;
        redisTemplate.opsForValue().set(key, username, 10, TimeUnit.SECONDS);
    }
    
    @Override
    public void removeTypingIndicator(UUID hiveId, UUID userId) {
        logger.debug("Removing typing indicator for user {} in hive {}", userId, hiveId);
        
        // Remove from database
        typingIndicatorRepository.deleteByHiveIdAndUserId(hiveId, userId);
        
        // Remove from Redis
        String key = TYPING_CHANNEL_PREFIX + hiveId + ":" + userId;
        redisTemplate.delete(key);
        
        // Broadcast typing stopped
        TypingIndicatorDto dto = new TypingIndicatorDto(hiveId, userId, "", false);
        broadcastTypingIndicator(dto);
    }
    
    @Override
    public List<TypingIndicatorDto> getActiveTypingIndicators(UUID hiveId) {
        ZonedDateTime now = ZonedDateTime.now();
        List<TypingIndicator> indicators = typingIndicatorRepository
                .findActiveTypingIndicatorsInHive(hiveId, now);
        
        return indicators.stream()
                .map(indicator -> new TypingIndicatorDto(
                        indicator.getHiveId(),
                        indicator.getUserId(),
                        indicator.getUsername(),
                        true
                ))
                .collect(Collectors.toList());
    }
    
    @Override
    public void cleanupExpiredTypingIndicators() {
        logger.debug("Cleaning up expired typing indicators");
        
        ZonedDateTime cutoff = ZonedDateTime.now();
        int deletedCount = typingIndicatorRepository.deleteExpiredIndicators(cutoff);
        
        if (deletedCount > 0) {
            logger.info("Cleaned up {} expired typing indicators", deletedCount);
        }
    }
    
    @Override
    public Object getChatStatistics(UUID hiveId, ZonedDateTime date) {
        return messageRepository.getHourlyMessageStatistics(hiveId, date);
    }
    
    @Override
    public boolean canUserAccessHiveChat(UUID userId, UUID hiveId) {
        // TODO: Implement proper authorization check with hive service
        // For now, return true - this should check if user is member of hive
        return true;
    }
    
    // Private helper methods
    
    private void broadcastMessage(ChatMessageDto message) {
        String destination = "/topic/hive/" + message.getHiveId() + "/chat";
        messagingTemplate.convertAndSend(destination, message);
        
        // Also publish to Redis for cross-instance communication
        String channel = MESSAGE_CHANNEL_PREFIX + message.getHiveId();
        redisTemplate.convertAndSend(channel, message);
    }
    
    private void broadcastMessageEdit(ChatMessageDto message) {
        String destination = "/topic/hive/" + message.getHiveId() + "/chat/edit";
        messagingTemplate.convertAndSend(destination, message);
    }
    
    private void broadcastMessageDeletion(ChatMessageDto message) {
        String destination = "/topic/hive/" + message.getHiveId() + "/chat/delete";
        messagingTemplate.convertAndSend(destination, message);
    }
    
    private void broadcastTypingIndicator(TypingIndicatorDto indicator) {
        String destination = "/topic/hive/" + indicator.getHiveId() + "/typing";
        messagingTemplate.convertAndSend(destination, indicator);
    }
    
    @CacheEvict(value = {"hiveMessages", "recentMessages"}, key = "#hiveId")
    private void invalidateHiveMessagesCache(UUID hiveId) {
        // Cache eviction is handled by annotation
    }
}