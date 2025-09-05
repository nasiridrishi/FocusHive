package com.focushive.chat.service;

import com.focushive.chat.dto.ChatMessageDto;
import com.focushive.chat.dto.MessageHistoryResponse;
import com.focushive.chat.dto.SendMessageRequest;
import com.focushive.chat.entity.ChatMessage;
import com.focushive.chat.repository.ChatMessageRepository;
import com.focushive.chat.service.impl.ChatServiceImpl;
import com.focushive.common.exception.ForbiddenException;
import com.focushive.common.exception.ResourceNotFoundException;
import com.focushive.hive.repository.HiveMemberRepository;
import com.focushive.user.dto.UserDto;
import com.focushive.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatServiceImplTest {

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private HiveMemberRepository hiveMemberRepository;

    @Mock
    private UserService userService;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private ChatServiceImpl chatService;

    private String hiveId;
    private String userId;
    private String messageId;
    private ChatMessage testMessage;
    private UserDto testUser;

    @BeforeEach
    void setUp() {
        hiveId = UUID.randomUUID().toString();
        userId = UUID.randomUUID().toString();
        messageId = UUID.randomUUID().toString();

        testUser = UserDto.builder()
                .id(userId)
                .username("testuser")
                .email("test@example.com")
                .build();

        testMessage = ChatMessage.builder()
                .hiveId(hiveId)
                .senderId(userId)
                .senderUsername("testuser")
                .content("Test message")
                .messageType(ChatMessage.MessageType.TEXT)
                .build();
        testMessage.setId(messageId);
        testMessage.setCreatedAt(LocalDateTime.now());
    }

    @Test
    void sendMessage_Success() {
        // Given
        SendMessageRequest request = SendMessageRequest.builder()
                .content("Hello, world!")
                .build();
        when(hiveMemberRepository.existsByHiveIdAndUserId(hiveId, userId)).thenReturn(true);
        when(userService.getUserById(userId)).thenReturn(testUser);
        when(chatMessageRepository.save(any(ChatMessage.class))).thenReturn(testMessage);

        // When
        ChatMessageDto result = chatService.sendMessage(hiveId, userId, request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEqualTo("Test message");
        assertThat(result.getSenderUsername()).isEqualTo("testuser");

        verify(messagingTemplate).convertAndSend(
                eq("/topic/hive/" + hiveId + "/messages"),
                any(ChatMessageDto.class)
        );
    }

    @Test
    void sendMessage_UserNotMember_ThrowsForbidden() {
        // Given
        SendMessageRequest request = SendMessageRequest.builder()
                .content("Hello")
                .build();
        when(hiveMemberRepository.existsByHiveIdAndUserId(hiveId, userId)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> chatService.sendMessage(hiveId, userId, request))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("You must be a member of the hive to send messages");
    }

    @Test
    void getMessageHistory_Success() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        List<ChatMessage> messages = Arrays.asList(testMessage);
        Page<ChatMessage> messagePage = new PageImpl<>(messages, pageable, 1);

        when(hiveMemberRepository.existsByHiveIdAndUserId(hiveId, userId)).thenReturn(true);
        when(chatMessageRepository.findByHiveIdOrderByCreatedAtDesc(hiveId, pageable))
                .thenReturn(messagePage);

        // When
        MessageHistoryResponse result = chatService.getMessageHistory(hiveId, userId, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getMessages()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getPage()).isEqualTo(0);
    }

    @Test
    void getRecentMessages_Success() {
        // Given
        int limit = 50;
        List<ChatMessage> messages = Arrays.asList(testMessage);
        
        when(hiveMemberRepository.existsByHiveIdAndUserId(hiveId, userId)).thenReturn(true);
        when(chatMessageRepository.findLastMessagesInHive(hiveId, limit)).thenReturn(messages);

        // When
        List<ChatMessageDto> result = chatService.getRecentMessages(hiveId, userId, limit);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getContent()).isEqualTo("Test message");
    }

    @Test
    void getMessagesAfter_Success() {
        // Given
        LocalDateTime after = LocalDateTime.now().minusHours(1);
        List<ChatMessage> messages = Arrays.asList(testMessage);
        
        when(hiveMemberRepository.existsByHiveIdAndUserId(hiveId, userId)).thenReturn(true);
        when(chatMessageRepository.findByHiveIdAndCreatedAtAfterOrderByCreatedAtAsc(hiveId, after))
                .thenReturn(messages);

        // When
        List<ChatMessageDto> result = chatService.getMessagesAfter(hiveId, userId, after);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(messageId);
    }

    @Test
    void editMessage_Success() {
        // Given
        String newContent = "Edited message";
        when(chatMessageRepository.findById(messageId)).thenReturn(Optional.of(testMessage));
        when(chatMessageRepository.save(any(ChatMessage.class))).thenReturn(testMessage);

        // When
        ChatMessageDto result = chatService.editMessage(messageId, userId, newContent);

        // Then
        assertThat(result).isNotNull();
        assertThat(testMessage.isEdited()).isTrue();
        assertThat(testMessage.getContent()).isEqualTo(newContent);
        assertThat(testMessage.getEditedAt()).isNotNull();

        verify(messagingTemplate).convertAndSend(
                eq("/topic/hive/" + hiveId + "/messages/edit"),
                any(ChatMessageDto.class)
        );
    }

    @Test
    void editMessage_NotOwner_ThrowsForbidden() {
        // Given
        String differentUserId = UUID.randomUUID().toString();
        when(chatMessageRepository.findById(messageId)).thenReturn(Optional.of(testMessage));

        // When & Then
        assertThatThrownBy(() -> chatService.editMessage(messageId, differentUserId, "New content"))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("You can only edit your own messages");
    }

    @Test
    void deleteMessage_AsOwner_Success() {
        // Given
        when(chatMessageRepository.findById(messageId)).thenReturn(Optional.of(testMessage));
        when(chatMessageRepository.save(any(ChatMessage.class))).thenReturn(testMessage);

        // When
        chatService.deleteMessage(messageId, userId);

        // Then
        assertThat(testMessage.getContent()).isEqualTo("[Message deleted]");
        assertThat(testMessage.isEdited()).isTrue();
        verify(messagingTemplate).convertAndSend("/topic/hive/" + hiveId + "/messages/delete", messageId);
    }

    @Test
    void deleteMessage_AsModerator_Success() {
        // Given
        String moderatorId = UUID.randomUUID().toString();
        when(chatMessageRepository.findById(messageId)).thenReturn(Optional.of(testMessage));
        when(hiveMemberRepository.isUserModeratorOrOwner(hiveId, moderatorId)).thenReturn(true);

        // When
        chatService.deleteMessage(messageId, moderatorId);

        // Then
        assertThat(testMessage.getContent()).isEqualTo("[Message deleted]");
        verify(chatMessageRepository).save(testMessage);
    }

    @Test
    void deleteMessage_NotAuthorized_ThrowsForbidden() {
        // Given
        String otherUserId = UUID.randomUUID().toString();
        when(chatMessageRepository.findById(messageId)).thenReturn(Optional.of(testMessage));
        when(hiveMemberRepository.isUserModeratorOrOwner(hiveId, otherUserId)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> chatService.deleteMessage(messageId, otherUserId))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("You don't have permission to delete this message");
    }

    @Test
    void sendSystemMessage_Success() {
        // Given
        String content = "System announcement";
        ChatMessage systemMessage = ChatMessage.builder()
                .hiveId(hiveId)
                .senderId("system")
                .senderUsername("System")
                .content(content)
                .messageType(ChatMessage.MessageType.SYSTEM)
                .build();
        systemMessage.setId(UUID.randomUUID().toString());
        
        when(chatMessageRepository.save(any(ChatMessage.class))).thenReturn(systemMessage);

        // When
        ChatMessageDto result = chatService.sendSystemMessage(hiveId, content);

        // Then
        assertThat(result.getMessageType()).isEqualTo(ChatMessage.MessageType.SYSTEM);
        assertThat(result.getSenderUsername()).isEqualTo("System");
        verify(messagingTemplate).convertAndSend(
                eq("/topic/hive/" + hiveId + "/messages"),
                any(ChatMessageDto.class)
        );
    }

    @Test
    void broadcastUserJoined_Success() {
        // Given
        String username = "newuser";
        when(chatMessageRepository.save(any(ChatMessage.class))).thenAnswer(i -> i.getArgument(0));

        // When
        chatService.broadcastUserJoined(hiveId, userId, username);

        // Then
        ArgumentCaptor<ChatMessage> messageCaptor = ArgumentCaptor.forClass(ChatMessage.class);
        verify(chatMessageRepository).save(messageCaptor.capture());
        
        ChatMessage savedMessage = messageCaptor.getValue();
        assertThat(savedMessage.getMessageType()).isEqualTo(ChatMessage.MessageType.JOIN);
        assertThat(savedMessage.getContent()).isEqualTo("newuser joined the hive");
        
        verify(messagingTemplate).convertAndSend(
                eq("/topic/hive/" + hiveId + "/messages"),
                any(ChatMessageDto.class)
        );
    }

    @Test
    void broadcastUserLeft_Success() {
        // Given
        String username = "departinguser";
        when(chatMessageRepository.save(any(ChatMessage.class))).thenAnswer(i -> i.getArgument(0));

        // When
        chatService.broadcastUserLeft(hiveId, userId, username);

        // Then
        ArgumentCaptor<ChatMessage> messageCaptor = ArgumentCaptor.forClass(ChatMessage.class);
        verify(chatMessageRepository).save(messageCaptor.capture());
        
        ChatMessage savedMessage = messageCaptor.getValue();
        assertThat(savedMessage.getMessageType()).isEqualTo(ChatMessage.MessageType.LEAVE);
        assertThat(savedMessage.getContent()).isEqualTo("departinguser left the hive");
    }
}