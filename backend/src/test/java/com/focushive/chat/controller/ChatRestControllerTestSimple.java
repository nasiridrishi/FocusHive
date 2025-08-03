package com.focushive.chat.controller;

import com.focushive.api.client.IdentityServiceClient;
import com.focushive.api.security.IdentityServiceAuthenticationFilter;
import com.focushive.chat.dto.ChatMessageDto;
import com.focushive.chat.dto.MessageHistoryResponse;
import com.focushive.chat.entity.ChatMessage;
import com.focushive.chat.service.ChatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
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

@WebMvcTest(controllers = ChatRestController.class, excludeAutoConfiguration = {
        SecurityAutoConfiguration.class,
        org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration.class,
        org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration.class,
        org.springframework.cloud.openfeign.FeignAutoConfiguration.class,
        org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration.class,
        org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration.class,
        org.springframework.boot.autoconfigure.data.web.SpringDataWebAutoConfiguration.class
})
class ChatRestControllerTestSimple {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ChatService chatService;
    
    // Mock the problematic beans to prevent autowiring issues
    @MockBean
    private IdentityServiceClient identityServiceClient;
    
    @MockBean
    private IdentityServiceAuthenticationFilter identityServiceAuthenticationFilter;

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
}