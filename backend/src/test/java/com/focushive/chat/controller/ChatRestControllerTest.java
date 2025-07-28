package com.focushive.chat.controller;

import com.focushive.chat.dto.ChatMessageDto;
import com.focushive.chat.dto.MessageHistoryResponse;
import com.focushive.chat.entity.ChatMessage;
import com.focushive.chat.service.ChatService;
import com.focushive.test.config.TestSecurityConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ChatRestController.class)
@Import(TestSecurityConfig.class)
class ChatRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ChatService chatService;

    private String hiveId;
    private String userId;
    private ChatMessageDto testMessage;

    @BeforeEach
    void setUp() {
        hiveId = UUID.randomUUID().toString();
        userId = UUID.randomUUID().toString();
        
        testMessage = ChatMessageDto.builder()
                .id(UUID.randomUUID().toString())
                .hiveId(hiveId)
                .senderId(userId)
                .senderUsername("testuser")
                .content("Test message")
                .messageType(ChatMessage.MessageType.TEXT)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @WithMockUser(username = "testuser")
    void getMessageHistory_Success() throws Exception {
        // Given
        MessageHistoryResponse response = MessageHistoryResponse.builder()
                .messages(Arrays.asList(testMessage))
                .page(0)
                .size(50)
                .totalElements(1L)
                .totalPages(1)
                .hasNext(false)
                .hasPrevious(false)
                .build();
        
        when(chatService.getMessageHistory(eq(hiveId), eq("testuser"), any(Pageable.class)))
                .thenReturn(response);

        // When & Then
        mockMvc.perform(get("/api/v1/chat/hives/{hiveId}/messages", hiveId)
                        .param("page", "0")
                        .param("size", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messages[0].content").value("Test message"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.page").value(0));
    }

    @Test
    @WithMockUser(username = "testuser")
    void getRecentMessages_Success() throws Exception {
        // Given
        List<ChatMessageDto> messages = Arrays.asList(testMessage);
        when(chatService.getRecentMessages(hiveId, "testuser", 50))
                .thenReturn(messages);

        // When & Then
        mockMvc.perform(get("/api/v1/chat/hives/{hiveId}/messages/recent", hiveId)
                        .param("limit", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].content").value("Test message"))
                .andExpect(jsonPath("$[0].senderUsername").value("testuser"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void getMessagesAfter_Success() throws Exception {
        // Given
        LocalDateTime after = LocalDateTime.now().minusHours(1);
        List<ChatMessageDto> messages = Arrays.asList(testMessage);
        
        when(chatService.getMessagesAfter(eq(hiveId), eq("testuser"), any(LocalDateTime.class)))
                .thenReturn(messages);

        // When & Then
        mockMvc.perform(get("/api/v1/chat/hives/{hiveId}/messages/after", hiveId)
                        .param("after", after.format(DateTimeFormatter.ISO_DATE_TIME)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].content").value("Test message"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void sendAnnouncement_Success() throws Exception {
        // Given
        String content = "System announcement";
        ChatMessageDto systemMessage = ChatMessageDto.builder()
                .id(UUID.randomUUID().toString())
                .hiveId(hiveId)
                .senderId("system")
                .senderUsername("System")
                .content(content)
                .messageType(ChatMessage.MessageType.SYSTEM)
                .createdAt(LocalDateTime.now())
                .build();
        
        when(chatService.sendSystemMessage(hiveId, content))
                .thenReturn(systemMessage);

        // When & Then
        mockMvc.perform(post("/api/v1/chat/hives/{hiveId}/announce", hiveId)
                        .contentType(MediaType.TEXT_PLAIN)
                        .content(content))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.senderUsername").value("System"))
                .andExpect(jsonPath("$.messageType").value("SYSTEM"));
    }

    @Test
    void getMessageHistory_Unauthenticated_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/chat/hives/{hiveId}/messages", hiveId))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "testuser")
    void getMessageHistory_InvalidPageParams_UseDefaults() throws Exception {
        // Given
        MessageHistoryResponse response = MessageHistoryResponse.builder()
                .messages(Arrays.asList(testMessage))
                .page(0)
                .size(50)
                .totalElements(1L)
                .totalPages(1)
                .hasNext(false)
                .hasPrevious(false)
                .build();
        
        when(chatService.getMessageHistory(eq(hiveId), eq("testuser"), any(Pageable.class)))
                .thenReturn(response);

        // When & Then - should use default values
        mockMvc.perform(get("/api/v1/chat/hives/{hiveId}/messages", hiveId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(50));
    }
}