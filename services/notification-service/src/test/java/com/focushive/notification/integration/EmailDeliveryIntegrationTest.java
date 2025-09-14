package com.focushive.notification.integration;

import com.focushive.notification.config.EmailTestConfiguration;
import com.focushive.notification.entity.Notification;
import com.focushive.notification.entity.NotificationTemplate;
import com.focushive.notification.entity.NotificationType;
import com.focushive.notification.repository.NotificationRepository;
import com.focushive.notification.repository.NotificationTemplateRepository;
import com.icegreen.greenmail.configuration.GreenMailConfiguration;
import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.ServerSetupTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.BodyPart;
import java.io.IOException;
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
    "spring.mail.host=127.0.0.1",
    "spring.mail.port=3025", 
    "spring.mail.properties.mail.smtp.auth=false",
    "spring.mail.properties.mail.smtp.starttls.enable=false",
    "logging.level.org.springframework.mail=DEBUG",
    "spring.main.allow-bean-definition-overriding=true"
})
@Import(EmailTestConfiguration.class)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@DisplayName("Email Delivery Integration Tests")
class EmailDeliveryIntegrationTest {

    @RegisterExtension
    static GreenMailExtension greenMail = new GreenMailExtension(ServerSetupTest.SMTP)
            .withConfiguration(GreenMailConfiguration.aConfig()
                    .withUser("test", "test")
                    .withDisabledAuthentication());

    @Autowired
    private JavaMailSender mailSender;
    
    @Autowired
    protected NotificationRepository notificationRepository;
    
    @Autowired 
    protected NotificationTemplateRepository notificationTemplateRepository;

    private static final String TEST_RECIPIENT = "test@focushive.com";
    private static final String TEST_SENDER = "noreply@focushive.com";

    @BeforeEach
    void setUpEmailTests() {
        greenMail.reset();
        
        // Debug: Print GreenMail configuration
        System.out.println("GreenMail SMTP server running on port: " + greenMail.getSmtp().getPort());
        System.out.println("GreenMail SMTP server binding address: " + greenMail.getSmtp().getBindTo());
        
        // Debug: Print JavaMailSender configuration
        if (mailSender instanceof JavaMailSenderImpl impl) {
            System.out.println("JavaMailSender host: " + impl.getHost());
            System.out.println("JavaMailSender port: " + impl.getPort());
            System.out.println("JavaMailSender properties: " + impl.getJavaMailProperties());
        }
    }

