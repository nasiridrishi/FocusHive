package com.focushive.chat.controller;

import com.focushive.chat.dto.ChatMessageDto;
import com.focushive.chat.dto.SendMessageRequest;
import com.focushive.chat.service.ChatService;
import com.focushive.common.exception.ForbiddenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.security.Principal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatWebSocketControllerTest {

    @Mock
    private ChatService chatService;

    @Mock
    private Principal principal;

    @InjectMocks
    private ChatWebSocketController controller;

    private String hiveId;
    private String userId;
    private String messageId;

    @BeforeEach
    void setUp() {
        hiveId = UUID.randomUUID().toString();
        userId = UUID.randomUUID().toString();
        messageId = UUID.randomUUID().toString();
        
        when(principal.getName()).thenReturn(userId);
    }

    @Test
    void sendMessage_Success() {
        // Given
        SendMessageRequest request = SendMessageRequest.builder()
                .content("Test message")
                .build();
        ChatMessageDto expectedResponse = ChatMessageDto.builder()
                .id(messageId)
                .content("Test message")
                .build();
        
        when(chatService.sendMessage(hiveId, userId, request)).thenReturn(expectedResponse);

        // When
        controller.sendMessage(hiveId, request, principal);

        // Then
        verify(chatService).sendMessage(hiveId, userId, request);
    }

    @Test
    void sendMessage_ServiceThrowsException_PropagatesException() {
        // Given
        SendMessageRequest request = SendMessageRequest.builder()
                .content("Test")
                .build();
        when(chatService.sendMessage(any(), any(), any()))
                .thenThrow(new ForbiddenException("Not a member"));

        // When & Then
        try {
            controller.sendMessage(hiveId, request, principal);
        } catch (ForbiddenException e) {
            assertThat(e.getMessage()).isEqualTo("Not a member");
        }
    }

    @Test
    void editMessage_Success() {
        // Given
        String newContent = "Edited content";
        ChatMessageDto editedMessage = ChatMessageDto.builder()
                .id(messageId)
                .content(newContent)
                .edited(true)
                .build();
        
        when(chatService.editMessage(messageId, userId, newContent)).thenReturn(editedMessage);

        // When
        ChatMessageDto result = controller.editMessage(messageId, newContent, principal);

        // Then
        assertThat(result).isEqualTo(editedMessage);
        verify(chatService).editMessage(messageId, userId, newContent);
    }

    @Test
    void deleteMessage_Success() {
        // Given
        doNothing().when(chatService).deleteMessage(messageId, userId);

        // When
        controller.deleteMessage(messageId, principal);

        // Then
        verify(chatService).deleteMessage(messageId, userId);
    }

    @Test
    void handleTyping_ReturnsTypingIndicator() {
        // Given
        boolean isTyping = true;

        // When
        ChatWebSocketController.TypingIndicator result = controller.handleTyping(hiveId, isTyping, principal);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.userId()).isEqualTo(userId);
        assertThat(result.isTyping()).isTrue();
    }

    @Test
    void handleTyping_NotTyping_ReturnsIndicatorWithFalse() {
        // Given
        boolean isTyping = false;

        // When
        ChatWebSocketController.TypingIndicator result = controller.handleTyping(hiveId, isTyping, principal);

        // Then
        assertThat(result.userId()).isEqualTo(userId);
        assertThat(result.isTyping()).isFalse();
    }
}