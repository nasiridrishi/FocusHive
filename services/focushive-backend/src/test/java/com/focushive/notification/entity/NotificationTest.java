package com.focushive.notification.entity;

import com.focushive.user.entity.Notification;
import com.focushive.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationTest {

    private Validator validator;
    private Notification notification;
    private User testUser;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
        
        testUser = new User();
        testUser.setId("test-user-id");
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        
        notification = new Notification();
        notification.setUser(testUser);
        notification.setType("HIVE_INVITATION");
        notification.setTitle("New Hive Invitation");
        notification.setContent("You've been invited to join the Study Group hive");
        notification.setActionUrl("/hives/123/join");
        notification.setData("{\"hiveId\":\"123\",\"inviterId\":\"456\"}");
        notification.setPriority(Notification.NotificationPriority.NORMAL);
    }

    @Test
    void validNotification_shouldPassValidation() {
        // When
        Set<ConstraintViolation<Notification>> violations = validator.validate(notification);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    void notification_withNullUser_shouldFailValidation() {
        // Given
        notification.setUser(null);

        // When
        Set<ConstraintViolation<Notification>> violations = validator.validate(notification);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("User is required");
    }

    @Test
    void notification_withBlankType_shouldFailValidation() {
        // Given
        notification.setType("");

        // When
        Set<ConstraintViolation<Notification>> violations = validator.validate(notification);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Type is required");
    }

    @Test
    void notification_withTooLongType_shouldFailValidation() {
        // Given
        String longType = "A".repeat(51); // Exceeds 50 character limit
        notification.setType(longType);

        // When
        Set<ConstraintViolation<Notification>> violations = validator.validate(notification);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Type must not exceed 50 characters");
    }

    @Test
    void notification_withBlankTitle_shouldFailValidation() {
        // Given
        notification.setTitle("");

        // When
        Set<ConstraintViolation<Notification>> violations = validator.validate(notification);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Title is required");
    }

    @Test
    void notification_withTooLongTitle_shouldFailValidation() {
        // Given
        String longTitle = "A".repeat(201); // Exceeds 200 character limit
        notification.setTitle(longTitle);

        // When
        Set<ConstraintViolation<Notification>> violations = validator.validate(notification);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Title must not exceed 200 characters");
    }

    @Test
    void notification_withTooLongContent_shouldFailValidation() {
        // Given
        String longContent = "A".repeat(2001); // Exceeds 2000 character limit
        notification.setContent(longContent);

        // When
        Set<ConstraintViolation<Notification>> violations = validator.validate(notification);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Content must not exceed 2000 characters");
    }

    @Test
    void notification_withTooLongActionUrl_shouldFailValidation() {
        // Given
        String longUrl = "A".repeat(501); // Exceeds 500 character limit
        notification.setActionUrl(longUrl);

        // When
        Set<ConstraintViolation<Notification>> violations = validator.validate(notification);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Action URL must not exceed 500 characters");
    }

    @Test
    void notification_withDefaultValues_shouldHaveCorrectDefaults() {
        // Given
        Notification newNotification = new Notification();

        // When & Then
        assertThat(newNotification.getData()).isEqualTo("{}");
        assertThat(newNotification.getPriority()).isEqualTo(Notification.NotificationPriority.NORMAL);
        assertThat(newNotification.getIsRead()).isFalse();
        assertThat(newNotification.getIsArchived()).isFalse();
    }

    @Test
    void notification_shouldHaveCorrectGettersAndSetters() {
        // Given
        String type = "TASK_ASSIGNED";
        String title = "New Task Assignment";
        String content = "You have been assigned a new task";
        String actionUrl = "/tasks/456";
        String data = "{\"taskId\":\"456\"}";
        Notification.NotificationPriority priority = Notification.NotificationPriority.HIGH;
        LocalDateTime readAt = LocalDateTime.now();

        // When
        notification.setType(type);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setActionUrl(actionUrl);
        notification.setData(data);
        notification.setPriority(priority);
        notification.setIsRead(true);
        notification.setReadAt(readAt);
        notification.setIsArchived(true);

        // Then
        assertThat(notification.getType()).isEqualTo(type);
        assertThat(notification.getTitle()).isEqualTo(title);
        assertThat(notification.getContent()).isEqualTo(content);
        assertThat(notification.getActionUrl()).isEqualTo(actionUrl);
        assertThat(notification.getData()).isEqualTo(data);
        assertThat(notification.getPriority()).isEqualTo(priority);
        assertThat(notification.getIsRead()).isTrue();
        assertThat(notification.getReadAt()).isEqualTo(readAt);
        assertThat(notification.getIsArchived()).isTrue();
    }

    @Test
    void notification_withNullContentAndActionUrl_shouldPassValidation() {
        // Given
        notification.setContent(null);
        notification.setActionUrl(null);

        // When
        Set<ConstraintViolation<Notification>> violations = validator.validate(notification);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    void notification_withAllPriorityLevels_shouldBeValid() {
        // Test all priority levels are valid
        for (Notification.NotificationPriority priority : Notification.NotificationPriority.values()) {
            // Given
            notification.setPriority(priority);

            // When
            Set<ConstraintViolation<Notification>> violations = validator.validate(notification);

            // Then
            assertThat(violations).isEmpty();
        }
    }

    @Test
    void notification_withEmptyData_shouldDefaultToEmptyJson() {
        // Given
        Notification newNotification = new Notification();

        // When & Then
        assertThat(newNotification.getData()).isEqualTo("{}");
    }

    @Test
    void notification_relationshipWithUser_shouldBeCorrect() {
        // When & Then
        assertThat(notification.getUser()).isEqualTo(testUser);
        assertThat(notification.getUser().getId()).isEqualTo("test-user-id");
        assertThat(notification.getUser().getUsername()).isEqualTo("testuser");
        assertThat(notification.getUser().getEmail()).isEqualTo("test@example.com");
    }
}