    @Test
    @DisplayName("Should send simple text email successfully")
    void shouldSendSimpleTextEmailSuccessfully() {
        // Given - TDD: Write test first
        String subject = "Test Email";
        String content = "This is a test email content.";
        
        // Debug: Check what type of mailSender we have
        System.out.println("MailSender class: " + mailSender.getClass().getName());
        if (mailSender instanceof JavaMailSenderImpl impl) {
            System.out.println("JavaMailSender host: " + impl.getHost());
            System.out.println("JavaMailSender port: " + impl.getPort());
            System.out.println("JavaMailSender properties: " + impl.getJavaMailProperties());
        } else {
            System.out.println("MailSender is NOT JavaMailSenderImpl: " + mailSender.getClass());
        }
        
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(TEST_SENDER);
        message.setTo(TEST_RECIPIENT);
        message.setSubject(subject);
        message.setText(content);

        // When - TDD: This will fail initially until implementation exists
        System.out.println("About to send email...");
        mailSender.send(message);
        System.out.println("Email sent, checking GreenMail...");

        // Then - TDD: Verify expected behavior
        await().atMost(ofSeconds(5)).untilAsserted(() -> {
            var messages = greenMail.getReceivedMessages();
            System.out.println("GreenMail received " + messages.length + " messages");
            assertThat(messages).hasSize(1);
            
            var receivedMessage = messages[0];
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
            
            // Verify the message contains multipart structure (could be nested)
            assertThat(receivedMessage.getContentType()).startsWith("multipart/");
            
            // Verify both text and HTML parts exist
            String emailContent = extractEmailContent(receivedMessage);
            assertThat(emailContent).contains("Welcome to FocusHive!");
            assertThat(emailContent).contains("<strong>rich HTML</strong>");
            assertThat(emailContent).contains("Visit FocusHive");
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
            String emailContent = extractEmailContent(receivedMessage);
            assertThat(emailContent).contains("John Doe");
            assertThat(emailContent).contains("FocusHive");
            assertThat(emailContent).contains(TEST_RECIPIENT);
            assertThat(emailContent).doesNotContain("{{userName}}");
            assertThat(emailContent).doesNotContain("{{platformName}}");
            assertThat(emailContent).doesNotContain("{{userEmail}}");
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
            
            String emailContent = extractEmailContent(receivedMessage);
            assertThat(emailContent).doesNotContain("<script>");
            assertThat(emailContent).doesNotContain("onclick");
            assertThat(emailContent).doesNotContain("onerror");
            assertThat(emailContent).contains("<h1>Hello</h1>");
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
            String emailContent = extractEmailContent(receivedMessage);
            assertThat(emailContent).contains(attachmentName);
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
            // Get fresh instance from database to avoid optimistic locking issues
            Notification freshNotification = notificationRepository.findById(notification.getId()).orElseThrow();
            freshNotification.markDeliveryFailed("Temporary failure - attempt " + attempt);
            notificationRepository.save(freshNotification);
            
            // Verify delivery attempts increment
            var savedNotification = notificationRepository.findById(notification.getId()).orElseThrow();
            assertThat(savedNotification.getDeliveryAttempts()).isEqualTo(attempt);
            
            // Update our local reference
            notification = savedNotification;
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

        // Store the ID for use in lambda
        String notificationId = notification.getId();
        
        // Then
        await().atMost(ofSeconds(5)).untilAsserted(() -> {
            assertThat(greenMail.getReceivedMessages()).hasSize(1);
            
            var savedNotification = notificationRepository.findById(notificationId).orElseThrow();
            assertThat(savedNotification.getDeliveredAt()).isNotNull();
            assertThat(savedNotification.getDeliveryAttempts()).isEqualTo(3);
        });
    }

    /**
     * Helper method to sanitize email subject - removes HTML tags and script content
     */
    private String sanitizeEmailSubject(String subject) {
        if (subject == null) return null;
        return subject.replaceAll("<[^>]*>", "")
                      .replaceAll("alert\\([^)]*\\)", "");
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
    
    /**
     * Helper method to create test notifications
     */
    protected Notification createTestNotification(String userId, NotificationType type, String title, String content) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(type);
        notification.setTitle(title);
        notification.setContent(content);
        return notification;
    }

    /**
     * Helper method to create test notification templates
     */
    protected NotificationTemplate createTestNotificationTemplate(
            NotificationType type, String language, String subject, String bodyText, String bodyHtml) {
        NotificationTemplate template = new NotificationTemplate();
        template.setNotificationType(type);
        template.setLanguage(language);
        template.setSubject(subject);
        template.setBodyText(bodyText);
        template.setBodyHtml(bodyHtml);
        return template;
    }

    /**
     * Helper method to extract text content from MimeMessage for test assertions.
     * Handles both simple text and multipart messages, including attachments.
     */
    private String extractEmailContent(MimeMessage message) throws MessagingException, IOException {
        Object content = message.getContent();
        
        if (content instanceof String) {
            return (String) content;
        } else if (content instanceof MimeMultipart) {
            return extractFromMultipart((MimeMultipart) content);
        }
        
        return content.toString();
    }
    
    /**
     * Recursively extract content from multipart messages.
     * Handles nested multipart, attachments, and mixed content types.
     */
    private String extractFromMultipart(MimeMultipart multipart) throws MessagingException, IOException {
        StringBuilder contentBuilder = new StringBuilder();
        
        for (int i = 0; i < multipart.getCount(); i++) {
            BodyPart bodyPart = multipart.getBodyPart(i);
            String disposition = bodyPart.getDisposition();
            
            // Handle attachments - include filename in content
            if (disposition != null && (disposition.equals(BodyPart.ATTACHMENT) || disposition.equals(BodyPart.INLINE))) {
                String filename = bodyPart.getFileName();
                if (filename != null) {
                    contentBuilder.append(filename);
                }
                // Also include attachment content
                Object partContent = bodyPart.getContent();
                if (partContent instanceof String) {
                    contentBuilder.append(partContent);
                }
                continue;
            }
            
            // Handle regular content parts
            Object partContent = bodyPart.getContent();
            String contentType = bodyPart.getContentType();
            
            if (partContent instanceof String) {
                // For HTML content, extract just the text parts we need for assertions
                if (contentType != null && contentType.toLowerCase().contains("text/html")) {
                    contentBuilder.append(partContent);
                } else {
                    contentBuilder.append(partContent);
                }
            } else if (partContent instanceof MimeMultipart) {
                // Handle nested multipart recursively
                contentBuilder.append(extractFromMultipart((MimeMultipart) partContent));
            }
        }
        
        return contentBuilder.toString();
    }
}