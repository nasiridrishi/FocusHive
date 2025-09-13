package com.focushive.notification.integration;

import com.focushive.notification.entity.Notification;
import com.focushive.notification.entity.NotificationPreference;
import com.focushive.notification.entity.NotificationTemplate;
import com.focushive.notification.entity.NotificationType;
import com.focushive.notification.entity.NotificationFrequency;
import com.focushive.notification.repository.NotificationRepository;
import com.focushive.notification.repository.NotificationPreferenceRepository;
import com.focushive.notification.repository.NotificationTemplateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.LocalTime;
import java.util.Map;

/**
 * Base integration test class providing common setup for all notification service integration tests.
 * Follows TDD approach with TestContainers for isolated testing environment.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Transactional
public abstract class BaseIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgresContainer = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("notification_test")
            .withUsername("test")
            .withPassword("test")
            .withReuse(true);

    @Container
    static GenericContainer<?> redisContainer = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379)
            .withReuse(true);

    @Autowired
    protected NotificationRepository notificationRepository;

    @Autowired
    protected NotificationPreferenceRepository notificationPreferenceRepository;

    @Autowired
    protected NotificationTemplateRepository notificationTemplateRepository;

    @DynamicPropertySource
    static void configureTestProperties(DynamicPropertyRegistry registry) {
        // Database configuration
        registry.add("spring.datasource.url", postgresContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgresContainer::getUsername);
        registry.add("spring.datasource.password", postgresContainer::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        
        // Redis configuration
        registry.add("spring.data.redis.host", redisContainer::getHost);
        registry.add("spring.data.redis.port", () -> redisContainer.getMappedPort(6379).toString());
        
        // Email configuration for testing
        registry.add("spring.mail.host", () -> "localhost");
        registry.add("spring.mail.port", () -> "3025"); // GreenMail default port
        registry.add("spring.mail.username", () -> "test");
        registry.add("spring.mail.password", () -> "test");
        registry.add("spring.mail.properties.mail.smtp.auth", () -> "true");
        registry.add("spring.mail.properties.mail.smtp.starttls.enable", () -> "false");
        
        // Disable security for tests
        registry.add("spring.security.enabled", () -> "false");
        
        // Test profile specific configurations
        registry.add("spring.profiles.active", () -> "test");
        registry.add("logging.level.com.focushive.notification", () -> "WARN");
        registry.add("logging.level.org.springframework.mail", () -> "DEBUG");
    }

    @BeforeEach
    void setUp() {
        cleanupTestData();
        setupTestData();
    }

    /**
     * Clean up test data before each test
     */
    protected void cleanupTestData() {
        notificationRepository.deleteAll();
        notificationPreferenceRepository.deleteAll();
        notificationTemplateRepository.deleteAll();
    }

    /**
     * Setup basic test data for all tests
     */
    protected void setupTestData() {
        createDefaultNotificationTemplates();
        createDefaultNotificationPreferences();
    }

    /**
     * Creates default notification templates for testing
     */
    protected void createDefaultNotificationTemplates() {
        // Welcome email template
        NotificationTemplate welcomeTemplate = NotificationTemplate.builder()
                .notificationType(NotificationType.WELCOME)
                .language("en")
                .subject("Welcome to FocusHive! {{userName}}")
                .bodyText("Hi {{userName}},\n\nWelcome to FocusHive! We're excited to have you join our community.\n\nBest regards,\nThe FocusHive Team")
                .bodyHtml("<html><body><h1>Welcome to FocusHive!</h1><p>Hi {{userName}},</p><p>Welcome to FocusHive! We're excited to have you join our community.</p><p>Best regards,<br>The FocusHive Team</p></body></html>")
                .build();
        notificationTemplateRepository.save(welcomeTemplate);

        // Password reset template
        NotificationTemplate passwordResetTemplate = NotificationTemplate.builder()
                .notificationType(NotificationType.PASSWORD_RESET)
                .language("en")
                .subject("Reset Your FocusHive Password")
                .bodyText("Hi {{userName}},\n\nYou requested a password reset for your FocusHive account.\n\nClick here to reset: {{resetUrl}}\n\nThis link expires in {{expirationTime}} minutes.\n\nIf you didn't request this, please ignore this email.")
                .bodyHtml("<html><body><h1>Reset Your Password</h1><p>Hi {{userName}},</p><p>You requested a password reset for your FocusHive account.</p><p><a href=\"{{resetUrl}}\">Click here to reset your password</a></p><p>This link expires in {{expirationTime}} minutes.</p><p>If you didn't request this, please ignore this email.</p></body></html>")
                .build();
        notificationTemplateRepository.save(passwordResetTemplate);

        // Hive invitation template
        NotificationTemplate hiveInvitationTemplate = NotificationTemplate.builder()
                .notificationType(NotificationType.HIVE_INVITATION)
                .language("en")
                .subject("You're invited to join {{hiveName}} on FocusHive!")
                .bodyText("Hi {{userName}},\n\n{{inviterName}} has invited you to join the hive \"{{hiveName}}\" on FocusHive.\n\nDescription: {{hiveDescription}}\n\nJoin here: {{invitationUrl}}\n\nHappy focusing!")
                .bodyHtml("<html><body><h1>Hive Invitation</h1><p>Hi {{userName}},</p><p>{{inviterName}} has invited you to join the hive \"{{hiveName}}\" on FocusHive.</p><p><strong>Description:</strong> {{hiveDescription}}</p><p><a href=\"{{invitationUrl}}\">Join the hive</a></p><p>Happy focusing!</p></body></html>")
                .build();
        notificationTemplateRepository.save(hiveInvitationTemplate);

        // Achievement notification template
        NotificationTemplate achievementTemplate = NotificationTemplate.builder()
                .notificationType(NotificationType.ACHIEVEMENT_UNLOCKED)
                .language("en")
                .subject("🎉 Achievement Unlocked: {{achievementName}}")
                .bodyText("Congratulations {{userName}}!\n\nYou've unlocked the achievement: {{achievementName}}\n\n{{achievementDescription}}\n\nKeep up the great work!")
                .bodyHtml("<html><body><h1>🎉 Achievement Unlocked!</h1><p>Congratulations {{userName}}!</p><p>You've unlocked the achievement: <strong>{{achievementName}}</strong></p><p>{{achievementDescription}}</p><p>Keep up the great work!</p></body></html>")
                .build();
        notificationTemplateRepository.save(achievementTemplate);
    }

    /**
     * Creates default notification preferences for testing
     */
    protected void createDefaultNotificationPreferences() {
        String[] testUserIds = {"test-user-1", "test-user-2", "test-user-3"};
        
        for (String userId : testUserIds) {
            // Create preferences for each notification type
            for (NotificationType type : NotificationType.values()) {
                NotificationPreference preference = NotificationPreference.builder()
                        .userId(userId)
                        .notificationType(type)
                        .inAppEnabled(true)
                        .emailEnabled(true)
                        .pushEnabled(false)
                        .frequency(NotificationFrequency.IMMEDIATE)
                        .quietStartTime(LocalTime.of(22, 0))
                        .quietEndTime(LocalTime.of(8, 0))
                        .build();
                notificationPreferenceRepository.save(preference);
            }
        }
    }

    /**
     * Creates a test notification with specified parameters
     */
    protected Notification createTestNotification(String userId, NotificationType type, String title, String content) {
        return Notification.builder()
                .userId(userId)
                .type(type)
                .title(title)
                .content(content)
                .priority(Notification.NotificationPriority.NORMAL)
                .isRead(false)
                .isArchived(false)
                .language("en")
                .deliveryAttempts(0)
                .build();
    }

    /**
     * Creates test notification with template variables
     */
    protected Map<String, Object> createTestTemplateVariables(String userName) {
        return Map.of(
                "userName", userName,
                "hiveName", "Test Study Group",
                "hiveDescription", "A productive study group for focused work sessions",
                "inviterName", "John Doe",
                "invitationUrl", "https://focushive.com/invite/abc123",
                "resetUrl", "https://focushive.com/reset/xyz789",
                "expirationTime", "30",
                "achievementName", "First Focus Session",
                "achievementDescription", "Completed your first 25-minute focus session!"
        );
    }

    /**
     * Creates a test notification preference with custom settings
     */
    protected NotificationPreference createTestNotificationPreference(
            String userId, 
            NotificationType type, 
            boolean emailEnabled, 
            boolean inAppEnabled,
            NotificationFrequency frequency) {
        return NotificationPreference.builder()
                .userId(userId)
                .notificationType(type)
                .emailEnabled(emailEnabled)
                .inAppEnabled(inAppEnabled)
                .pushEnabled(false)
                .frequency(frequency)
                .quietStartTime(LocalTime.of(22, 0))
                .quietEndTime(LocalTime.of(8, 0))
                .build();
    }

    /**
     * Creates a test notification template
     */
    protected NotificationTemplate createTestNotificationTemplate(
            NotificationType type, 
            String language, 
            String subject, 
            String bodyText, 
            String bodyHtml) {
        return NotificationTemplate.builder()
                .notificationType(type)
                .language(language)
                .subject(subject)
                .bodyText(bodyText)
                .bodyHtml(bodyHtml)
                .build();
    }

    /**
     * Waits for asynchronous operations to complete
     */
    protected void waitForAsyncOperations() {
        try {
            Thread.sleep(100); // Small delay for async operations
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}