package com.focushive.notification.service;

import com.focushive.notification.messaging.dto.NotificationMessage;
import com.focushive.notification.entity.NotificationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import jakarta.mail.internet.MimeMessage;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for EmailNotificationService.
 * Tests email notification processing and template rendering.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EmailNotificationService Tests")
class EmailNotificationServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailNotificationService emailNotificationService;

    private NotificationMessage notificationMessage;

    @BeforeEach
    void setUp() {
        // Note: EmailNotificationService constructor needs fromEmailAddress
        // We'll need to create it manually or use reflection to set the field
        emailNotificationService = new EmailNotificationService(mailSender, "noreply@focushive.com");

        notificationMessage = NotificationMessage.builder()
                .notificationId("test-notification-id")
                .userId("user123")
                .type(NotificationType.WELCOME)
                .emailTo("user@example.com")
                .emailSubject("Test Subject")
                .message("Test email content")
                .build();
    }

    @Test
    @DisplayName("Should send simple email successfully")
    void shouldSendSimpleEmail() {
        // Given - no mocking needed, service creates SimpleMailMessage internally

        // When
        String trackingId = emailNotificationService.sendEmail(notificationMessage);

        // Then
        assertThat(trackingId).isNotNull();
        assertThat(trackingId).startsWith("smtp-"); // Format: smtp-timestamp-random
        assertThat(trackingId).matches("smtp-\\d+-.+"); // Match the actual format
        verify(mailSender).send(any(SimpleMailMessage.class)); // Verify with SimpleMailMessage for plain text
    }
}