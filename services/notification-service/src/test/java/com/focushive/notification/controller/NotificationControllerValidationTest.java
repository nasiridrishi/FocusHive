package com.focushive.notification.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.focushive.notification.config.ControllerTestConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import com.focushive.notification.dto.CreateNotificationRequest;
import com.focushive.notification.entity.NotificationType;
import com.focushive.notification.service.NotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
    controllers = NotificationController.class,
    excludeAutoConfiguration = {
        org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration.class,
        org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration.class,
        org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration.class,
        org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration.class,
        org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration.class
    }
)
@Import(ControllerTestConfiguration.class)
@ActiveProfiles("test")
@DisplayName("NotificationController Validation Integration Tests")
class NotificationControllerValidationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private NotificationService notificationService;

    @Test
    @DisplayName("Should return 400 when userId is null")
    void shouldReturn400WhenUserIdIsNull() throws Exception {
        CreateNotificationRequest request = CreateNotificationRequest.builder()
                .userId(null)
                .type(NotificationType.SYSTEM_NOTIFICATION)
                .title("Test title")
                .build();

        mockMvc.perform(post("/api/v1/notifications")
                .with(jwt().jwt(jwt -> jwt.claim("sub", "testuser")))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    @DisplayName("Should return 400 when userId contains invalid characters")
    void shouldReturn400WhenUserIdContainsInvalidCharacters() throws Exception {
        CreateNotificationRequest request = CreateNotificationRequest.builder()
                .userId("user@invalid")
                .type(NotificationType.SYSTEM_NOTIFICATION)
                .title("Test title")
                .build();

        mockMvc.perform(post("/api/v1/notifications")
                .with(jwt().jwt(jwt -> jwt.claim("sub", "testuser")))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.userId").value(containsString("invalid characters")));
    }

    @Test
    @DisplayName("Should return 400 when title is null")
    void shouldReturn400WhenTitleIsNull() throws Exception {
        CreateNotificationRequest request = CreateNotificationRequest.builder()
                .userId("user123")
                .type(NotificationType.SYSTEM_NOTIFICATION)
                .title(null)
                .build();

        mockMvc.perform(post("/api/v1/notifications")
                .with(jwt().jwt(jwt -> jwt.claim("sub", "testuser")))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.title").value(containsString("Title is required")));
    }

    @Test
    @DisplayName("Should return 400 when title exceeds 200 characters")
    void shouldReturn400WhenTitleTooLong() throws Exception {
        String longTitle = "a".repeat(201);
        CreateNotificationRequest request = CreateNotificationRequest.builder()
                .userId("user123")
                .type(NotificationType.SYSTEM_NOTIFICATION)
                .title(longTitle)
                .build();

        mockMvc.perform(post("/api/v1/notifications")
                .with(jwt().jwt(jwt -> jwt.claim("sub", "testuser")))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.title").value(containsString("200 characters")));
    }

    @Test
    @DisplayName("Should return 400 when content exceeds 5000 characters")
    void shouldReturn400WhenContentTooLong() throws Exception {
        String longContent = "a".repeat(5001);
        CreateNotificationRequest request = CreateNotificationRequest.builder()
                .userId("user123")
                .type(NotificationType.SYSTEM_NOTIFICATION)
                .title("Test title")
                .content(longContent)
                .build();

        mockMvc.perform(post("/api/v1/notifications")
                .with(jwt().jwt(jwt -> jwt.claim("sub", "testuser")))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.content").value(containsString("5000 characters")));
    }

    @Test
    @DisplayName("Should return 400 when title contains XSS content")
    void shouldReturn400WhenTitleContainsXSS() throws Exception {
        CreateNotificationRequest request = CreateNotificationRequest.builder()
                .userId("user123")
                .type(NotificationType.SYSTEM_NOTIFICATION)
                .title("<script>alert('xss')</script>")
                .build();

        mockMvc.perform(post("/api/v1/notifications")
                .with(jwt().jwt(jwt -> jwt.claim("sub", "testuser")))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.title").value(containsString("dangerous content")));
    }

    @Test
    @DisplayName("Should return 400 when content contains dangerous JavaScript")
    void shouldReturn400WhenContentContainsDangerousJavaScript() throws Exception {
        CreateNotificationRequest request = CreateNotificationRequest.builder()
                .userId("user123")
                .type(NotificationType.SYSTEM_NOTIFICATION)
                .title("Test title")
                .content("Click here: <a href=\"javascript:alert('xss')\">link</a>")
                .build();

        mockMvc.perform(post("/api/v1/notifications")
                .with(jwt().jwt(jwt -> jwt.claim("sub", "testuser")))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.content").value(containsString("dangerous content")));
    }

    @Test
    @DisplayName("Should return 400 when notification type is null")
    void shouldReturn400WhenTypeIsNull() throws Exception {
        CreateNotificationRequest request = CreateNotificationRequest.builder()
                .userId("user123")
                .type(null)
                .title("Test title")
                .build();

        mockMvc.perform(post("/api/v1/notifications")
                .with(jwt().jwt(jwt -> jwt.claim("sub", "testuser")))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.type").value(containsString("Notification type is required")));
    }

    @Test
    @DisplayName("Should return 400 with validation errors for empty JSON")
    void shouldReturn400WithValidationErrorsForEmptyJson() throws Exception {
        mockMvc.perform(post("/api/v1/notifications")
                .with(jwt().jwt(jwt -> jwt.claim("sub", "testuser")))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("Should return 400 for malformed JSON")
    void shouldReturn400ForMalformedJson() throws Exception {
        mockMvc.perform(post("/api/v1/notifications")
                .with(jwt().jwt(jwt -> jwt.claim("sub", "testuser")))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{invalid json}"))
                .andExpect(status().isBadRequest());
    }
}