package com.focushive.notification.service;

import com.focushive.notification.dto.EmailRequest;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.TemplateEngine;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for AsyncEmailService.
 * Tests asynchronous email sending functionality.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AsyncEmailService Tests")
class AsyncEmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private TemplateEngine templateEngine;

    @Mock
    private EmailMetricsService metricsService;

    @InjectMocks
    private AsyncEmailService asyncEmailService;

    private EmailRequest emailRequest;

    @BeforeEach
    void setUp() {
        emailRequest = EmailRequest.builder()
                .to("user@example.com")
                .subject("Test Subject")
                .htmlContent("<h1>Test Body</h1>")
                .build();
    }

    @Test
    @DisplayName("Should send email asynchronously and return tracking ID")
    void shouldSendEmailAsynchronously() throws Exception {
        // Given - mock mail sender
        MimeMessage mimeMessage = mock(MimeMessage.class);
        given(mailSender.createMimeMessage()).willReturn(mimeMessage);

        // When
        CompletableFuture<String> future = asyncEmailService.sendEmailAsync(emailRequest);
        String trackingId = future.get(); // Wait for async completion

        // Then
        assertThat(trackingId).isNotNull();
        assertThat(trackingId).isNotEmpty();

        // Verify email was sent
        verify(mailSender).send(any(MimeMessage.class));

        // Verify metrics were recorded
        verify(metricsService).recordEmailSent(any(Long.class));
    }

    @Test
    @DisplayName("Should handle email sending failure")
    void shouldHandleEmailSendingFailure() {
        // Given - mail sender fails
        MimeMessage mimeMessage = mock(MimeMessage.class);
        given(mailSender.createMimeMessage()).willReturn(mimeMessage);
        // Use doThrow for void method
        org.mockito.Mockito.doThrow(new RuntimeException("SMTP error"))
                .when(mailSender).send(any(MimeMessage.class));

        // When/Then - should throw EmailDeliveryException
        // Note: In unit test, @Async doesn't work, so method runs synchronously
        assertThrows(AsyncEmailService.EmailDeliveryException.class, () -> {
            asyncEmailService.sendEmailAsync(emailRequest);
        });

        // Verify metrics recorded the failure
        verify(metricsService).recordEmailFailed(any(String.class));
    }

    @Test
    @DisplayName("Should process email with HTML template")
    void shouldProcessEmailWithHtmlTemplate() throws Exception {
        // Given - email request with HTML content
        EmailRequest htmlEmailRequest = EmailRequest.builder()
                .to("user@example.com")
                .subject("Welcome to FocusHive")
                .templateName("welcome")
                .variables(java.util.Map.of(
                    "userName", "John Doe",
                    "platformName", "FocusHive"
                ))
                .build();

        MimeMessage mimeMessage = mock(MimeMessage.class);
        given(mailSender.createMimeMessage()).willReturn(mimeMessage);
        given(templateEngine.process(any(String.class), any())).willReturn("<html><body>Welcome John Doe to FocusHive!</body></html>");

        // When
        CompletableFuture<String> future = asyncEmailService.sendEmailAsync(htmlEmailRequest);
        String trackingId = future.get();

        // Then
        assertThat(trackingId).isNotNull();
        assertThat(trackingId).matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"); // UUID format

        // Verify template was processed
        verify(templateEngine).process(eq("welcome"), any());
        verify(mailSender).send(any(MimeMessage.class));
        verify(metricsService).recordEmailSent(any(Long.class));
    }
}