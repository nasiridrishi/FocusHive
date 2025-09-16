package com.focushive.notification.contract;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.focushive.notification.dto.EmailRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

/**
 * Contract tests for Notification Service API endpoints.
 * Ensures API contracts are maintained and documented.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Tag("contract")
@DisplayName("Notification API Contract Tests")
class NotificationApiContractTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String validAuthToken;

    @BeforeEach
    void setUp() {
        // In a real test, this would be a valid JWT token
        validAuthToken = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test.signature";
    }

    @Test
    @DisplayName("POST /api/notifications/send should accept valid email request")
    void sendNotificationContractTest() throws Exception {
        EmailRequest request = EmailRequest.builder()
                .to("user@example.com")
                .subject("Test Notification")
                .htmlContent("<p>This is a test notification</p>")
                .build();

        String requestJson = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/api/notifications/send")
                .header("Authorization", validAuthToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.trackingId").exists())
                .andExpect(jsonPath("$.trackingId").isString())
                .andExpect(jsonPath("$.status").value("QUEUED"));
    }
}