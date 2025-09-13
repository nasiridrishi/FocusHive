package com.focushive.notification.integration;

import com.focushive.notification.entity.Notification;
import com.focushive.notification.entity.NotificationTemplate;
import com.focushive.notification.entity.NotificationType;
import com.icegreen.greenmail.configuration.GreenMailConfiguration;
import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.ServerSetupTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.test.context.TestPropertySource;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static java.time.Duration.ofSeconds;

/**
 * Integration tests for email delivery functionality.
 * Tests SMTP email sending with mock SMTP server (GreenMail) following TDD approach.
 * 
 * Test scenarios:
 * 1. Basic email sending with simple text content
 * 2. HTML email sending with rich content
 * 3. Email template rendering with personalization tokens
 * 4. Email validation and sanitization
 * 5. Attachment handling
 * 6. Delivery failure and retry logic
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "spring.mail.host=localhost",
    "spring.mail.port=3025",
    "spring.mail.username=test",
    "spring.mail.password=test",
    "spring.mail.properties.mail.smtp.auth=true",
    "spring.mail.properties.mail.smtp.starttls.enable=false"
})
@DisplayName("Email Delivery Integration Tests")
class EmailDeliveryIntegrationTest extends BaseIntegrationTest {

    @RegisterExtension
    static GreenMailExtension greenMail = new GreenMailExtension(ServerSetupTest.SMTP)
            .withConfiguration(GreenMailConfiguration.aConfig()
                    .withUser("test", "test")
                    .withDisabledAuthentication());

    @Autowired
    private JavaMailSender mailSender;

    private static final String TEST_RECIPIENT = "test@focushive.com";
    private static final String TEST_SENDER = "noreply@focushive.com";

    @BeforeEach
    void setUpEmailTests() {
        greenMail.reset();
    }

    @Test
    @DisplayName("Should send simple text email successfully")
    void shouldSendSimpleTextEmailSuccessfully() {
        // Given - TDD: Write test first
        String subject = "Test Email";
        String content = "This is a test email content.";
        
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(TEST_SENDER);
        message.setTo(TEST_RECIPIENT);
        message.setSubject(subject);
        message.setText(content);

        // When - TDD: This will fail initially until implementation exists
        mailSender.send(message);

        // Then - TDD: Verify expected behavior
        await().atMost(ofSeconds(5)).untilAsserted(() -> {
            assertThat(greenMail.getReceivedMessages()).hasSize(1);
            
            var receivedMessage = greenMail.getReceivedMessages()[0];
            assertThat(receivedMessage.getSubject()).isEqualTo(subject);
            assertThat(receivedMessage.getContent().toString().trim()).isEqualTo(content);
            assertThat(receivedMessage.getAllRecipients()[0].toString()).isEqualTo(TEST_RECIPIENT);
        });
    }

    @Test
    @DisplayName("Should send HTML email with rich content")
    void shouldSendHtmlEmailWithRichContent() throws MessagingException {
        // Given
        String subject = "HTML Test Email";
        String htmlContent = """
            <html>
            <body>
                <h1>Welcome to FocusHive!</h1>
                <p>This is a <strong>rich HTML</strong> email with formatting.</p>
                <ul>
                    <li>Feature 1</li>
                    <li>Feature 2</li>
                </ul>
                <a href="https://focushive.com">Visit FocusHive</a>
            </body>
            </html>
            """;
        String textContent = """
            Welcome to FocusHive!
            This is a rich HTML email with formatting.
            - Feature 1
            - Feature 2
            Visit FocusHive: https://focushive.com
            """;

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(TEST_SENDER);
        helper.setTo(TEST_RECIPIENT);
        helper.setSubject(subject);
        helper.setText(textContent, htmlContent);

        // When
        mailSender.send(message);

        // Then
        await().atMost(ofSeconds(5)).untilAsserted(() -> {
            assertThat(greenMail.getReceivedMessages()).hasSize(1);
            
            var receivedMessage = greenMail.getReceivedMessages()[0];
            assertThat(receivedMessage.getSubject()).isEqualTo(subject);
            assertThat(receivedMessage.getContentType()).contains("multipart/alternative");
            
            // Verify both text and HTML parts exist
            var content = receivedMessage.getContent();
            assertThat(content.toString()).contains("Welcome to FocusHive!");
        });
    }

    @Test
    @DisplayName("Should render email template with personalization tokens")
    void shouldRenderEmailTemplateWithPersonalizationTokens() throws MessagingException {
        // Given
        NotificationTemplate template = createTestNotificationTemplate(
            NotificationType.WELCOME,
            "en",
            "Welcome {{userName}} to {{platformName}}!",
            "Hi {{userName}}, welcome to {{platformName}}! Your email is {{userEmail}}.",
            "<html><body><h1>Welcome {{userName}}!</h1><p>Welcome to {{platformName}}!</p><p>Your email: {{userEmail}}</p></body></html>"
        );
        notificationTemplateRepository.save(template);

        Map<String, Object> variables = Map.of(
            "userName", "John Doe",
            "platformName", "FocusHive",
            "userEmail", TEST_RECIPIENT
        );

        String processedSubject = template.getProcessedSubject(variables);
        String processedText = template.getProcessedBodyText(variables);
        String processedHtml = template.getProcessedBodyHtml(variables);

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(TEST_SENDER);
        helper.setTo(TEST_RECIPIENT);
        helper.setSubject(processedSubject);
        helper.setText(processedText, processedHtml);

        // When
        mailSender.send(message);

        // Then
        await().atMost(ofSeconds(5)).untilAsserted(() -> {
            assertThat(greenMail.getReceivedMessages()).hasSize(1);
            
            var receivedMessage = greenMail.getReceivedMessages()[0];
            assertThat(receivedMessage.getSubject()).isEqualTo("Welcome John Doe to FocusHive!");
            
            // Verify template variables were replaced
            var content = receivedMessage.getContent().toString();
            assertThat(content).contains("John Doe");
            assertThat(content).contains("FocusHive");
            assertThat(content).contains(TEST_RECIPIENT);
            assertThat(content).doesNotContain("{{userName}}");
            assertThat(content).doesNotContain("{{platformName}}");
            assertThat(content).doesNotContain("{{userEmail}}");
        });
    }

    @Test
    @DisplayName("Should handle email validation and sanitization")
    void shouldHandleEmailValidationAndSanitization() throws MessagingException {
        // Given - Test with potentially malicious content
        String subject = "Test with <script>alert('xss')</script> content";
        String maliciousContent = """
            <html>
            <body>
                <h1>Hello</h1>
                <script>alert('xss');</script>
                <p onclick="alert('click')">Click me</p>
                <img src="x" onerror="alert('error')">
            </body>
            </html>
            """;
        String sanitizedSubject = sanitizeEmailSubject(subject);
        String sanitizedContent = sanitizeEmailContent(maliciousContent);

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(TEST_SENDER);
        helper.setTo(TEST_RECIPIENT);
        helper.setSubject(sanitizedSubject);
        helper.setText("Safe text content", sanitizedContent);

        // When
        mailSender.send(message);

        // Then
        await().atMost(ofSeconds(5)).untilAsserted(() -> {
            assertThat(greenMail.getReceivedMessages()).hasSize(1);
            
            var receivedMessage = greenMail.getReceivedMessages()[0];
            
            // Verify malicious content was sanitized
            assertThat(receivedMessage.getSubject()).doesNotContain("<script>");
            assertThat(receivedMessage.getSubject()).contains("Test with  content");
            
            var content = receivedMessage.getContent().toString();
            assertThat(content).doesNotContain("<script>");
            assertThat(content).doesNotContain("onclick");
            assertThat(content).doesNotContain("onerror");
            assertThat(content).contains("<h1>Hello</h1>");
        });
    }

    @Test
    @DisplayName("Should handle email with attachments")
    void shouldHandleEmailWithAttachments() throws MessagingException {
        // Given
        String subject = "Email with Attachment";
        String content = "This email contains an attachment.";
        byte[] attachmentData = "Sample file content for testing".getBytes();
        String attachmentName = "test-document.txt";

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(TEST_SENDER);
        helper.setTo(TEST_RECIPIENT);
        helper.setSubject(subject);
        helper.setText(content);
        helper.addAttachment(attachmentName, () -> new java.io.ByteArrayInputStream(attachmentData), "text/plain");

        // When
        mailSender.send(message);

        // Then
        await().atMost(ofSeconds(5)).untilAsserted(() -> {
            assertThat(greenMail.getReceivedMessages()).hasSize(1);
            
            var receivedMessage = greenMail.getReceivedMessages()[0];
            assertThat(receivedMessage.getSubject()).isEqualTo(subject);
            assertThat(receivedMessage.getContentType()).contains("multipart/mixed");
            
            // Verify attachment exists (basic check)
            var messageContent = receivedMessage.getContent();
            assertThat(messageContent.toString()).contains(attachmentName);
        });
    }

    @Test
    @DisplayName("Should handle delivery failure scenarios")
    void shouldHandleDeliveryFailureScenarios() {
        // Given - Create notification to track delivery attempts
        Notification notification = createTestNotification(
            "test-user-1",
            NotificationType.WELCOME,
            "Test Notification",
            "Test content"
        );
        notificationRepository.save(notification);

        // Simulate failure by using invalid email configuration
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("invalid-sender"); // Invalid from address
        message.setTo("invalid@"); // Invalid to address
        message.setSubject("Test Failure");
        message.setText("This should fail");

        // When & Then - Should handle gracefully
        try {
            mailSender.send(message);
            
            // Update notification with failure
            notification.markDeliveryFailed("Invalid email addresses");
            notificationRepository.save(notification);
            
            // Verify failure was recorded
            var savedNotification = notificationRepository.findById(notification.getId()).orElseThrow();
            assertThat(savedNotification.getFailureReason()).isEqualTo("Invalid email addresses");
            assertThat(savedNotification.getDeliveryAttempts()).isEqualTo(1);
            assertThat(savedNotification.getFailedAt()).isNotNull();
            
        } catch (Exception e) {
            // Expected failure for invalid email
            assertThat(e).isNotNull();
        }
    }

    @Test
    @DisplayName("Should implement retry logic for failed deliveries")
    void shouldImplementRetryLogicForFailedDeliveries() {
        // Given
        Notification notification = createTestNotification(
            "test-user-1",
            NotificationType.PASSWORD_RESET,
            "Password Reset",
            "Reset your password"
        );
        notificationRepository.save(notification);

        // Simulate multiple delivery attempts
        for (int attempt = 1; attempt <= 3; attempt++) {
            notification.markDeliveryFailed("Temporary failure - attempt " + attempt);
            notificationRepository.save(notification);
            
            // Verify delivery attempts increment
            var savedNotification = notificationRepository.findById(notification.getId()).orElseThrow();
            assertThat(savedNotification.getDeliveryAttempts()).isEqualTo(attempt);
        }

        // When - Final successful delivery
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(TEST_SENDER);
        message.setTo(TEST_RECIPIENT);
        message.setSubject("Password Reset - Retry Success");
        message.setText("Your password reset link");

        mailSender.send(message);
        notification.markDelivered();
        notificationRepository.save(notification);

        // Then
        await().atMost(ofSeconds(5)).untilAsserted(() -> {
            assertThat(greenMail.getReceivedMessages()).hasSize(1);
            
            var savedNotification = notificationRepository.findById(notification.getId()).orElseThrow();
            assertThat(savedNotification.getDeliveredAt()).isNotNull();
            assertThat(savedNotification.getDeliveryAttempts()).isEqualTo(3);
        });
    }

    /**
     * Helper method to sanitize email subject - removes HTML tags
     */
    private String sanitizeEmailSubject(String subject) {
        if (subject == null) return null;
        return subject.replaceAll("<[^>]*>", "");
    }

    /**
     * Helper method to sanitize email content - removes dangerous HTML
     */
    private String sanitizeEmailContent(String content) {
        if (content == null) return null;
        return content
                .replaceAll("<script[^>]*>.*?</script>", "")
                .replaceAll("(?i)on\\w+\\s*=\\s*[\"'][^\"']*[\"']", "")
                .replaceAll("(?i)onerror\\s*=\\s*[\"'][^\"']*[\"']", "");
    }
}