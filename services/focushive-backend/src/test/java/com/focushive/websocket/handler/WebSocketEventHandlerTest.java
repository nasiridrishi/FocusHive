package com.focushive.websocket.handler;

import com.focushive.buddy.service.BuddyService;
import com.focushive.websocket.controller.BuddyWebSocketController;
import com.focushive.websocket.dto.WebSocketMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WebSocketEventHandlerTest {
    
    @Mock
    private SimpMessagingTemplate messagingTemplate;
    
    @Mock
    private BuddyService buddyService;
    
    @Mock
    private BuddyWebSocketController buddyWebSocketController;
    
    @Mock
    private SessionConnectedEvent connectEvent;
    
    @Mock
    private SessionDisconnectEvent disconnectEvent;
    
    @Mock
    private StompHeaderAccessor headerAccessor;
    
    @Mock
    private Principal principal;
    
    @Mock
    private Message<byte[]> message;
    
    @InjectMocks
    private WebSocketEventHandler webSocketEventHandler;
    
    private String sessionId;
    private String username;
    
    @BeforeEach
    void setUp() {
        sessionId = "test-session-id";
        username = "testuser";
    }
    
    @Test
    void handleWebSocketConnectListener_WithValidUser_ShouldTrackConnection() {
        // Given
        when(connectEvent.getMessage()).thenReturn(message);
        when(connectEvent.getUser()).thenReturn(principal);
        when(principal.getName()).thenReturn(username);
        when(headerAccessor.getSessionId()).thenReturn(sessionId);
        
        // Mock the static method StompHeaderAccessor.wrap
        try (MockedStatic<StompHeaderAccessor> mockedStatic = mockStatic(StompHeaderAccessor.class)) {
            mockedStatic.when(() -> StompHeaderAccessor.wrap(message)).thenReturn(headerAccessor);
            
            // When
            webSocketEventHandler.handleWebSocketConnectListener(connectEvent);
            
            // Then
            verify(messagingTemplate).convertAndSend(
                eq("/topic/presence"), 
                any(WebSocketMessage.class)
            );
        }
    }
    
    @Test
    void handleWebSocketDisconnectListener_WithValidSession_ShouldRemoveConnection() {
        // Given - First connect the user to have a session to disconnect
        when(connectEvent.getMessage()).thenReturn(message);
        when(connectEvent.getUser()).thenReturn(principal);
        when(principal.getName()).thenReturn(username);
        when(headerAccessor.getSessionId()).thenReturn(sessionId);
        
        // Mock disconnect event
        when(disconnectEvent.getMessage()).thenReturn(message);
        
        try (MockedStatic<StompHeaderAccessor> mockedStatic = mockStatic(StompHeaderAccessor.class)) {
            mockedStatic.when(() -> StompHeaderAccessor.wrap(message)).thenReturn(headerAccessor);
            
            // First connect the user
            webSocketEventHandler.handleWebSocketConnectListener(connectEvent);
            
            // Reset the mock to clear previous interactions
            reset(messagingTemplate);
            
            // When - Disconnect the user
            webSocketEventHandler.handleWebSocketDisconnectListener(disconnectEvent);
            
            // Then - Should broadcast offline status
            verify(messagingTemplate).convertAndSend(
                eq("/topic/presence"), 
                any(WebSocketMessage.class)
            );
        }
    }
}