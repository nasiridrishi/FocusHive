package com.focushive.notification;

import com.focushive.notification.entity.Notification;
import com.focushive.notification.entity.NotificationType;
import com.focushive.notification.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Simple integration test to verify basic Spring Boot context loading and JPA functionality.
 */
@SpringBootTest
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.datasource.driver-class-name=org.h2.Driver", 
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
    "spring.flyway.enabled=false",
    "spring.mail.host=localhost",
    "spring.mail.port=3025",
    "spring.mail.username=test",
    "spring.mail.password=test",
    "spring.cache.type=simple",
    "DB_PASSWORD=test",
    "REDIS_PASSWORD=",
    "RABBITMQ_PASSWORD=guest"
})
@Transactional
class SimpleIntegrationTest {

    @Autowired
    private NotificationRepository notificationRepository;

    @Test
    void contextLoads() {
        assertThat(notificationRepository).isNotNull();
    }

    @Test
    void canSaveAndRetrieveNotification() {
        // Given
        Notification notification = Notification.builder()
                .userId("test-user")
                .type(NotificationType.WELCOME)
                .title("Test Notification")
                .content("This is a test notification")
                .priority(Notification.NotificationPriority.NORMAL)
                .isRead(false)
                .isArchived(false)
                .language("en")
                .deliveryAttempts(0)
                .build();

        // When
        Notification saved = notificationRepository.save(notification);

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getTitle()).isEqualTo("Test Notification");
        assertThat(saved.getUserId()).isEqualTo("test-user");
        assertThat(saved.getType()).isEqualTo(NotificationType.WELCOME);
    }
}